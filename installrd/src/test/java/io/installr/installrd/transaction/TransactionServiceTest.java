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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.installr.installrd.logstore.EventLog;
import io.installr.installrd.logstore.TransactionLog;
import io.installr.installrd.namespace.LockException;
import io.installr.installrd.namespace.Namespace;

@ExtendWith( MockitoExtension.class )
class TransactionServiceTest {

    static final String ACTOR = "CN=test,O=installr-test";

    @Mock
    Namespace namespace;

    @Mock
    TransactionLog txLog;

    @Mock
    EventLog eventLog;

    @InjectMocks
    TransactionService txService;

    @BeforeEach
    void setUp() {
        when( namespace.name() ).thenReturn( "test-namespace" );
    }

    @Test
    void acquireTransaction_should_return_active_transaction() {
        final Transaction tx = txService.acquireTransaction( ACTOR );

        assertTrue( tx.isActive() );
    }

    @Test
    void acquireTransaction_should_throw_when_namespace_already_locked() {
        txService.acquireTransaction( ACTOR );

        assertThrows( LockException.class, () -> txService.acquireTransaction( ACTOR ) );
    }

    @Test
    void currentTransaction_should_return_same_instance_as_acquired() {
        final Transaction acquired = txService.acquireTransaction( ACTOR );

        assertSame( acquired, txService.currentTransaction() );
    }

    @Test
    void currentTransaction_should_throw_when_no_transaction_for_namespace() {
        assertThrows( IllegalStateException.class, () -> txService.currentTransaction() );
    }

}
