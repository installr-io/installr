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
package io.installr.installrd.security;

import java.security.Permission;
import java.util.Objects;

/**
 * Permission to execute a namespace scoped {@link Operation} within a namespace.
 * <p>
 * Instantiated by Quarkus for {@code @PermissionsAllowed( value = ..., permission = NamespacePermission.class, params =
 * "namespace" )}.
 */
public class NamespacePermission extends Permission {

    private static final long serialVersionUID = 1L;

    private final Operation operation;
    private final String namespace;

    public NamespacePermission( String name, String namespace ) {
        super( name );
        this.operation = Operation.valueOf( name );
        this.namespace = namespace;
    }

    public Operation operation() {
        return operation;
    }

    public String namespace() {
        return namespace;
    }

    @Override
    public boolean implies( Permission permission ) {
        return equals( permission );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof NamespacePermission other ) ) {
            return false;
        }
        return operation == other.operation && Objects.equals( namespace, other.namespace );
    }

    @Override
    public int hashCode() {
        return Objects.hash( operation, namespace );
    }

    @Override
    public String getActions() {
        return "";
    }

}
