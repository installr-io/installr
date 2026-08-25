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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import io.installr.installrd.logstore.entity.PackageLogEntity;
import io.installr.installrd.logstore.entity.PackageLogId;
import io.installr.lib.installrd.pkg.PackageLogRecord;
import io.installr.lib.installrd.pkg.PackageLogRecord.Filter;
import io.installr.lib.installrd.util.Pagination;
import io.installr.lib.installrd.util.QueryBuilder;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class PackageLog {

    @Transactional
    public PackageLogRecord put( PackageLogRecord record ) {
        final PackageLogId id = new PackageLogId( record.getNamespace(), record.getTxId(), record.getPackageName() );
        final PackageLogEntity entity = (PackageLogEntity)PackageLogEntity
            .findByIdOptional( id )
            .orElse( new PackageLogEntity() );
        entity.namespace = record.getNamespace();
        entity.txId = record.getTxId();
        entity.package_name = record.getPackageName();
        entity.package_info = record.getPackageInfo();
        entity.repository_id = record.getRepositoryId();
        entity.action = record.getAction();
        entity.action_timestamp = record.getActionTimestamp();
        entity.status = record.getStatus();
        entity.persistAndFlush();
        return record;
    }

    public PackageLogRecord get( String namespace, UUID txId, String packageName ) {
        return ( (PackageLogEntity)PackageLogEntity.findById( new PackageLogId( namespace, txId, packageName ) ) )
            .toPackageLogRecord();
    }

    public Optional<List<PackageLogRecord>> getByPackagename( String namespace, String packageName ) {
        return Optional.ofNullable(
            PackageLogEntity.stream( "namespace = ?1 and package_name = ?2", namespace, packageName )
                .map( x -> ( (PackageLogEntity)x ).toPackageLogRecord() )
                .toList()
        );
    }

    public List<PackageLogRecord> getAll( String namespace, Filter filter ) {
        final Stream<PackageLogEntity> logStream = buildQuery( namespace, filter ).stream();
        return logStream
            .map( PackageLogEntity::toPackageLogRecord )
            .toList();
    }

    public List<PackageLogRecord> getPaged( String namespace, Pagination pagination, Filter filter ) {
        final Stream<PackageLogEntity> logStream = buildQuery( namespace, filter )
            .page( pagination.getPage(), pagination.getSize() )
            .stream();
        return logStream
            .map( PackageLogEntity::toPackageLogRecord )
            .toList();
    }

    private PanacheQuery<PanacheEntityBase> buildQuery( String namespace, Filter filter ) {
        final QueryBuilder qb = filter == null ? new QueryBuilder() : filter.buildQuery();
        qb.put( "namespace", namespace );
        return PackageLogEntity
            .find(
                qb.query(),
                Sort.by( "action_timestamp", Direction.Ascending ),
                qb.params()
            );
    }

}
