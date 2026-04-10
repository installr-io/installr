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
package io.installr.installrd.deployment.deployer.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.installr.lib.deployer.Deployer;
import io.installr.lib.deployer.DeploymentSpec;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.util.FileUtil;

public class LocalFileDeployer implements Deployer {

    private final LocalFileDeployerConfig config;

    public LocalFileDeployer( LocalFileDeployerConfig config ) {
        this.config = config;
    }

    @Override
    public void deploy( DeploymentSpec deployment ) throws DeploymentException {
        try {
            final Path target = config.getDirectory();
            if ( !Files.exists( target ) ) {
                Files.createDirectories( target );
            }

            final Path file = target.resolve( deployment.getName() );
            final byte[] data = deployment.getData();
            Files.write( file, data );
            if ( FileUtil.isExecutable( data ) ) {
                FileUtil.chmod( file, 754 );
            }
        }
        catch ( IOException ex ) {
            throw new DeploymentException( "%s", ex.getMessage() );
        }
    }

    @Override
    public void undeploy( DeploymentSpec deployment ) throws DeploymentException {
        try {
            Files.deleteIfExists(
                config.getDirectory().resolve( deployment.getName() )
            );
        }
        catch ( IOException ex ) {
            throw new DeploymentException( "%s", ex.getMessage() );
        }
    }

}
