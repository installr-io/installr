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
package io.installr.installrd.namespace;

import java.io.IOException;
import java.nio.file.Path;

import io.installr.installrd.config.NamespaceConfiguration;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.jbosslog.JBossLog;

@RequestScoped
@JBossLog
public class Namespace {

    /*
     * The name of the namespace.
     */
    private String name;

    /*
     * The location of the namespace to read the configuration, store data, etc.
     */
    private Path location;

    /*
     * To prevent loading the namespace configuration more than one time per request, we store the config temporarily.
     */
    private NamespaceConfiguration namespaceConfig;

    void init( String name, Path location ) {
        this.name = name;
        this.location = location;
    }

    public String name() {
        return name;
    }

    public Path location() {
        return location;
    }

    public NamespaceConfiguration config() throws NamespaceNotFoundException {
        if ( namespaceConfig == null ) {
            try {
                log.debugf( "loading config from: %s", location );
                final SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultInterceptors()
                    .withSources(
                        new YamlConfigSource(
                            UriBuilder.fromPath( "/{location}/config.yaml" )
                                .scheme( "file" )
                                .build( location, name )
                                .toURL()
                        )
                    )
                    .withValidateUnknown( false ) // this ignores not mapped values! should be needed...
                    .withMapping( NamespaceConfiguration.class )
                    .build();
                namespaceConfig = config.getConfigMapping( NamespaceConfiguration.class );
            }
            catch ( IOException ex ) {
                throw new NamespaceNotFoundException( name );
            }
        }
        return namespaceConfig;
    }

    @Override
    public String toString() {
        return name;
    }

}
