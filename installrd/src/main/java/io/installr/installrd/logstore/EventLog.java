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
import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.event.EventLogRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class EventLog {

    @Transactional
    public void put( EventLogRecord record ) {
        logEvent( record.getEvent() );
        final EventLogEntity entity = new EventLogEntity();
        entity.namespace = record.getNamespace();
        entity.tx_id = record.getTxId();
        entity.id = UUID.randomUUID();
        entity.event = record.getEvent();
        entity.persistAndFlush();
    }

    public List<EventLogRecord> getAll( String namespace, UUID txId ) {
        return EventLogEntity
            .stream( "namespace = ?1 and tx_id = ?2", namespace, txId )
            .map( x -> ( (EventLogEntity)x ).toEventLogRecord() )
            .sorted( ( x1, x2 ) -> x1.getEvent().timestamp().compareTo( x2.getEvent().timestamp() ) )
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
