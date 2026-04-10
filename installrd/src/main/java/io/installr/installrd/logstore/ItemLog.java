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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.semver4j.Semver;

import io.installr.installrd.logstore.entity.ItemLogEntity;
import io.installr.installrd.logstore.entity.TransactionLogEntity;
import io.installr.lib.installrd.pkg.ItemLogRecord;
import io.installr.lib.pkg.PackageContent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class ItemLog {

    @Transactional
    public Long put( ItemLogRecord record ) {
        final ItemLogEntity entity = Optional.ofNullable(
            (ItemLogEntity)ItemLogEntity.find(
                "namespace = ?1 and package_name = ?2 and path = ?3",
                record.getNamespace(),
                record.getPackageName(),
                record.getPath()
            ).firstResult()
        )
            .orElse( new ItemLogEntity() );
        entity.namespace = record.getNamespace();
        entity.package_name = record.getPackageName();
        entity.path = record.getPath();
        entity.version = record.getVersion().toString();
        entity.media_type = record.getMediaType();
        entity.encoding = record.getEncoding();
        entity.hash = record.getHash();
        entity.installed_by_tx = TransactionLogEntity.findById( record.getInstalledBy() );
        entity.installed_on = Instant.now();
        entity.error = record.hasError();
        entity.persistAndFlush();
        return entity.id;
    }

    public ItemLogRecord get( Long id ) {
        return ( (ItemLogEntity)ItemLogEntity.findById( id ) ).toItemLogRecord();
    }

    public List<ItemLogRecord> getAll( String namespace ) {
        return ItemLogEntity.stream( "namespace = ?1", namespace )
            .map( x -> ( (ItemLogEntity)x ).toItemLogRecord() )
            .toList();
    }

    public Map<String, ItemLogRecord> getByPackagename( String namespace, String packageName ) {
        List<ItemLogEntity> itemLogRecords = ItemLogEntity.list(
            "namespace = ?1 and package_name = ?2",
            namespace,
            packageName
        );
        if ( itemLogRecords == null || itemLogRecords.isEmpty() ) {
            return Map.of();
        }
        return itemLogRecords.stream()
            .map(
                item -> {
                    final ItemLogRecord itemLogRecord = new ItemLogRecord();
                    itemLogRecord.setPath( item.path );
                    itemLogRecord.setMediaType( item.media_type );
                    itemLogRecord.setEncoding( item.encoding );
                    itemLogRecord.setHash( item.hash );
                    itemLogRecord.setVersion( Semver.parse( item.version ) );
                    itemLogRecord.hasError( item.error );
                    return itemLogRecord;
                }
            )
            .collect( Collectors.toMap( PackageContent::getPath, x -> x ) );
    }

}
