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
package io.installr.deployer;

import java.util.Random;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import io.installr.installrd.util.ThreadUtil;
import io.installr.lib.deployer.DeploymentResponse;
import io.installr.lib.deployer.DeploymentSpec;
import io.installr.lib.deployer.DeploymentStatus;
import io.installr.lib.deployer.resource.DeployerResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
@Path( "/deployer-mock" )
public class DeployerMockServer implements DeployerResource {

    @ServerRequestFilter
    void filter( ContainerRequestContext rc ) {
        // System.out.println( rc );
    }

    @Context
    HttpHeaders httpHeaders;

    @Override
    public RestResponse<DeploymentResponse> deploy( @Valid DeploymentSpec deployment ) {
        ThreadUtil.sleep( new Random().nextLong( 500, 1000 ) );
        final String token = getToken();
        if ( token == null || token.isBlank() ) {
            return RestResponse.ok(
                DeploymentResponse.builder().status( DeploymentStatus.Failed ).message( "Token is null" )
                    .build()
            );
        }
        if ( !token.equals( deployment.getEnv().get( "namespace" ) ) ) {
            return RestResponse.ok(
                DeploymentResponse.builder().status( DeploymentStatus.Failed ).message( "Invalid token: " + token )
                    .build()
            );
        }
        return RestResponse.ok( DeploymentResponse.builder().status( DeploymentStatus.Success ).build() );
    }

    private String getToken() {
        final String key = "x-auth-token";
        final MultivaluedMap<String, String> requestHeaders = httpHeaders.getRequestHeaders();
        if ( !requestHeaders.containsKey( key ) ) {
            return null;
        }
        return requestHeaders.get( key ).getFirst();
    }

    @Override
    public RestResponse<DeploymentResponse> status( @Valid @NotBlank String name ) {
        throw new UnsupportedOperationException( "'DeployerMockserver.status' not implemented." );
    }

    @Override
    public RestResponse<DeploymentResponse> undeploy( @Valid DeploymentSpec deployment ) {
        throw new UnsupportedOperationException( "'DeployerMockserver.status' not implemented." );
    }

}
