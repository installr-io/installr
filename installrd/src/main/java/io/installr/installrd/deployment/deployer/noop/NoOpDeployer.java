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
package io.installr.installrd.deployment.deployer.noop;

import io.installr.installrd.deployment.deployer.BuiltInDeployer;
import io.installr.lib.deployer.AbstractConfigurableDeployer;
import io.installr.lib.deployer.DeployerConfig;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentSpec;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@BuiltInDeployer( "NoOpDeployer" )
public class NoOpDeployer extends AbstractConfigurableDeployer<DeployerConfig> {

    @Override
    public Class<DeployerConfig> getConfigType() {
        return null; // No configuration needed
    }

    @Override
    public void deploy( DeploymentSpec spec ) throws DeploymentException {
    }

    @Override
    public void undeploy( DeploymentSpec spec ) throws DeploymentException {
    }

}
