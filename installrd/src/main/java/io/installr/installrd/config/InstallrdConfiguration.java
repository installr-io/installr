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
package io.installr.installrd.config;

import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.installr.installrd.logging.ConsoleLog;
import io.installr.installrd.util.YamlConfigLoader;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.SneakyThrows;

@ApplicationScoped
public class InstallrdConfiguration implements InstallrdConfigurationMapping {

    public enum LocationType {
        Namespace,
        Plugin,
        Repository
    }

    public interface DeployerConfiguration {
        @WithName( "type" )
        String type();

        @WithName( "config" )
        Map<String, String> config();
    }

    public interface PackageCacheConfiguration {
        @WithName( "expires-after" )
        @WithDefault( "1h" )
        Duration expiresAfter();
    }

    public interface RepositoryConfiguration {
        String id();

        String name();

        URL url();

        @WithDefault( "true" )
        boolean enabled();

        @WithName( "package-cache" )
        Optional<PackageCacheConfiguration> packageCache();

        Optional<Authentication> auth();

        interface Authentication {
            String username();

            String password();
        }
    }

    public interface NamespaceDefinition {
        String name();
    }

    @Inject
    YamlConfigLoader yamlConfigLoader;

    @Inject
    NamespaceConfigurationLoader namespaceConfigurationLoader;

    @ConfigProperty( name = "installrd.config.location" )
    Path configLocation;

    @Inject
    ConsoleLog consoleLog;

    private volatile InstallrdConfigurationMapping installrdConfig;

    private volatile Map<String, NamespaceConfiguration> namespaceConfigs;

    @PostConstruct
    void init() {
        reload();
    }

    @SneakyThrows
    void reload() {
        final Path location = configLocation.toAbsolutePath();
        Files.createDirectories( location );
        final Path configFile = location.resolve( "config.yaml" );
        if ( !Files.exists( configFile ) ) {
            Files.createFile( configFile );
        }
        installrdConfig = yamlConfigLoader.load( configFile, InstallrdConfigurationMapping.class );
        namespaceConfigs = namespaceConfigurationLoader.load( locationFor( LocationType.Namespace ) );
    }

    void onConfigReloaded( @Observes ConfigReloadedEvent event ) {
        consoleLog.info( "Reloading configuration..." );
        reload();
        consoleLog.info( "Configuration reloaded." );
    }

    public Path location() {
        return configLocation;
    }

    public Path locationFor( LocationType type ) {
        return switch ( type ) {
            case Namespace  -> location().resolve( "namespaces" );
            case Plugin     -> location().resolve( "plugins" );
            case Repository -> location().resolve( "repos" );
        };
    }

    @Override
    public Map<String, DeployerConfiguration> deployerConfigurations() {
        return installrdConfig.deployerConfigurations();
    }

    @Override
    public PackageCacheConfiguration packageCache() {
        return installrdConfig.packageCache();
    }

    @Override
    @SneakyThrows
    public Optional<List<RepositoryConfiguration>> repositories() {
        final Optional<List<RepositoryConfiguration>> repositories = installrdConfig.repositories();
        final Path repositoryLocation = locationFor( LocationType.Repository );
        if ( !Files.exists( repositoryLocation ) ) {
            return repositories;
        }
        final List<RepositoryConfiguration> effectiveRepositoryList = new ArrayList<>();
        if ( repositories.isPresent() ) {
            effectiveRepositoryList.addAll( repositories.get() );
        }
        effectiveRepositoryList.addAll(
            Files.walk( repositoryLocation, 1, FileVisitOption.FOLLOW_LINKS )
                .filter( Files::isRegularFile )
                .sorted()
                .map( x -> yamlConfigLoader.load( x, RepositoryConfiguration.class ) )
                .toList()
        );
        return Optional.of( effectiveRepositoryList );
    }

    @Override
    public Map<String, String> selector() {
        return installrdConfig.selector();
    }

    @SneakyThrows
    public List<NamespaceDefinition> namespaceDefinitions() {
        final Path namespaceLocation = locationFor( LocationType.Namespace );
        if ( !Files.exists( namespaceLocation ) ) {
            return List.of();
        }
        return Files.walk( namespaceLocation, 1, FileVisitOption.FOLLOW_LINKS )
            .filter( Files::isDirectory )
            .filter( x -> !x.equals( namespaceLocation ) )
            .map( Path::getFileName )
            .map( Path::toString )
            .sorted()
            .map( x -> (NamespaceDefinition)() -> x )
            .toList();
    }

    public Map<String, NamespaceConfiguration> namespaceConfigurations() {
        return namespaceConfigs;
    }

    public Optional<NamespaceConfiguration> namespaceConfiguration( String name ) {
        return Optional.ofNullable( namespaceConfigs.get( name ) );
    }

}
