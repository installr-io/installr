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
package io.installr.installrd.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.LocationType;
import io.installr.lib.deployer.DeployerConfig;
import io.installr.lib.deployer.DeployerPlugin;
import io.installr.lib.installrd.PackageContentFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

/**
 * Currently, this is a proof of concept...
 */
@ApplicationScoped
@JBossLog
public class PluginLoader {

    @Inject
    InstallrdConfiguration config;

    private Map<String, PackageContentFilter> packageContentFilters;
    private Map<String, DeployerPlugin<DeployerConfig>> deployers;

    public Optional<Map<String, PackageContentFilter>> getPackageContentFilters() {
        return Optional.ofNullable( packageContentFilters );
    }

    public Optional<Map<String, DeployerPlugin<DeployerConfig>>> getDeployers() {
        return Optional.ofNullable( deployers );
    }

    @SuppressWarnings( "unchecked" )
    public void load() {
        final Path pluginDir = config.locationFor( LocationType.Plugin );
        if ( !Files.exists( pluginDir ) ) {
            log.debugf( "Plugin directory not present: %s", pluginDir );
            return;
        }
        log.info( "Plugins:" );
        log.infof( "Loading plugins from '%s'...", pluginDir );
        try {
            final URL[] pluginUrls = Files.walk( pluginDir )
                .filter( Files::isRegularFile )
                .filter( this::isJar )
                .map( this::toUrl )
                .toArray( URL[]::new );
            log.infof( "Found %d plugin(s).", pluginUrls.length );
            final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            try ( URLClassLoader classLoader = new URLClassLoader( "plugin-loader", pluginUrls, ccl ) ) {
                packageContentFilters = loadServices( PackageContentFilter.class, classLoader );
                deployers = (Map<String, DeployerPlugin<DeployerConfig>>)(Map<?, ?>)loadServices(
                    DeployerPlugin.class,
                    classLoader
                );
            }
        }
        catch ( IOException ex ) {
            throw new IllegalStateException( ex );
        }
    }

    private boolean isJar( Path file ) {
        return file.toString().endsWith( ".jar" );
    }

    @SneakyThrows
    private URL toUrl( Path file ) {
        return file.toUri().toURL();
    }

    private <S> Map<String, S> loadServices( Class<S> clazz, ClassLoader classLoader ) {
        final Map<String, S> services = ServiceLoader.load( clazz, classLoader )
            .stream()
            .map( Provider::get )
            .collect( Collectors.toMap( k -> k.getClass().getName(), v -> v ) );
        log.infof(
            "Found %d service(s) of type '%s'.",
            services.size(),
            clazz.getSimpleName()
        );
        return services;
    }

}
