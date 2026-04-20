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
import org.jboss.resteasy.reactive.RestResponse.Status;

import io.installr.lib.deployer.DeploymentSpec;
import io.installr.lib.deployer.DeploymentResponse;
import io.installr.lib.deployer.DeploymentStatus;
import io.installr.lib.deployer.queue.AbstractQueuedDeployer;

public abstract class AbstractQueuedDeployerResource implements DeployerResource {

    private AbstractQueuedDeployer deployer;

    public AbstractQueuedDeployer getDeployer() {
        return deployer;
    }

    public void setDeployer( AbstractQueuedDeployer deployer ) {
        this.deployer = deployer;
    }

    @Override
    public RestResponse<DeploymentResponse> deploy( DeploymentSpec spec ) {
        checkDeployer();
        deployer.deploy( spec );
        return RestResponse.accepted( deployer.status( spec.getName() ) );
    }

    @Override
    public RestResponse<DeploymentResponse> undeploy( DeploymentSpec spec ) {
        checkDeployer();
        deployer.undeploy( spec );
        return RestResponse.accepted( deployer.status( spec.getName() ) );
    }

    @Override
    public RestResponse<DeploymentResponse> status( String name ) {
        checkDeployer();
        final DeploymentResponse statusResponse = deployer.status( name );
        if ( statusResponse == null ) {
            return RestResponse.status(
                Status.BAD_REQUEST,
                DeploymentResponse.builder()
                    .status( DeploymentStatus.Failed )
                    .message( String.format( "Deployment not found: %s", name ) )
                    .build()
            );
        }
        return RestResponse.ok( statusResponse );
    }

    private void checkDeployer() {
        if ( deployer == null ) {
            throw new IllegalStateException(
                "Deployer was not set. Use AbstractQueuedDeployerResource.setDeployer to set the deployer to use."
            );
        }
    }

}
