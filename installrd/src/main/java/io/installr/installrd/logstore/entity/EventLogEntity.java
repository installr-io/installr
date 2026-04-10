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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.event.EventLogRecord;
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

@Entity
@Table( name = "EVENT_LOG" )
public class EventLogEntity extends PanacheEntityBase {

    @Id
    @SequenceGenerator( name = "EVENT_LOG_SEQ", initialValue = 1, allocationSize = 1 )
    @GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "EVENT_LOG_SEQ" )
    public Long id;

    @Column( name = "namespace", nullable = false )
    public String namespace;

    @ManyToOne
    @JoinColumn( name = "transaction_id", nullable = false )
    public TransactionLogEntity txEntity;

    @Column( name = "event", nullable = false )
    @JdbcTypeCode( SqlTypes.JSON )
    public Event event;

    public EventLogRecord toEventLogRecord() {
        return new EventLogRecord( id, namespace, txEntity.id, event );
    }

}
