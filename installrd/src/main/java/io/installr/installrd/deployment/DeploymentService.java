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
import io.installr.installrd.deployment.deployer.BuiltInDeployerLiteral;
import io.installr.installrd.plugin.PluginLoader;
import io.installr.installrd.util.MapUtil;
import io.installr.installrd.util.PropertyMapUtil;
import io.installr.lib.deployer.ConfigurableDeployer;
import io.installr.lib.deployer.DeployerConfig;
import io.installr.lib.deployer.DeployerPlugin;
import io.installr.lib.deployer.DeploymentException;
import io.installr.lib.deployer.DeploymentSpec;
import io.installr.installrd.config.ConfigReloadedEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class DeploymentService {

    @Inject
    InstallrdConfiguration config;

    @Inject
    PluginLoader pluginLoader;

    // Instance<Object> is intentional: CDI type resolution is invariant, so Instance<ConfigurableDeployer<X>>
    // would only match beans whose type parameter is exactly X. Since each built-in deployer uses its own
    // config subtype (e.g. ConfigurableDeployer<DockerDeployerConfig>), a typed injection point would make
    // most deployers invisible. Instance<Object> with @Any is the CDI-standard approach for programmatic
    // lookup of heterogeneous beans — filtering is done via the @BuiltInDeployer qualifier in initDeployer().
    @Inject
    @Any
    Instance<Object> allBeans;

    private volatile Map<String, ConfigurableDeployer<?>> globalDeployerMap;
    private volatile Map<String, Map<String, ConfigurableDeployer<?>>> namespaceDeployerMap;

    @PostConstruct
    void init() {
        buildDeployerMaps();
    }

    void buildDeployerMaps() {
        globalDeployerMap = config.deployerConfigurations()
            .entrySet()
            .stream()
            .collect( Collectors.toMap( Map.Entry::getKey, this::mapToDeployer ) );

        namespaceDeployerMap = config.namespaceConfigurations()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().deployerConfigurations()
                        .entrySet()
                        .stream()
                        .collect( Collectors.toMap( Map.Entry::getKey, this::mapToDeployer ) )
                )
            );
    }

    void onConfigReloaded( @Observes ConfigReloadedEvent event ) {
        log.info( "Rebuilding deployer maps after configuration reload..." );
        buildDeployerMaps();
    }

    private ConfigurableDeployer<?> mapToDeployer( Map.Entry<String, DeployerConfiguration> entry ) {
        final DeployerConfiguration config = entry.getValue();
        final Instance<Object> selected = allBeans.select( new BuiltInDeployerLiteral( config.type() ) );

        if ( !selected.isUnsatisfied() ) {
            return initDeployer( selected.get(), config.config() );
        }
        return findExternalDeployer( config.type(), entry.getKey(), config.config() );
    }

    @SuppressWarnings( "unchecked" )
    private <C extends DeployerConfig> ConfigurableDeployer<C> initDeployer(
        Object bean, Map<String, String> rawConfig
    ) {
        final ConfigurableDeployer<C> deployer = (ConfigurableDeployer<C>)bean;
        deployer.init( deserializeConfig( deployer, rawConfig ) );
        return deployer;
    }

    private <C extends DeployerConfig> C deserializeConfig(
        ConfigurableDeployer<C> deployer, Map<String, String> rawConfig
    ) {
        final Class<C> configType = deployer.getConfigType();
        if ( configType == null ) {
            return null;
        }
        return PropertyMapUtil.readPropertyMap( rawConfig, configType );
    }

    private DeployerPlugin<DeployerConfig> findExternalDeployer(
        String deployerName, String mediaType, Map<String, String> config
    ) {
        final Optional<Map<String, DeployerPlugin<DeployerConfig>>> plugins = pluginLoader.getDeployers();
        if ( !plugins.isPresent() ) {
            throw new IllegalStateException( String.format( "No deployer found for media-type '%s'.", mediaType ) );
        }
        final Map<String, DeployerPlugin<DeployerConfig>> deployers = plugins.get();
        if ( !deployers.containsKey( deployerName ) ) {
            throw new IllegalStateException( "Deployer not found: " + deployerName );
        }
        final DeployerPlugin<DeployerConfig> deployer = deployers.get( deployerName );
        deployer.init( deserializeConfig( deployer, config ) );
        return deployer;
    }

    public DeploymentSpec deploy( DeploymentSpec spec ) {
        return handleError( spec, deployer -> deployer.deploy( spec ) );
    }

    public DeploymentSpec undeploy( DeploymentSpec spec ) {
        return handleError( spec, deployer -> deployer.undeploy( spec ) );
    }

    private DeploymentSpec handleError( DeploymentSpec spec, Consumer<ConfigurableDeployer<?>> consumer ) {
        final ConfigurableDeployer<?> deployer = getDeployer( spec );
        extendEnv( spec, deployer );
        try {
            consumer.accept( deployer );
            return spec;
        }
        catch ( Throwable t ) {
            log.debugf( t, "STACKTRACE" );
            if ( t instanceof DeploymentWarning dw ) {
                throw dw;
            }
            final DeploymentException de = t instanceof DeploymentException existing
                ? existing
                : deployer.translateException( t );
            throw new DeploymentException( "[%s] %s", deployer.getClass().getSimpleName(), de.getMessage() );
        }
    }

    private ConfigurableDeployer<?> getDeployer( DeploymentSpec spec ) {
        ConfigurableDeployer<?> deployer = null;

        // namespace deployer have precedence over global deployers
        if ( spec.getEnv() != null && spec.getEnv().containsKey( "namespace" ) ) {
            deployer = findNamespaceDeployer( spec );
        }
        // global deployers
        if ( deployer == null ) {
            final MediaTypeMatcher matcher = MediaTypeMatcher.matcher(
                globalDeployerMap.keySet(),
                spec.getMediaType()
            );
            if ( !matcher.matches() ) {
                throw new MediaTypeNotSupportedException( spec.getMediaType() );
            }
            deployer = globalDeployerMap.get( matcher.mediaType() );
        }
        log.debugf( "media-type: '%s'", spec.getMediaType() );
        log.debug( deployer );
        return deployer;
    }

    private ConfigurableDeployer<?> findNamespaceDeployer( DeploymentSpec spec ) {
        final String namespace = spec.getEnv().get( "namespace" );
        final MediaTypeMatcher matcher = MediaTypeMatcher.matcher(
            namespaceDeployerMap.getOrDefault( namespace, Map.of() ).keySet(),
            spec.getMediaType()
        );
        if ( matcher.matches() ) {
            return namespaceDeployerMap.get( namespace ).get( matcher.mediaType() );
        }
        return null;
    }

    private void extendEnv( DeploymentSpec spec, final ConfigurableDeployer<?> deployer ) {
        if ( deployer.getConfig().isPresent() ) {
            final Optional<Map<String, String>> deployerEnv = deployer.getConfig().get().env();
            if ( deployerEnv.isPresent() ) {
                final Map<String, String> env = deployerEnv.get();
                env.putAll( spec.getEnv() );
                spec.setEnv( env );
                log.debug( "extended environment:" );
                MapUtil.dump( log, env );
            }
        }
    }

}
