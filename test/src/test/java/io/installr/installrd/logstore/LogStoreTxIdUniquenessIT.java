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
package io.installr.installrd.logstore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.installr.installrd.logstore.entity.TransactionLogEntity;
import io.installr.lib.installrd.transaction.TransactionLogRecord;
import io.installr.lib.installrd.transaction.TransactionStatus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class LogStoreTxIdUniquenessIT {

    static final String NS = "ns-txid-uniqueness";

    @Inject
    TransactionLog txLog;

    @AfterEach
    @Transactional
    void cleanup() {
        TransactionLogEntity.delete( "namespace = ?1", NS );
    }

    @Test
    @Transactional
    void same_tx_id_in_same_namespace_yields_exactly_one_record() {
        final UUID txId = UUID.randomUUID();

        txLog.put(
            new TransactionLogRecord( NS, txId, Instant.now(), null, TransactionStatus.InProgress, "0.0.0-test" )
        );
        txLog.put(
            new TransactionLogRecord( NS, txId, Instant.now(), Instant.now(), TransactionStatus.Canceled, "0.0.0-test" )
        );

        assertEquals( 1L, TransactionLogEntity.count( "namespace = ?1 and txId = ?2", NS, txId ) );

        final TransactionLogRecord record = txLog.get( NS, txId );
        assertEquals( TransactionStatus.Canceled, record.getStatus() );
    }

}
