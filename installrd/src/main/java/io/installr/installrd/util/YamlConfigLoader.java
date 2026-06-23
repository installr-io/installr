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
package io.installr.installrd.util;

import java.nio.file.Path;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
public class YamlConfigLoader {

    @SneakyThrows
    public <T> T load( Path file, Class<T> type ) {
        log.debugf( "Loading %s from: %s", type.getSimpleName(), file );
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

}
