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

import io.installr.lib.installrd.transaction.TransactionLogRecord;
import io.installr.lib.installrd.transaction.TransactionStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table( name = "TRANSACTION_LOG" )
@IdClass( TransactionLogId.class )
public class TransactionLogEntity extends PanacheEntityBase {

    @Id
    @Column( name = "namespace" )
    public String namespace;

    @Id
    @Column( name = "tx_id" )
    public UUID tx_id;

    @Column( name = "begin_timestamp", nullable = false )
    public Instant begin_timestamp;

    @Column( name = "end_timestamp", nullable = true )
    public Instant end_timestamp;

    @Column( name = "status", nullable = false )
    @Enumerated( EnumType.STRING )
    public TransactionStatus status;

    @Column( name = "daemon_version", nullable = false )
    public String daemon_version;

    public TransactionLogRecord toTransactionLogRecord() {
        return new TransactionLogRecord( namespace, tx_id, begin_timestamp, end_timestamp, status, daemon_version );
    }

}
