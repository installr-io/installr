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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
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
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class PluginLoader {

    @Inject
    InstallrdConfiguration config;

    private Map<String, PackageContentFilter> packageContentFilters;
    private Map<String, DeployerPlugin<DeployerConfig>> deployers;

    private URLClassLoader pcl;

    @PreDestroy
    @SneakyThrows
    void destroy() {
        if ( pcl != null ) {
            pcl.close();
        }
    }

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
            final ClassLoader qcl = this.getClass().getClassLoader(); // this is the quarkus class loader
            pcl = new URLClassLoader( "plugin-loader", pluginUrls, qcl ); // plugin class loader
            packageContentFilters = loadServices( PackageContentFilter.class, pcl );
            deployers = (Map<String, DeployerPlugin<DeployerConfig>>)(Map<?, ?>)loadServices(
                DeployerPlugin.class,
                pcl
            );
            loadDatabaseDrivers( pcl );
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

    private void loadDatabaseDrivers( final URLClassLoader pcl ) {
        List<String> registeredDrivers = Collections.list( DriverManager.getDrivers() )
            .stream()
            .map( Driver::getClass )
            .map( Class::getName )
            .toList();
        loadServices( java.sql.Driver.class, pcl )
            .forEach(
                ( k, v ) -> {
                    try {
                        if ( registeredDrivers.contains( k ) ) {
                            log.tracef( "- Driver already register: %s", k );
                            return;
                        }
                        registerDriver( pcl, k );
                    }
                    catch ( SQLException ex ) {
                        throw new IllegalStateException( ex );
                    }
                }
            );
    }

    @SneakyThrows
    private void registerDriver( final URLClassLoader pcl, String driverName )
        throws SQLException {
        // Since we use a different class loader we cannot use DriverManager.registerDriver with the driver loaded by
        // the ServiceLoader. Instead we use instantiate the driver "per hand" a register it with shim.
        final Class<?> driverClass = Class.forName( driverName, true, pcl );
        final Driver driver = (Driver)driverClass.getDeclaredConstructor().newInstance();
        DriverManager.registerDriver( new DriverShim( driver ) );
        log.infof( "- Driver registered: %s", driverName );
    }

}
