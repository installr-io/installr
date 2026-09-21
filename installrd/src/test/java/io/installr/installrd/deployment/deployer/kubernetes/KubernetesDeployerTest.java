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
package io.installr.installrd.deployment.deployer.kubernetes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.semver4j.Semver;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.installr.installrd.util.interpolation.EnvironmentInterpolator;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentSpec;
import lombok.SneakyThrows;

// We use fabric8's built-in mock client instead of @QuarkusTest to avoid starting the full application.
@EnableKubernetesMockClient( crud = true, https = false )
public class KubernetesDeployerTest {

    private static final String NAMESPACE = "test-namespace";

    // Client gets injected automatically
    KubernetesClient client;

    KubernetesDeployer deployer() {
        return deployer( new KubernetesDeployerConfig() );
    }

    KubernetesDeployer deployer( KubernetesDeployerConfig config ) {
        config.setApiServerUrl( client.getMasterUrl().toString() );
        final KubernetesDeployer deployer = new KubernetesDeployer();
        deployer.interpolator = new EnvironmentInterpolator();
        deployer.init( config );
        return deployer;
    }

    @SneakyThrows
    byte[] readDescriptor( String name ) {
        final URI root = KubernetesDeployerTest.class.getClassLoader().getResource( "deployer/k8s" ).toURI();
        return Files.readAllBytes( Path.of( root ).resolve( name ) );
    }

    DeploymentSpec genSpec( String name ) {
        final DeploymentSpec spec = new DeploymentSpec();
        spec.setName( name );
        spec.setVersion( Semver.parse( "0.1.0" ) );
        spec.setMediaType( "application/vnd.io.installr.kubernetes" );
        spec.setEnv( Map.of( "namespace", NAMESPACE ) );
        spec.setData( readDescriptor( name + ".yaml" ) );
        return spec;
    }

    @Test
    void deploy_should_create_resources() {
        final KubernetesDeployer deployer = deployer();
        final DeploymentSpec spec = genSpec( "test-k8s-1" );

        assertDoesNotThrow( () -> deployer.deploy( spec ) );

        final ConfigMap cm = client.configMaps()
            .inNamespace( NAMESPACE )
            .withName( "test-k8s-1" )
            .get();
        assertNotNull( cm, "ConfigMap should exist after deploy" );
    }

    @Test
    void undeploy_should_remove_resources() {
        final KubernetesDeployer deployer = deployer();
        final DeploymentSpec spec = genSpec( "test-k8s-1" );

        assertDoesNotThrow( () -> deployer.undeploy( spec ) );

        final ConfigMap cm = client.configMaps()
            .inNamespace( NAMESPACE )
            .withName( "test-k8s-1" )
            .get();
        assertNull( cm, "ConfigMap should be gone after undeploy" );
    }

    @Test
    void deploy_should_update_existing_resources() {
        final ConfigMap existing = new ConfigMapBuilder()
            .withNewMetadata().withName( "test-k8s-1" ).withNamespace( NAMESPACE ).endMetadata()
            .addToData( "key", "old-value" )
            .build();
        client.configMaps().inNamespace( NAMESPACE ).resource( existing ).create();

        final KubernetesDeployer deployer = deployer();
        final DeploymentSpec spec = genSpec( "test-k8s-1" );

        assertDoesNotThrow( () -> deployer.deploy( spec ) );

        final ConfigMap cm = client.configMaps()
            .inNamespace( NAMESPACE )
            .withName( "test-k8s-1" )
            .get();
        assertNotNull( cm );
        assertEquals( "value", cm.getData().get( "key" ), "ConfigMap data should be updated" );
    }

    @Test
    void deploy_should_throw_when_workload_does_not_become_ready() {
        final KubernetesDeployerConfig config = new KubernetesDeployerConfig();
        config.setPollingTimeout( Duration.ofSeconds( 3 ) );
        final KubernetesDeployer deployer = deployer( config );
        final DeploymentSpec spec = genSpec( "test-k8s-2" );

        // The mock server never updates Deployment status, so polling will time out
        assertThrows( DeploymentException.class, () -> deployer.deploy( spec ) );
    }

}
