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

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.source.yaml.YamlConfigSource;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
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

    @ConfigProperty( name = "installrd.config.location" )
    Path configLocation;

    private InstallrdConfigurationMapping installrdConfig;

    @PostConstruct
    @SneakyThrows
    void init() {
        final Path location = configLocation.toAbsolutePath();
        Files.createDirectories( location );
        final Path configFile = location.resolve( "config.yaml" );
        if ( !Files.exists( configFile ) ) {
            Files.createFile( configFile );
        }
        installrdConfig = readFile( configFile, InstallrdConfigurationMapping.class );
    }

    @SneakyThrows
    private <T> T readFile( Path file, Class<T> type ) {
        return type.cast(
            new SmallRyeConfigBuilder()
                .addDiscoveredConverters()
                .addDiscoveredCustomizers()
                .addDiscoveredInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .addDiscoveredSources()
                .addDiscoveredValidator()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(
                    new YamlConfigSource(
                        UriBuilder.fromPath( "/{configFile}" )
                            .scheme( "file" )
                            .build( file )
                            .toURL()
                    )
                )
                .withValidateUnknown( false )
                .withMapping( type )
                .build()
                .getConfigMapping( type )
        );
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
                .map( x -> readFile( x, RepositoryConfiguration.class ) )
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

}
