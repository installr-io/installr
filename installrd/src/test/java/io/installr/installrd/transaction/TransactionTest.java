/*
 * Installr.io
 *
 * Copyright (c) 2025-present Dorian Vallant
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.installr.installrd.transaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.installr.installrd.logstore.EventLog;
import io.installr.installrd.logstore.TransactionLog;
import io.installr.installrd.namespace.LockException;
import io.installr.installrd.namespace.Namespace;
import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.event.EventLogRecord;
import io.installr.lib.installrd.event.Events;
import io.installr.lib.installrd.transaction.TransactionLogRecord;
import io.installr.lib.installrd.transaction.TransactionStatus;
import io.smallrye.mutiny.subscription.MultiEmitter;

@ExtendWith( MockitoExtension.class )
class TransactionTest {

    static final String ACTOR = "CN=test,O=installr-test";

    @Mock
    Namespace namespace;

    @Mock
    TransactionLog txLog;

    @Mock
    EventLog eventLog;

    @Mock
    MultiEmitter<Event> emitter;

    Transaction tx;

    @BeforeEach
    void setUp() {
        lenient().when( namespace.name() ).thenReturn( "test-namespace" );
        tx = new Transaction( namespace, txLog, eventLog );
    }

    @Test
    void acquire_should_return_transaction_id() {
        final UUID id = tx.acquire( ACTOR );

        assertNotNull( id );
        assertTrue( tx.isActive() );
    }

    @Test
    void acquire_should_throw_when_namespace_already_locked() {
        tx.acquire( ACTOR );

        assertThrows( LockException.class, () -> tx.acquire( ACTOR ) );
    }

    @Test
    void begin_should_emit_transaction_started_event() {
        tx.acquire( ACTOR );
        givenEmitterActive();

        tx.begin( emitter );

        verify( emitter ).emit( any( Events.TransactionStarted.class ) );
    }

    @Test
    void end_should_emit_transaction_finished_and_complete_emitter() {
        tx.acquire( ACTOR );
        givenEmitterActive();
        givenTransactionLogRecord();

        tx.begin( emitter );
        tx.end();

        verify( emitter ).emit( any( Events.TransactionFinished.class ) );
        verify( emitter ).complete();
    }

    @Test
    void end_should_persist_success_status_when_no_explicit_status_set() {
        tx.acquire( ACTOR );
        givenEmitterActive();
        givenTransactionLogRecord();
        final ArgumentCaptor<TransactionLogRecord> captor = ArgumentCaptor.forClass( TransactionLogRecord.class );

        tx.begin( emitter );
        tx.end();

        verify( txLog, atLeastOnce() ).put( captor.capture() );
        final TransactionLogRecord finalRecord = captor.getAllValues().stream()
            .filter( r -> r.getStatus() != TransactionStatus.InProgress )
            .findFirst()
            .orElseThrow();
        assertTrue( finalRecord.getStatus() == TransactionStatus.Success );
    }

    @Test
    void end_should_clear_state_so_namespace_can_be_reused() {
        tx.acquire( ACTOR );
        givenEmitterActive();
        givenTransactionLogRecord();

        tx.begin( emitter );
        tx.end();

        assertFalse( tx.isActive() );
        assertNotNull( tx.acquire( ACTOR ) ); // namespace can be locked again
    }

    @Test
    void fail_should_persist_failed_status() {
        tx.acquire( ACTOR );
        givenEmitterActive();
        givenTransactionLogRecord();
        final ArgumentCaptor<TransactionLogRecord> captor = ArgumentCaptor.forClass( TransactionLogRecord.class );

        tx.begin( emitter );
        tx.fail( new RuntimeException( "something went wrong" ) );
        tx.end();

        verify( txLog, atLeastOnce() ).put( captor.capture() );
        final TransactionLogRecord finalRecord = captor.getAllValues().stream()
            .filter( r -> r.getStatus() != TransactionStatus.InProgress )
            .findFirst()
            .orElseThrow();
        assertTrue( finalRecord.getStatus() == TransactionStatus.Failed );
    }

    @Test
    void fail_should_not_overwrite_canceled_status() {
        tx.acquire( ACTOR );
        tx.cancel();

        tx.fail( new RuntimeException( "ignored" ) );

        assertTrue( tx.isCanceled() );
    }

    @Test
    void cancel_should_mark_transaction_as_canceled() {
        tx.acquire( ACTOR );

        tx.cancel();

        assertTrue( tx.isCanceled() );
    }

    @Test
    void emitEvent_should_write_to_log_but_skip_emit_when_emitter_is_cancelled() {
        tx.acquire( ACTOR );
        when( emitter.isCancelled() ).thenReturn( true );
        when( emitter.onCancellation( any() ) ).thenReturn( emitter );

        tx.begin( emitter );
        tx.emitEvent( new Events.PackageResolvingStarted() );

        // TransactionStarted + PackageResolvingStarted both written to eventLog
        verify( eventLog, times( 2 ) ).put( any( EventLogRecord.class ) );
        // but never forwarded to the SSE emitter
        verify( emitter, never() ).emit( any() );
    }

    @Test
    void end_should_not_complete_emitter_when_emitter_is_cancelled() {
        tx.acquire( ACTOR );
        when( emitter.isCancelled() ).thenReturn( true );
        when( emitter.onCancellation( any() ) ).thenReturn( emitter );
        givenTransactionLogRecord();

        tx.begin( emitter );
        tx.end();

        verify( emitter, never() ).complete();
    }

    @Test
    void begin_should_cancel_transaction_when_client_disconnects() {
        tx.acquire( ACTOR );
        final ArgumentCaptor<Runnable> terminationCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( emitter.onCancellation( terminationCaptor.capture() ) ).thenReturn( emitter );
        // first call: during begin's emitEvent; second call: inside the disconnect callback
        when( emitter.isCancelled() ).thenReturn( false, true );

        tx.begin( emitter );

        // simulate client disconnect: Mutiny fires the termination callback
        terminationCaptor.getValue().run();

        assertTrue( tx.isCanceled() );
    }

    private void givenEmitterActive() {
        when( emitter.isCancelled() ).thenReturn( false );
        when( emitter.onCancellation( any() ) ).thenReturn( emitter );
    }

    private void givenTransactionLogRecord() {
        when( txLog.get( any( String.class ), any( UUID.class ) ) ).thenReturn(
            new TransactionLogRecord(
                "test-namespace",
                UUID.randomUUID(),
                Instant.now(),
                null,
                TransactionStatus.InProgress,
                "test",
                ACTOR
            )
        );
    }

}
