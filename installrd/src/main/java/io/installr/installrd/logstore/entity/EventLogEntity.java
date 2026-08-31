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

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.event.EventLogRecord;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table( name = "EVENT_LOG" )
@IdClass( EventLogId.class )
public class EventLogEntity extends PanacheEntityBase {

    @Id
    @Column( name = "namespace" )
    public String namespace;

    @Id
    @Column( name = "tx_id" )
    public UUID tx_id;

    /**
     * Hibernate needs a unique key to distinguish between entities (cache). We use a dummy ID as part of the composite
     * key to make entities distinct.
     */
    @Id
    @Column( name = "id" )
    public UUID id;

    @Column( name = "event", nullable = false )
    @JdbcTypeCode( SqlTypes.JSON )
    public Event event;

    public EventLogRecord toEventLogRecord() {
        return new EventLogRecord( namespace, tx_id, event );
    }

}
