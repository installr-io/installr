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
package io.installr.installrd.deployment.deployer.http;

import java.util.function.Function;

import org.jboss.resteasy.reactive.RestResponse;

import io.installr.installrd.common.http.HttpClientBuilder;
import io.installr.installrd.deployment.DeploymentWarning;
import io.installr.installrd.deployment.deployer.BuiltInDeployer;
import io.installr.lib.deployer.ConfigurableDeployer;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentResponse;
import io.installr.lib.deployer.DeploymentSpec;
import jakarta.enterprise.context.Dependent;
import lombok.extern.jbosslog.JBossLog;

@Dependent
@BuiltInDeployer( "HttpDeployer" )
@JBossLog
public class HttpDeployer implements ConfigurableDeployer<HttpDeployerConfig> {

    private HttpDeployerConfig config;
    private HttpDeployerClient client;

    @Override
    public Class<HttpDeployerConfig> getConfigType() {
        return HttpDeployerConfig.class;
    }

    @Override
    public void init( HttpDeployerConfig config ) {
        this.config = config;
        this.client = HttpClientBuilder.build( this.config, HttpDeployerClient.class );
    }

    @Override
    public void deploy( DeploymentSpec spec ) throws DeploymentException {
        handleTask( client::deploy, spec );
    }

    @Override
    public void undeploy( DeploymentSpec spec ) throws DeploymentException {
        handleTask( client::undeploy, spec );
    }

    private void handleTask( Function<DeploymentSpec, RestResponse<DeploymentResponse>> task, DeploymentSpec spec ) {
        final RestResponse<DeploymentResponse> response = task.apply( spec );
        // sync
        if ( response.getStatus() == RestResponse.Status.OK.getStatusCode() ) {
            final DeploymentResponse statusResponse = response.getEntity();
            switch ( statusResponse.getStatus() ) {
                case Queued:
                case InProgress:
                case Success:
                    break;
                case Warning:
                    throw new DeploymentWarning( statusResponse.getMessage() );
                case Failed:
                    throw new DeploymentException( statusResponse.getMessage() );
            }
        }
        // async
        else if ( response.getStatus() == RestResponse.Status.ACCEPTED.getStatusCode() ) {
            boolean loop = true;
            while ( loop ) {
                RestResponse<DeploymentResponse> serverResponse = client.status( spec.getName() );
                final DeploymentResponse statusResponse = serverResponse.getEntity();
                switch ( statusResponse.getStatus() ) {
                    case Queued:
                    case InProgress:
                        break;
                    case Success:
                        loop = false;
                        break;
                    case Warning:
                        throw new DeploymentWarning( statusResponse.getMessage() );
                    case Failed:
                        throw new DeploymentException( statusResponse.getMessage() );
                }
                try {
                    Thread.sleep( 500 );
                }
                catch ( InterruptedException ex ) {
                    Thread.interrupted();
                    log.warn( "Current thread was interrupted." );
                    break;
                }
            }
        }
        else {
            throw new IllegalStateException(
                String.format( "HttpDeployer: Unsupported http status %d", response.getStatus() )
            );
        }
    }

    @Override
    public String toString() {
        return String.format(
            "HttpDeployer: {url: %s, auth: %s}",
            config.getUrl(),
            config.getAuth().getType()
        );
    }

}
