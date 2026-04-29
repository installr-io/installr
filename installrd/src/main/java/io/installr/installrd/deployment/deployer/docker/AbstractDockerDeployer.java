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
package io.installr.installrd.deployment.deployer.docker;

import java.net.SocketException;

import de.gesellix.docker.client.DockerClient;
import de.gesellix.docker.client.DockerClientImpl;
import de.gesellix.docker.remote.api.core.ClientException;
import io.installr.installrd.util.ExceptionUtil;
import io.installr.lib.deployer.ConfigurableDeployer;
import io.installr.lib.deployer.DeploymentException;

abstract class AbstractDockerDeployer implements ConfigurableDeployer<DockerDeployerConfig> {

    DockerDeployerConfig config;
    DockerClient client;

    @Override
    public Class<DockerDeployerConfig> getConfigType() {
        return DockerDeployerConfig.class;
    }

    @Override
    public void init( DockerDeployerConfig config ) {
        this.config = config;
        this.client = new DockerClientImpl( config.getHost() );
    }

    @Override
    public DeploymentException translateException( Throwable ex ) {
        if ( ex instanceof ClientException ) {
            // toString() returns the message body
            return new DeploymentException( ex.toString() );
        }
        if ( ExceptionUtil.isCausedBy( ex, SocketException.class ) ) {
            return new DeploymentException(
                "Cannot connect to Docker socket at '%s'.",
                config.getHost()
            );
        }
        return ConfigurableDeployer.super.translateException( ex );
    }

}
