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
import java.util.stream.Collectors;

import io.installr.installrd.util.MapUtil;
import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
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
        all.putAll( env );
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
        final YamlConfigSource yamlSource = new YamlConfigSource( file.toUri().toURL() );
        final SmallRyeConfig config = new SmallRyeConfigBuilder()
            .addDefaultInterceptors()
            .withSources( yamlSource )
            .withValidateUnknown( false )
            .withDefaultValues( env )
            .build();
        return MapUtil.flatten( resolve( yamlSource, config ) );
    }

    private Map<String, String> resolve( YamlConfigSource yamlSource, SmallRyeConfig config ) {
        return yamlSource.getProperties().keySet().stream()
            .collect(
                Collectors.toMap(
                    key -> key,
                    key -> {
                        return config.getOptionalValue( key, String.class ).orElse( "" );
                    }
                )
            );
    }

}
