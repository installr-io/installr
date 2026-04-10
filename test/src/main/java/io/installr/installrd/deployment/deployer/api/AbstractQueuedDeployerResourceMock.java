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
package io.installr.installrd.deployment.deployer.api;

import java.time.Duration;

import io.installr.installrd.util.ThreadUtil;
import io.installr.lib.deployer.DeploymentSpec;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.queue.AbstractQueuedDeployer;
import io.installr.lib.deployer.resource.AbstractQueuedDeployerResource;
import jakarta.ws.rs.Path;

@Path( "/test" )
public class AbstractQueuedDeployerResourceMock extends AbstractQueuedDeployerResource {

    public static final Duration DEPLOY_TIME = Duration.ofSeconds( 3 );

    private static class DeployerMock extends AbstractQueuedDeployer {

        DeployerMock() {
            super( DEPLOY_TIME.multipliedBy( 2L ) );
        }

        @Override
        protected void doDeploy( DeploymentSpec deployment ) throws DeploymentException {
            ThreadUtil.sleep( DEPLOY_TIME );
        }

        @Override
        protected void doUndeploy( DeploymentSpec deployment ) throws DeploymentException {
        }
    }

    public AbstractQueuedDeployerResourceMock() {
        super.setDeployer( new DeployerMock() );
    }

}
