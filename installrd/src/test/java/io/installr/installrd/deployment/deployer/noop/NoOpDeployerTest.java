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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import io.installr.lib.deployer.DeploymentSpec;

class NoOpDeployerTest {

    @Test
    void deploy_should_not_throw() {
        final DeploymentSpec spec = new DeploymentSpec();
        spec.setName( "test-artifact" );
        spec.setMediaType( "application/vnd.io.installr.noop" );

        assertDoesNotThrow( () -> new NoOpDeployer().deploy( spec ) );
    }

    @Test
    void undeploy_should_not_throw() {
        final DeploymentSpec spec = new DeploymentSpec();
        spec.setName( "test-artifact" );
        spec.setMediaType( "application/vnd.io.installr.noop" );

        assertDoesNotThrow( () -> new NoOpDeployer().undeploy( spec ) );
    }

}
