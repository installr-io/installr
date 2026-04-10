/*
 * Installr.io
 *
 * Copyright (c) 2025-present Dorian Vallant
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.installr.lib.installrd.controller;

import java.util.List;
import java.util.UUID;

import org.jboss.resteasy.reactive.RestPath;

import io.installr.lib.installrd.event.EventLogRecord;
import io.installr.lib.installrd.transaction.TransactionLogRecord;
import io.installr.lib.installrd.util.Pagination;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path( "/namespaces/{namespace}" )
public interface TransactionController {

    @GET
    @Path( "/transactions" )
    List<TransactionLogRecord> transactions( @RestPath String namespace, @BeanParam Pagination pagination );

    @GET
    @Path( "/transactions/{transactionId}" )

    TransactionLogRecord transaction( @RestPath String namespace, @RestPath UUID transactionId );

    @GET
    @Path( "/transactions/{transactionId}/events" )
    List<EventLogRecord> events( @RestPath String namespace, @RestPath UUID transactionId );

}
