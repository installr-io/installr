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

import static io.installr.installrd.util.ExceptionUtil.wrap;

import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.function.Function;

import org.jboss.resteasy.reactive.RestResponse;

import io.installr.installrd.common.http.HttpClientBuilder;
import io.installr.installrd.deployment.DeploymentWarning;
import io.installr.installrd.deployment.deployer.BuiltInDeployer;
import io.installr.installrd.util.PeriodicEvaluator;
import io.installr.lib.deployer.AbstractConfigurableDeployer;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentResponse;
import io.installr.lib.deployer.DeploymentSpec;
import io.smallrye.faulttolerance.api.Guard;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.jbosslog.JBossLog;

@Dependent
@BuiltInDeployer( "HttpDeployer" )
@JBossLog
public class HttpDeployer extends AbstractConfigurableDeployer<HttpDeployerConfig> {

    private HttpDeployerClient client;

    @Override
    public Class<HttpDeployerConfig> getConfigType() {
        return HttpDeployerConfig.class;
    }

    @Override
    public void init( HttpDeployerConfig config ) {
        super.init( config );
        this.client = HttpClientBuilder.build( this.config, HttpDeployerClient.class );
    }

    @Override
    public void deploy( DeploymentSpec spec ) throws DeploymentException {
        handleResponse( client::deploy, spec );
    }

    @Override
    public void undeploy( DeploymentSpec spec ) throws DeploymentException {
        handleResponse( client::undeploy, spec );
    }

    @SuppressWarnings( "unchecked" )
    private void handleResponse(
        Function<DeploymentSpec, RestResponse<DeploymentResponse>> task, DeploymentSpec spec
    ) {
        // spec.getName() contains forward slashes and dots but is part of the request path
        DeploymentSpec encodedSpec = encodeSpec( spec );

        // @formatter:off
        final RestResponse<DeploymentResponse> response = wrap(
            () -> Guard.create()
                .withRetry()
                    .maxDuration( config.getPollingTimeout().toMillis(), ChronoUnit.MILLIS )
                    .delay( config.getPollingInterval().toMillis(), ChronoUnit.MILLIS )
                    .retryOn( NotFoundException.class )
                    .done()
                .build()
                .call( () -> task.apply( encodedSpec ), RestResponse.class ),
            this::translateException
        ).get();
        // @formatter:on
        checkResponse( response, encodedSpec );
    }

    private DeploymentSpec encodeSpec( DeploymentSpec spec ) {
        final DeploymentSpec encoded = new DeploymentSpec();
        encoded.setName( Base64.getEncoder().encodeToString( spec.getName().getBytes() ) );
        encoded.setVersion( spec.getVersion() );
        encoded.setData( spec.getData() );
        encoded.setHash( spec.getHash() );
        encoded.setMediaType( spec.getMediaType() );
        encoded.setEncoding( spec.getEncoding() );
        encoded.setEnv( spec.getEnv() );
        return encoded;
    }

    private void checkResponse( RestResponse<DeploymentResponse> response, DeploymentSpec spec ) {
        // sync
        if ( response.getStatus() == RestResponse.Status.OK.getStatusCode() ) {
            checkResponseSync( response );
        }
        // async
        else if ( response.getStatus() == RestResponse.Status.ACCEPTED.getStatusCode() ) {
            checkResponseAsync( spec );
        }
        else {
            throw new IllegalStateException(
                String.format( "HttpDeployer: Unsupported http status %d", response.getStatus() )
            );
        }
    }

    private void checkResponseSync( RestResponse<DeploymentResponse> response ) {
        final DeploymentResponse statusResponse = response.getEntity();
        switch ( statusResponse.getStatus() ) {
            case Queued:
            case InProgress:
                throw new IllegalStateException(
                    String.format(
                        "HttpDeployer: Unexpected status %s in synchronous response",
                        statusResponse.getStatus()
                    )
                );
            case Success:
                break;
            case Warning:
                throw new DeploymentWarning( statusResponse.getMessage() );
            case Failed:
                throw new DeploymentException( statusResponse.getMessage() );
        }
    }

    private void checkResponseAsync( DeploymentSpec spec ) {
        final PeriodicEvaluator<RestResponse<DeploymentResponse>> eval = new PeriodicEvaluator<>(
            wrap( () -> client.status( spec.getName() ), this::translateException ),
            ( res ) -> res.getEntity().getStatus().isFinished()
        )
            .withInterval( config.getPollingInterval() )
            .withTimeout( config.getPollingTimeout() );
        if ( !eval.evaluate() ) {
            throw new DeploymentException( "Timeout waiting for async deployment to complete" );
        }
        final DeploymentResponse statusResponse = eval.getValue().getEntity();
        switch ( statusResponse.getStatus() ) {
            case Success:
                break;
            case Warning:
                throw new DeploymentWarning( statusResponse.getMessage() );
            case Failed:
                throw new DeploymentException( statusResponse.getMessage() );
            default:
                throw new IllegalStateException(
                    String.format( "HttpDeployer: Unexpected deployment status %s", statusResponse.getStatus() )
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
