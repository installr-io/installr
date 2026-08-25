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
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.semver4j.Semver;

import io.installr.installrd.logstore.entity.EventLogEntity;
import io.installr.installrd.logstore.entity.ItemLogEntity;
import io.installr.installrd.logstore.entity.PackageLogEntity;
import io.installr.installrd.logstore.entity.TransactionLogEntity;
import io.installr.lib.installrd.event.EventLogRecord;
import io.installr.lib.installrd.event.Events;
import io.installr.lib.installrd.pkg.Action;
import io.installr.lib.installrd.pkg.ItemLogRecord;
import io.installr.lib.installrd.pkg.PackageLogRecord;
import io.installr.lib.installrd.pkg.PackageStatus;
import io.installr.lib.installrd.transaction.TransactionLogRecord;
import io.installr.lib.installrd.transaction.TransactionStatus;
import io.installr.lib.pkg.PackageId;
import io.installr.lib.pkg.PackageInfo;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class LogStoreNamespaceIsolationIT {

    static final String NS_ALPHA = "namespace-alpha";
    static final String NS_BETA = "namespace-beta";

    @Inject
    TransactionLog txLog;
    @Inject
    PackageLog packageLog;
    @Inject
    ItemLog itemLog;
    @Inject
    EventLog eventLog;

    @AfterEach
    @Transactional
    void cleanup() {
        // delete in FK order: dependents first, then TRANSACTION_LOG
        EventLogEntity.delete( "namespace in ?1", List.of( NS_ALPHA, NS_BETA ) );
        ItemLogEntity.delete( "namespace in ?1", List.of( NS_ALPHA, NS_BETA ) );
        PackageLogEntity.delete( "namespace in ?1", List.of( NS_ALPHA, NS_BETA ) );
        TransactionLogEntity.delete( "namespace in ?1", List.of( NS_ALPHA, NS_BETA ) );
    }

    @Test
    @Transactional
    void same_tx_id_in_different_namespaces_creates_independent_records() {
        final UUID txId = UUID.randomUUID();

        // --- TRANSACTION_LOG ---
        txLog.put(
            new TransactionLogRecord( NS_ALPHA, txId, Instant.now(), null, TransactionStatus.InProgress, "0.0.0-test" )
        );
        txLog.put(
            new TransactionLogRecord( NS_BETA, txId, Instant.now(), null, TransactionStatus.InProgress, "0.0.0-test" )
        );

        assertEquals( NS_ALPHA, txLog.get( NS_ALPHA, txId ).getNamespace() );
        assertEquals( NS_BETA, txLog.get( NS_BETA, txId ).getNamespace() );

        // --- PACKAGE_LOG ---
        final PackageInfo pkgInfo = new PackageInfo();
        pkgInfo.setPackageId( new PackageId( "my-package", "1.0.0" ) );

        packageLog.put(
            new PackageLogRecord(
                NS_ALPHA,
                txId,
                "my-package",
                pkgInfo,
                "repo",
                Action.Install,
                Instant.now(),
                PackageStatus.InProgress
            )
        );
        packageLog.put(
            new PackageLogRecord(
                NS_BETA,
                txId,
                "my-package",
                pkgInfo,
                "repo",
                Action.Install,
                Instant.now(),
                PackageStatus.InProgress
            )
        );

        assertEquals( NS_ALPHA, packageLog.get( NS_ALPHA, txId, "my-package" ).getNamespace() );
        assertEquals( NS_BETA, packageLog.get( NS_BETA, txId, "my-package" ).getNamespace() );

        // --- ITEM_LOG ---
        itemLog.put( itemRecord( NS_ALPHA, txId ) );
        itemLog.put( itemRecord( NS_BETA, txId ) );

        assertEquals( 1, itemLog.getByPackagename( NS_ALPHA, "my-package" ).size() );
        assertEquals( 1, itemLog.getByPackagename( NS_BETA, "my-package" ).size() );

        // --- EVENT_LOG ---
        eventLog.put( new EventLogRecord( NS_ALPHA, txId, new Events.TransactionStarted( txId ) ) );
        eventLog.put( new EventLogRecord( NS_BETA, txId, new Events.TransactionStarted( txId ) ) );

        assertEquals( 1, eventLog.getAll( NS_ALPHA, txId ).size() );
        assertEquals( 1, eventLog.getAll( NS_BETA, txId ).size() );
    }

    private ItemLogRecord itemRecord( String namespace, UUID txId ) {
        final ItemLogRecord item = new ItemLogRecord();
        item.setNamespace( namespace );
        item.setTxId( txId );
        item.setPackageName( "my-package" );
        item.setPath( "/etc/my-package/config.yaml" );
        item.setVersion( new Semver( "1.0.0" ) );
        item.setMediaType( "text/yaml" );
        item.setEncoding( "identity" );
        item.setHash( "deadbeef" );
        item.hasError( false );
        return item;
    }

}
