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

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import io.installr.installrd.util.YamlConfigLoader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class NamespaceConfigurationLoader {

    @Inject
    YamlConfigLoader yamlConfigLoader;

    @SneakyThrows
    public Map<String, NamespaceConfiguration> load( Path namespaceLocation ) {
        if ( !Files.exists( namespaceLocation ) ) {
            return Map.of();
        }
        final Map<String, NamespaceConfiguration> configs = new HashMap<>();
        Files.walk( namespaceLocation, 1, FileVisitOption.FOLLOW_LINKS )
            .filter( Files::isDirectory )
            .filter( path -> !path.equals( namespaceLocation ) )
            .forEach(
                nsDir -> {
                    final String namespaceName = nsDir.getFileName().toString();
                    try {
                        configs.put(
                            namespaceName,
                            yamlConfigLoader
                                .load( nsDir.resolve( "config.yaml" ).toAbsolutePath(), NamespaceConfiguration.class )
                        );
                    }
                    catch ( Exception ex ) {
                        log.errorf( "Could not load config for namespace '%s': %s", namespaceName, ex.getMessage() );
                        log.debug( ex );
                    }
                }
            );
        return configs;
    }

}
