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
package io.installr.lib.deployer.resource;

import org.jboss.resteasy.reactive.RestResponse;

import io.installr.lib.deployer.DeploymentSpec;
import io.installr.lib.deployer.DeploymentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * This class represents the standard deployment interface for all deployers accessible over http(s).
 *
 * <pre>
 * Specification:
 *      /Deployer.deploy/{name} and /Deployer.undeploy/{name}
 *          status 200  synchron execution; DeploymentResponse.status must not be Queued or InProgress
 *          status 202  asynchron execution; DeploymentResponse.status must be Queued or InProgress
 *          status 400  in case of an error
 *
 *      /Deployer.status/{name}
 *          status 200  if operation was successful
 *          status 400  in case of an error
 * </pre>
 */
public interface DeployerResource {

    @PUT
    @Path( "/Deployer.deploy/{name}" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.APPLICATION_JSON )
    RestResponse<DeploymentResponse> deploy( @BeanParam @Valid DeploymentSpec deployment );

    @DELETE
    @Path( "/Deployer.undeploy/{name}" )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.APPLICATION_JSON )
    RestResponse<DeploymentResponse> undeploy( @BeanParam @Valid DeploymentSpec deployment );

    @GET
    @Path( "/Deployer.status/{name}" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    RestResponse<DeploymentResponse> status( @Valid @NotBlank String name );

}
