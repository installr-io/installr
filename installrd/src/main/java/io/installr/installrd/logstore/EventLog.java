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

import org.jboss.logging.Logger.Level;

import io.installr.installrd.logstore.entity.EventLogEntity;
import io.installr.installrd.logstore.entity.TransactionLogEntity;
import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.event.EventLogRecord;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class EventLog {

    @Transactional
    public Long put( EventLogRecord record ) {
        logEvent( record.getEvent() );
        final EventLogEntity entity = new EventLogEntity();
        entity.namespace = record.getNamespace();
        entity.txEntity = TransactionLogEntity.findById( record.getTransactionId() );
        entity.event = record.getEvent();
        entity.persistAndFlush();
        return entity.id;
    }

    public EventLogRecord get( Long id ) {
        return ( (EventLogEntity)EventLogEntity.findById( id ) ).toEventLogRecord();
    }

    public List<EventLogRecord> getAll( UUID transactionId ) {
        return EventLogEntity
            .stream(
                "txEntity = ?1",
                Sort.by( "id" ),
                (TransactionLogEntity)TransactionLogEntity.findById( transactionId )
            )
            .map( x -> ( (EventLogEntity)x ).toEventLogRecord() )
            .toList();
    }

    private void logEvent( Event event ) {
        final Level level = switch ( event.status().type() ) {
            case Fatal            -> Level.FATAL;
            case Failed, Canceled -> Level.ERROR;
            default               -> Level.INFO;
        };
        log.logf( level, "%s", event );
    }

}
