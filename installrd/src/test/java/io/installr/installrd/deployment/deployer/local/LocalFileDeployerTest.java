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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.installr.lib.deployer.DeploymentSpec;
import io.installr.lib.util.FileUtil;

class LocalFileDeployerTest {

    @Test
    void deploy_should_create_the_file_and_set_exec_flag() throws IOException {
        final String filename = "script.sh";
        final Path target = Path.of( "target" ).resolve( "test-local-file-deployer" ).toAbsolutePath();
        final Path file = target.resolve( filename );
        assertFalse( Files.exists( file ), "file should not exist before deploy" );

        try {
            final DeploymentSpec deployment = new DeploymentSpec();
            deployment.setName( filename );
            deployment.setMediaType( "application/vnd.io.installr.binary" );
            deployment.setData( "#!/bin/bash\n\necho 'Hello World'\n".getBytes() );

            final LocalFileDeployer deployer = new LocalFileDeployer( new LocalFileDeployerConfig( target ) );
            deployer.deploy( deployment );

            assertTrue( Files.exists( file ), "File should exist after deploy" );
            assertTrue( FileUtil.isExecutable( Files.readAllBytes( file ) ), "File should be executable" );
        }
        finally {
            Files.deleteIfExists( file );
            FileUtil.rmdir( target );
        }
    }

    @Test
    void undeploy_should_remove_the_file() throws IOException {
        final String filename = "a-script.sh";
        final Path target = Path.of( "target" ).resolve( "test-local-file-deployer" );
        final Path file = target.resolve( filename );

        Files.createDirectories( target );
        Files.write( file, "Some content\n".getBytes() );
        assertTrue( Files.exists( file ), "File should exist before undeploy" );

        try {
            final DeploymentSpec deployment = new DeploymentSpec();
            deployment.setName( filename );
            deployment.setMediaType( "application/vnd.io.installr.binary" );

            final LocalFileDeployer deployer = new LocalFileDeployer( new LocalFileDeployerConfig( target ) );
            deployer.undeploy( deployment );

            assertFalse( Files.exists( file ), "File should not exist after undeploy" );
        }
        finally {
            FileUtil.rmdir( target );
        }
    }

}
