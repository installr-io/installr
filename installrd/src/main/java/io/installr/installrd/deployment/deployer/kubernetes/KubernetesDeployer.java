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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.installr.installrd.deployment.deployer.BuiltInDeployer;
import io.installr.installrd.util.PeriodicEvaluator;
import io.installr.installrd.util.interpolation.EnvironmentInterpolator;
import io.installr.lib.deployer.ConfigurableDeployer;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentSpec;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

@Dependent
@BuiltInDeployer( "KubernetesDeployer" )
@JBossLog
public class KubernetesDeployer implements ConfigurableDeployer<KubernetesDeployerConfig> {

    @Inject
    EnvironmentInterpolator interpolator;

    private KubernetesClient client;
    private KubernetesDeployerConfig config;

    @Override
    public Class<KubernetesDeployerConfig> getConfigType() {
        return KubernetesDeployerConfig.class;
    }

    @Override
    public void init( KubernetesDeployerConfig config ) {
        this.config = config;
        final KubernetesClientBuilder builder = new KubernetesClientBuilder();
        if ( config.getApiServerUrl() != null ) {
            final Config k8sConfig = new ConfigBuilder().withMasterUrl( config.getApiServerUrl() ).build();
            builder.withConfig( k8sConfig );
        }
        this.client = builder.build();
    }

    @Override
    public DeploymentException translateException( Throwable ex ) {
        if ( ex instanceof KubernetesClientException kce ) {
            final Throwable cause = kce.getCause() == null ? kce : kce.getCause();
            final String message = cause.getMessage() != null ? cause.getMessage() : cause.toString();
            return new DeploymentException( message );
        }
        return ConfigurableDeployer.super.translateException( ex );
    }

    @Override
    public void deploy( DeploymentSpec spec ) throws DeploymentException {
        final String namespace = namespace( spec );
        log.debugf( "Deploying '%s' to namespace '%s'...", spec.getName(), namespace );

        final var loaded = client.load( loadSpec( spec ) );
        for ( final HasMetadata resource : loaded.items() ) {
            client.resource( resource ).inNamespace( namespace ).createOr( r -> r.update() );
        }

        final List<HasMetadata> resources = loaded.items();
        final boolean ready = new PeriodicEvaluator<>( () -> resources, checkAllReady( namespace ) )
            .withInterval( config.getPollingInterval() )
            .withTimeout( config.getPollingTimeout() )
            .evaluate();

        if ( !ready ) {
            throw new DeploymentException(
                "Could not deploy '%s'. Check 'kubectl describe pods -n %s' for details.",
                spec.getName(),
                namespace
            );
        }
        log.debug( "  => done" );
    }

    @Override
    public void undeploy( DeploymentSpec spec ) throws DeploymentException {
        final String namespace = namespace( spec );
        log.debugf( "Un-deploying '%s' from namespace '%s'...", spec.getName(), namespace );

        final var loaded = client.load( loadSpec( spec ) );
        final List<HasMetadata> resources = loaded.items();
        loaded.inNamespace( namespace ).delete();

        final boolean gone = new PeriodicEvaluator<>( () -> resources, checkAllGone( namespace ) )
            .withInterval( config.getPollingInterval() )
            .withTimeout( config.getPollingTimeout() )
            .evaluate();

        if ( !gone ) {
            throw new DeploymentException(
                "Could not undeploy '%s'. Check 'kubectl describe pods -n %s' for details.",
                spec.getName(),
                namespace
            );
        }
        log.debug( "  => done" );
    }

    private String namespace( DeploymentSpec spec ) {
        final String namespace = spec.getEnv().get( "namespace" );
        if ( namespace == null || namespace.isBlank() ) {
            throw new IllegalStateException( "Namespace must be provided via env." );
        }
        return namespace.toLowerCase();
    }

    private InputStream loadSpec( DeploymentSpec spec ) {
        final String yaml = interpolator
            .interpolate( new String( spec.getData(), StandardCharsets.UTF_8 ), spec.getEnv() );
        return new ByteArrayInputStream( yaml.getBytes( StandardCharsets.UTF_8 ) );
    }

    private Predicate<List<HasMetadata>> checkAllReady( String namespace ) {
        return resources -> resources.stream().allMatch( r -> isReady( r, namespace ) );
    }

    private Predicate<List<HasMetadata>> checkAllGone( String namespace ) {
        return resources -> resources.stream().allMatch( r -> isGone( r, namespace ) );
    }

    private boolean isGone( HasMetadata resource, String namespace ) {
        return client.resource( resource ).inNamespace( namespace ).get() == null;
    }

    private boolean isReady( HasMetadata resource, String namespace ) {
        final String name = resource.getMetadata().getName();
        if ( resource instanceof Deployment ) {
            return isDeploymentReady( name, namespace );
        }
        if ( resource instanceof StatefulSet ) {
            return isStatefulSetReady( name, namespace );
        }
        if ( resource instanceof DaemonSet ) {
            return isDaemonSetReady( name, namespace );
        }
        // ConfigMap, Service, Secret, etc. are immediately available after creation
        return true;
    }

    private boolean isDeploymentReady( String name, String namespace ) {
        final Deployment deployment = client.apps().deployments()
            .inNamespace( namespace ).withName( name ).get();
        if ( deployment == null || deployment.getStatus() == null ) {
            return false;
        }
        final int desired = deployment.getSpec().getReplicas() != null
            ? deployment.getSpec().getReplicas()
            : 1;
        final Integer available = deployment.getStatus().getAvailableReplicas();
        return available != null && available >= desired;
    }

    private boolean isStatefulSetReady( String name, String namespace ) {
        final StatefulSet sts = client.apps().statefulSets()
            .inNamespace( namespace ).withName( name ).get();
        if ( sts == null || sts.getStatus() == null ) {
            return false;
        }
        final int desired = sts.getSpec().getReplicas() != null
            ? sts.getSpec().getReplicas()
            : 1;
        final Integer ready = sts.getStatus().getReadyReplicas();
        return ready != null && ready >= desired;
    }

    private boolean isDaemonSetReady( String name, String namespace ) {
        final DaemonSet ds = client.apps().daemonSets()
            .inNamespace( namespace ).withName( name ).get();
        if ( ds == null || ds.getStatus() == null ) {
            return false;
        }
        final Integer desired = ds.getStatus().getDesiredNumberScheduled();
        final Integer ready = ds.getStatus().getNumberReady();
        return desired != null && ready != null && ready >= desired;
    }

}
