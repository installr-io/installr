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

import io.installr.lib.deployer.Deployer;
import io.installr.lib.deployer.DeploymentSpec;
import io.installr.lib.deployer.DeploymentException;

public class NoOpDeployer implements Deployer {

    public static final Deployer INSTANCE = new NoOpDeployer();

    @Override
    public void deploy( DeploymentSpec deployment ) throws DeploymentException {
    }

    @Override
    public void undeploy( DeploymentSpec deployment ) throws DeploymentException {
    }

}
