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
package io.installr.installrd.deployment;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.DeployerConfiguration;
import io.installr.installrd.deployment.deployer.docker.DockerDeployerConfig;
import io.installr.installrd.deployment.deployer.docker.DockerNetworkDeployer;
import io.installr.installrd.deployment.deployer.docker.DockerStackDeployer;
import io.installr.installrd.deployment.deployer.http.HttpDeployer;
import io.installr.installrd.deployment.deployer.http.HttpDeployerConfig;
import io.installr.installrd.deployment.deployer.local.LocalFileDeployer;
import io.installr.installrd.deployment.deployer.local.LocalFileDeployerConfig;
import io.installr.installrd.deployment.deployer.noop.NoOpDeployer;
import io.installr.installrd.plugin.PluginLoader;
import io.installr.installrd.util.PropertyMapUtil;
import io.installr.lib.deployer.Deployer;
import io.installr.lib.deployer.DeployerPlugin;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentSpec;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class DeploymentService {

    @Inject
    InstallrdConfiguration config;

    @Inject
    PluginLoader pluginLoader;

    private Map<String, Deployer> deployerMap;

    @PostConstruct
    void init() {
        deployerMap = config.deployerConfigurations()
            .entrySet()
            .stream()
            .collect( Collectors.toMap( Map.Entry::getKey, this::mapToDeployer ) );
    }

    private Deployer mapToDeployer( Map.Entry<String, DeployerConfiguration> entry ) {
        final DeployerConfiguration config = entry.getValue();
        switch ( config.type() ) {
            case "NoOpDeployer":
                return new NoOpDeployer();
            case "LocalFileDeployer":
                return new LocalFileDeployer(
                    PropertyMapUtil.readPropertyMap( config.config(), LocalFileDeployerConfig.class )
                );
            case "HttpDeployer":
                return new HttpDeployer(
                    PropertyMapUtil.readPropertyMap( config.config(), HttpDeployerConfig.class )
                );
            case "DockerStackDeployer":
                return new DockerStackDeployer(
                    PropertyMapUtil.readPropertyMap( config.config(), DockerDeployerConfig.class )
                );
            case "DockerNetworkDeployer":
                return new DockerNetworkDeployer(
                    PropertyMapUtil.readPropertyMap( config.config(), DockerDeployerConfig.class )
                );
            default:
                return findDeployer( config.type(), entry.getKey(), config.config() );
        }
    }

    private DeployerPlugin findDeployer( String deployerName, String mediaType, Map<String, String> config ) {
        final Optional<Map<String, DeployerPlugin>> plugins = pluginLoader.getDeployers();
        if ( !plugins.isPresent() ) {
            throw new IllegalStateException( String.format( "No deployer found for media-type '%s'.", mediaType ) );
        }
        final Map<String, DeployerPlugin> deployers = plugins.get();
        if ( !deployers.containsKey( deployerName ) ) {
            throw new IllegalStateException( "Deployer not found: " + deployerName );
        }
        final DeployerPlugin deployer = deployers.get( deployerName );
        deployer.init( config );
        return deployer;
    }

    public DeploymentSpec deploy( DeploymentSpec spec ) {
        return handleError( spec, deployer -> deployer.deploy( spec ) );
    }

    public DeploymentSpec undeploy( DeploymentSpec spec ) {
        return handleError( spec, deployer -> deployer.undeploy( spec ) );
    }

    private DeploymentSpec handleError( DeploymentSpec spec, Consumer<Deployer> consumer ) {
        final Deployer deployer = getDeployer( spec );
        try {
            consumer.accept( deployer );
            return spec;
        }
        catch ( Throwable t ) {
            log.debugf( t, "STACKTRACE" );
            if ( t instanceof DeploymentWarning ) {
                throw t;
            }
            throw new DeploymentException(
                "[%s] %s",
                deployer.getClass().getSimpleName(),
                t.getMessage() == null ? t.toString() : t.getMessage()
            );
        }
    }

    private Deployer getDeployer( DeploymentSpec spec ) {
        final MediaTypeMatcher matcher = MediaTypeMatcher.matcher(
            deployerMap.keySet(),
            spec.getMediaType()
        );
        if ( !matcher.matches() ) {
            throw new MediaTypeNotSupportedException( spec.getMediaType() );
        }
        final Deployer deployer = deployerMap.get( matcher.mediaType() );
        log.debugf( "media-type: '%s'", spec.getMediaType() );
        log.debug( deployer );
        return deployer;
    }

}
