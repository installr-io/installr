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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import io.installr.installrd.Installrd;
import io.installr.installrd.logstore.EventLog;
import io.installr.installrd.logstore.TransactionLog;
import io.installr.installrd.namespace.LockException;
import io.installr.installrd.namespace.Namespace;
import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.event.EventLogRecord;
import io.installr.lib.installrd.event.Events;
import io.installr.lib.installrd.event.Status;
import io.installr.lib.installrd.transaction.TransactionLogRecord;
import io.installr.lib.installrd.transaction.TransactionStatus;
import io.installr.lib.util.FileUtil;
import io.smallrye.mutiny.subscription.MultiEmitter;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class Transaction {

    private final Namespace namespace;
    private final TransactionLog txLog;
    private final EventLog eventLog;
    private UUID txId;
    private Status transactionStatus;
    private MultiEmitter<? super Event> emitter;
    private Path tmpPath;

    Transaction( Namespace namespace, TransactionLog txLog, EventLog eventLog ) {
        this.namespace = namespace;
        this.txLog = txLog;
        this.eventLog = eventLog;
    }

    UUID acquire() {
        if ( isActive() ) {
            log.errorf( "Namespace '%s' is locked by TX '%s'.", namespace.name(), txId );
            throw new LockException();
        }
        return this.txId = UUID.randomUUID();
    }

    public Namespace namespace() {
        return namespace;
    }

    public void begin( MultiEmitter<? super Event> emitter ) {
        this.emitter = emitter;
        emitter.onCancellation(
            () -> {
                if ( emitter.isCancelled() ) {
                    log.infof( "Client disconnected from transaction '%s', cancelling.", txId );
                    this.transactionStatus = Status.Canceled;
                }
            }
        );
        log.debug( "Starting transaction..." );
        txLog.put(
            new TransactionLogRecord(
                namespace.name(),
                txId,
                Instant.now(),
                null,
                TransactionStatus.InProgress,
                Installrd.version().getVersion()
            )
        );
        log.debugf( "Transaction with id %s started.", txId );
        emitEvent( new Events.TransactionStarted( txId ) );
    }

    public void end() {
        if ( transactionStatus == null ) {
            transactionStatus = Status.Success;
        }
        log.debugf( "Ending transaction with id %s.", txId );
        checkActive();
        try {
            final TransactionLogRecord txLogRecord = txLog.get( namespace.name(), txId );
            txLogRecord.setEndTimestamp( Instant.now() );
            txLogRecord.setStatus( TransactionStatus.fromEventStatus( transactionStatus ) );
            txLog.put( txLogRecord );
            if ( !emitter.isCancelled() ) {
                emitEvent( new Events.TransactionFinished( txId, transactionStatus ) );
                emitter.complete();
            }
        }
        finally {
            log.debugf( "Transaction '%s' finished with status '%s'.", txId, transactionStatus );
            txId = null;
            transactionStatus = null;
            emitter = null;
            if ( tmpPath != null ) {
                FileUtil.rmdir( tmpPath );
                tmpPath = null;
            }
        }
    }

    private void checkActive() {
        if ( !isActive() ) {
            throw new IllegalStateException( "No active transaction found." );
        }
    }

    public boolean isActive() {
        return txId != null;
    }

    public void emitEvent( Event event ) {
        checkActive();
        eventLog.put( new EventLogRecord( namespace.name(), txId, event ) );
        if ( !emitter.isCancelled() ) {
            emitter.emit( event );
        }
        if ( event.status().type() == Status.Type.Fatal ) {
            transactionStatus = event.status();
        }
        else if ( transactionStatus == null || transactionStatus.type().ordinal() < Status.Type.Failed.ordinal() ) {
            this.transactionStatus = event.status();
        }
    }

    public UUID id() {
        return txId;
    }

    public void cancel() {
        checkActive();
        this.transactionStatus = Status.Canceled;
    }

    public boolean isCanceled() {
        return this.transactionStatus == Status.Canceled;
    }

    public void fail( Throwable t ) {
        if ( isCanceled() ) {
            return;
        }
        transactionStatus = Status.Failed( t );
        log.error( t.toString() );
        log.debugf( t, "Stacktrace" );
    }

    public void reset( Throwable t ) {
        txId = null;
        log.error( "Transaction reset called!" );
        log.error( t.toString() );
        log.debugf( t, "Stacktrace" );
    }

    public Path tmpDir() throws IOException {
        checkActive();
        // Create a temporary path for this transaction, which can be used e.g. to extract archives.
        // The temporary path will be removed when the transaction ends.
        if ( tmpPath == null ) {
            tmpPath = Path.of( "/tmp" ).resolve( UUID.randomUUID().toString() );
            Files.createDirectory( tmpPath );
        }
        return tmpPath;
    }

}
