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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.installr.installrd.logstore.EventLog;
import io.installr.installrd.logstore.TransactionLog;
import io.installr.installrd.namespace.Namespace;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class TransactionService {

    @Inject
    Namespace namespace;

    @Inject
    TransactionLog txLog;

    @Inject
    EventLog eventLog;

    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();

    /**
     * @param actor subject DN of the client, which started the transaction
     */
    public Transaction acquireTransaction( String actor ) {
        log.infof( "Acquiring transaction for namespace '%s'", namespace.name() );
        final Transaction transaction = transactions.computeIfAbsent(
            namespace.name(),
            name -> new Transaction( namespace, txLog, eventLog )
        );
        final UUID txId = transaction.acquire( actor );
        log.infof( "Transaction acquired. ID = %s", txId );
        return transaction;
    }

    public Transaction currentTransaction() {
        if ( !transactions.containsKey( namespace.name() ) ) {
            throw new IllegalStateException(
                String.format( "No active transaction for namespace '%s'.", namespace.name() )
            );
        }
        return transactions.get( namespace.name() );
    }

}
