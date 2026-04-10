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
package io.installr.installrd.pkg.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithParentName;
import io.smallrye.config.source.yaml.YamlConfigSource;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class PackageConfigReader {

    public Map<String, String> readAll( Path namespaceLocation, Map<String, String> env, PackageId packageId ) {
        Map<String, String> all = new HashMap<>();
        all.putAll( readDefaults( namespaceLocation, env, packageId ) );
        all.putAll( readCustom( namespaceLocation, env, packageId ) );
        return all;
    }

    public Map<String, String> readDefaults( Path namespaceLocation, Map<String, String> env, PackageId packageId ) {
        return readFile( namespaceLocation, env, packageId.name(), Package.PACKAGE_DEFAULTS_YAML );
    }

    public Map<String, String> readCustom( Path namespaceLocation, Map<String, String> env, PackageId packageId ) {
        return readFile( namespaceLocation, env, packageId.name(), "custom.yaml" );
    }

    @SneakyThrows
    private Map<String, String> readFile(
        Path namespaceLocation, Map<String, String> env, String packagename, String filename
    ) {
        final Path file = namespaceLocation.resolve( Path.of( packagename, filename ) );
        if ( !Files.exists( file ) ) {
            log.debugf( "readFile: not found: %s", file );
            return Map.of();
        }
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
            .addDefaultInterceptors()
            .withSources( new YamlConfigSource( file.toUri().toURL() ) )
            .withValidateUnknown( false )
            .withMapping( PackageConfig.class )
            .withDefaultValues( env )
            .build();
        return config.getConfigMapping( PackageConfig.class )
            .map()
            .entrySet()
            .stream()
            .collect( Collectors.toMap( this::transformKey, this::transformValue ) );
    }

    private String transformKey( Map.Entry<String, PackageConfig.OptionalString> keySet ) {
        return keySet.getKey().replace( "-", "_" );
    }

    private String transformValue( Map.Entry<String, PackageConfig.OptionalString> keySet ) {
        final Optional<String> value = keySet.getValue().value();
        if ( value.isEmpty() ) {
            return "";
        }
        return value.get();
    }

    @ConfigMapping
    interface PackageConfig {

        @WithParentName
        // 'Map<String, Optional<String>> map();'' doesn't work, so we use an intermediate type...
        Map<String, OptionalString> map();

        interface OptionalString {
            @WithParentName
            Optional<String> value();
        }

    }

}
