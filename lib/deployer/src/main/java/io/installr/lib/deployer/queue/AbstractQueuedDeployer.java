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
package io.installr.lib.deployer.queue;

import java.time.Duration;

import io.installr.lib.deployer.Deployer;
import io.installr.lib.deployer.DeploymentSpec;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentResponse;

public abstract class AbstractQueuedDeployer implements Deployer {

    private final SingleThreadDeploymentQueue deploymentQueue;

    public AbstractQueuedDeployer() {
        this.deploymentQueue = new SingleThreadDeploymentQueue();
    }

    public AbstractQueuedDeployer( Duration retention ) {
        this.deploymentQueue = new SingleThreadDeploymentQueue( retention );
    }

    @Override
    public final void deploy( DeploymentSpec spec ) throws DeploymentException {
        deploymentQueue.enqueue(
            spec.getName(),
            () -> {
                doDeploy( spec );
            }
        );
    }

    @Override
    public final void undeploy( DeploymentSpec spec ) throws DeploymentException {
        deploymentQueue.enqueue(
            spec.getName(),
            () -> {
                doUndeploy( spec );
            }
        );
    }

    public final DeploymentResponse status( String name ) {
        return deploymentQueue.getStatus( name );
    }

    protected abstract void doDeploy( DeploymentSpec spec ) throws DeploymentException;

    protected abstract void doUndeploy( DeploymentSpec spec ) throws DeploymentException;

}
