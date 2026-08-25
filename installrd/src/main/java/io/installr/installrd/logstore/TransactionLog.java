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

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import io.installr.installrd.logstore.entity.TransactionLogEntity;
import io.installr.installrd.logstore.entity.TransactionLogId;
import io.installr.lib.installrd.transaction.TransactionLogRecord;
import io.installr.lib.installrd.util.Pagination;
import io.installr.lib.installrd.util.QueryBuilder;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class TransactionLog {

    @Transactional
    public TransactionLogRecord put( TransactionLogRecord record ) {
        final TransactionLogId id = new TransactionLogId( record.getNamespace(), record.getId() );
        final TransactionLogEntity entity = (TransactionLogEntity)TransactionLogEntity
            .findByIdOptional( id )
            .orElse( new TransactionLogEntity() );
        entity.namespace = record.getNamespace();
        entity.txId = record.getId();
        entity.begin_timestamp = record.getBeginTimestamp();
        entity.end_timestamp = record.getEndTimestamp();
        entity.status = record.getStatus();
        entity.daemon_version = record.getDaemonVersion();
        entity.persistAndFlush();
        return record;
    }

    public TransactionLogRecord get( String namespace, UUID transactionId ) {
        final TransactionLogEntity tx = (TransactionLogEntity)TransactionLogEntity
            .find( "namespace = ?1 and txId = ?2", namespace, transactionId )
            .firstResult();
        if ( tx == null ) {
            throw new BadRequestException(
                String.format( "Transaction with id '%s' not found.", transactionId )
            );
        }
        return tx.toTransactionLogRecord();
    }

    public List<TransactionLogRecord> getAll( String namespace ) {
        final Stream<TransactionLogEntity> txStream = buildQuery( namespace ).stream();
        return txStream
            .map( TransactionLogEntity::toTransactionLogRecord )
            .toList();
    }

    public List<TransactionLogRecord> getPaged( String namespace, Pagination pagination ) {
        final Stream<TransactionLogEntity> txStream = buildQuery( namespace )
            .page( pagination.getPage(), pagination.getSize() )
            .stream();
        return txStream
            .map( TransactionLogEntity::toTransactionLogRecord )
            .toList();
    }

    private PanacheQuery<PanacheEntityBase> buildQuery( String namespace ) {
        final QueryBuilder qb = new QueryBuilder();
        qb.put( "namespace", namespace );
        return TransactionLogEntity
            .find(
                qb.query(),
                Sort.by( "begin_timestamp", Direction.Descending ),
                qb.params()
            );
    }

}
