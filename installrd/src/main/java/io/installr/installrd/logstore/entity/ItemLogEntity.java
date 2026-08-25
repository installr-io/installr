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
package io.installr.installrd.logstore.entity;

import java.time.Instant;
import java.util.UUID;

import org.semver4j.Semver;

import io.installr.lib.installrd.pkg.ItemLogRecord;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table( name = "ITEM_LOG" )
@IdClass( ItemLogId.class )
public class ItemLogEntity extends PanacheEntityBase {

    @Id
    @Column( name = "namespace" )
    public String namespace;

    @Id
    @Column( name = "tx_id" )
    public UUID txId;

    @Id
    @Column( name = "package_name" )
    public String package_name;

    @Id
    @Column( name = "path" )
    public String path;

    @Column( name = "version", nullable = false )
    public String version;

    @Column( name = "media_type", nullable = false )
    public String media_type;

    @Column( name = "encoding", nullable = false )
    public String encoding;

    @Column( name = "hash", nullable = false )
    public String hash;

    @Column( name = "installed_on", nullable = false )
    public Instant installed_on;

    @Column( name = "error", nullable = false )
    public Boolean error;

    public ItemLogRecord toItemLogRecord() {
        final ItemLogRecord item = new ItemLogRecord();
        item.setNamespace( namespace );
        item.setTxId( txId );
        item.setPackageName( package_name );
        item.setPath( path );
        item.setVersion( Semver.parse( version ) );
        item.setMediaType( media_type );
        item.setEncoding( encoding );
        item.setHash( hash );
        item.setInstalledOn( installed_on );
        item.hasError( error );
        return item;
    }

}
