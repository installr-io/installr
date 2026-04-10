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

import org.semver4j.Semver;

import io.installr.lib.installrd.pkg.ItemLogRecord;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "ITEM_LOG",
    uniqueConstraints = {
        @UniqueConstraint( columnNames = { "namespace", "package_name", "path" }, name = "ITEM_LOG_NS_PKG_PATH_UC" )
    }
)
public class ItemLogEntity extends PanacheEntityBase {

    @Id
    @SequenceGenerator( name = "ITEM_LOG_SEQ", initialValue = 1, allocationSize = 1 )
    @GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "ITEM_LOG_SEQ" )
    public Long id;

    @Column( name = "namespace", nullable = false )
    public String namespace;

    @Column( name = "package_name", nullable = false )
    public String package_name;

    @Column( name = "path", nullable = false )
    public String path;

    @Column( name = "version", nullable = false )
    public String version;

    @Column( name = "media_type", nullable = false )
    public String media_type;

    @Column( name = "encoding", nullable = false )
    public String encoding;

    @Column( name = "hash", nullable = false )
    public String hash;

    @ManyToOne
    @JoinColumn( name = "installed_by_txid", nullable = false )
    public TransactionLogEntity installed_by_tx;

    @Column( name = "installed_on", nullable = false )
    public Instant installed_on;

    @Column( name = "error", nullable = false )
    public Boolean error;

    public ItemLogRecord toItemLogRecord() {
        final ItemLogRecord item = new ItemLogRecord();
        item.setId( id );
        item.setNamespace( namespace );
        item.setPackageName( package_name );
        item.setPath( path );
        item.setVersion( Semver.parse( version ) );
        item.setMediaType( media_type );
        item.setEncoding( encoding );
        item.setHash( hash );
        item.setInstalledBy( installed_by_tx.id );
        item.setInstalledOn( installed_on );
        item.hasError( error );
        return item;
    }

}
