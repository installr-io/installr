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
package io.installr.installrd.transaction;

import java.util.List;
import java.util.UUID;

import io.installr.installrd.logstore.EventLog;
import io.installr.installrd.logstore.TransactionLog;
import io.installr.installrd.security.NamespacePermission;
import io.installr.installrd.security.Operation.Names;
import io.installr.lib.installrd.controller.TransactionController;
import io.installr.lib.installrd.event.EventLogRecord;
import io.installr.lib.installrd.transaction.TransactionLogRecord;
import io.installr.lib.installrd.util.Pagination;
import io.quarkus.security.PermissionsAllowed;
import jakarta.inject.Inject;

public class TransactionControllerImpl implements TransactionController {

    @Inject
    TransactionLog txLog;

    @Inject
    EventLog eventLog;

    @Inject
    TransactionService txService;

    @Override
    @PermissionsAllowed( value = Names.TX_READ, permission = NamespacePermission.class, params = "namespace" )
    public List<TransactionLogRecord> transactions( String namespace, Pagination pagination ) {
        if ( pagination.getPage() == -1 ) {
            return txLog.getAll( namespace );
        }
        return txLog.getPaged( namespace, pagination );
    }

    @Override
    @PermissionsAllowed( value = Names.TX_READ, permission = NamespacePermission.class, params = "namespace" )
    public TransactionLogRecord transaction( String namespace, UUID txId ) {
        return txLog.get( namespace, txId );
    }

    @Override
    @PermissionsAllowed( value = Names.TX_READ, permission = NamespacePermission.class, params = "namespace" )
    public List<EventLogRecord> events( String namespace, UUID txId ) {
        return eventLog.getAll( namespace, txId );
    }

}
