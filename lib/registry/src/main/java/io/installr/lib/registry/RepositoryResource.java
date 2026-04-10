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
package io.installr.lib.registry;

import java.util.List;

import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import io.installr.lib.registry.repository.Repository;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path( "/repositories" )
@Produces( MediaType.APPLICATION_JSON )
public interface RepositoryResource {

    @GET
    @Path( "/" )
    List<Repository> listRepositories();

    @GET
    @Path( "/{repositoryId}" )
    Repository getRepository( @NotEmpty String repositoryId );

    @GET
    @Path( "/{repositoryId}/index" )
    @Produces( MediaType.APPLICATION_OCTET_STREAM )
    Uni<Response> index( @NotEmpty String repositoryId );

    @GET
    @Path( "/{repositoryId}/packages" )
    List<Package> getPackages( @NotEmpty String repositoryId );

    @GET
    @Path( "/{repositoryId}/packages/{packageId}" )
    @Produces( MediaType.APPLICATION_OCTET_STREAM )
    Uni<Response> getPackage( @NotEmpty String repositoryId, @NotNull PackageId packageId );

    @PUT
    @Path( "/{repositoryId}/packages" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    Response putPackage( @Valid @BeanParam PackageUpload packageUpload );

    @DELETE
    @Path( "/{repositoryId}/packages/{packageId}" )
    Response deletePackage( @NotEmpty String repositoryId, @NotNull PackageId packageId );

}
