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

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestStreamElementType;

import io.installr.lib.installrd.event.Event;
import io.installr.lib.installrd.resource.InstallFileResource;
import io.installr.lib.installrd.resource.InstallResource;
import io.smallrye.mutiny.Multi;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path( "/namespaces/{namespace}" )
@Consumes( MediaType.APPLICATION_JSON )
@Produces( MediaType.APPLICATION_JSON )
public interface InstallController {

    @Path( "/install" )
    @POST
    @Produces( MediaType.SERVER_SENT_EVENTS )
    @RestStreamElementType( MediaType.APPLICATION_JSON )
    Multi<Event> install( @RestPath String namespace, @Valid InstallResource resource );

    @Path( "/install-file" )
    @POST
    @Produces( MediaType.SERVER_SENT_EVENTS )
    @RestStreamElementType( MediaType.APPLICATION_JSON )
    Multi<Event> install( @RestPath String namespace, @Valid InstallFileResource resource );

    @Path( "/reinstall" )
    @POST
    @Produces( MediaType.SERVER_SENT_EVENTS )
    @RestStreamElementType( MediaType.APPLICATION_JSON )
    Multi<Event> reinstall( @RestPath String namespace, @Valid InstallResource resource );

    @Path( "/reinstall-file" )
    @POST
    @Produces( MediaType.SERVER_SENT_EVENTS )
    @RestStreamElementType( MediaType.APPLICATION_JSON )
    Multi<Event> reinstall( @RestPath String namespace, @Valid InstallFileResource resource );

    @Path( "/cancel" )
    @POST
    void cancel( @RestPath String namespace );

}
