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

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import io.installr.lib.installrd.model.NamespaceInfo;
import io.installr.lib.installrd.resource.NamespaceResource;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path( "/" )
@Consumes( MediaType.APPLICATION_JSON )
@Produces( MediaType.APPLICATION_JSON )
public interface NamespaceController {

    @Path( "/namespaces" )
    @POST
    RestResponse<NamespaceInfo> create( @Valid NamespaceResource resource );

    @Path( "/namespaces" )
    @GET
    List<NamespaceInfo> namespaces();

    @Path( "/namespaces/{namespace}" )
    @GET
    NamespaceInfo namespace( @RestPath String namespace );

    @Path( "/namespaces/{namespace}" )
    @DELETE
    void delete( @RestPath String namespace );

}
