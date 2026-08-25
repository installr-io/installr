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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.installr.lib.installrd.pkg.Action;
import io.installr.lib.installrd.pkg.PackageLogRecord;
import io.installr.lib.installrd.pkg.PackageStatus;
import io.installr.lib.pkg.PackageInfo;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table( name = "PACKAGE_LOG" )
@IdClass( PackageLogId.class )
public class PackageLogEntity extends PanacheEntityBase {

    @Id
    @Column( name = "namespace" )
    public String namespace;

    @Id
    @Column( name = "tx_id" )
    public UUID txId;

    @Id
    @Column( name = "package_name" )
    public String package_name;

    @Column( name = "package_info", nullable = false )
    @JdbcTypeCode( SqlTypes.JSON )
    public PackageInfo package_info;

    @Column( name = "repository_id", nullable = false )
    public String repository_id;

    @Column( name = "action", nullable = false )
    @Enumerated( EnumType.STRING )
    public Action action;

    @Column( name = "action_timestamp", nullable = false )
    public Instant action_timestamp;

    @Column( name = "status", nullable = false )
    @Enumerated( EnumType.STRING )
    public PackageStatus status;

    public PackageLogRecord toPackageLogRecord() {
        return new PackageLogRecord(
            namespace,
            txId,
            package_name,
            package_info,
            repository_id,
            action,
            action_timestamp,
            status
        );
    }

}
