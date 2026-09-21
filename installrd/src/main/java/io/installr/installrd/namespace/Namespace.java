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

import java.nio.file.Path;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.installrd.config.NamespaceConfiguration;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class Namespace {

    /*
     * The name of the namespace.
     */
    private String name;

    /*
     * The location of the namespace to read the configuration, store data, etc.
     */
    private Path location;

    @Inject
    InstallrdConfiguration installrdConfig;

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

    public NamespaceConfiguration config() {
        return installrdConfig.namespaceConfiguration( name )
            .orElseThrow( () -> new NamespaceNotFoundException( name ) );
    }

    @Override
    public String toString() {
        return name;
    }

}
