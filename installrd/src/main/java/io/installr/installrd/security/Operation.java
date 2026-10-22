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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Catalog of all operations that can be granted to a role.
 * <p>
 * Roles reference operations by their {@link #id()} (e.g. {@code pkg:install}); wildcards like {@code pkg:*} or
 * {@code *} are resolved via {@link #resolve(String)}. Secured endpoints reference operations by their enum name (see
 * {@link Names}), because {@code @PermissionsAllowed} treats a colon as separator between name and actions.
 */
public enum Operation {

    NS_READ( "ns:read", Scope.Namespace ),
    NS_CREATE( "ns:create", Scope.Global ),
    NS_DELETE( "ns:delete", Scope.Global ),
    PKG_INSTALL( "pkg:install", Scope.Namespace ),
    PKG_REINSTALL( "pkg:reinstall", Scope.Namespace ),
    PKG_INSTALL_FILE( "pkg:install-file", Scope.Namespace ),
    PKG_REINSTALL_FILE( "pkg:reinstall-file", Scope.Namespace ),
    PKG_READ( "pkg:read", Scope.Namespace ),
    TX_READ( "tx:read", Scope.Namespace ),
    TX_CANCEL( "tx:cancel", Scope.Namespace ),
    DAEMON_STOP( "daemon:stop", Scope.Global );

    public enum Scope {
        /*
         * Operation affects the whole daemon/host and can only be granted by global bindings.
         */
        Global,
        /*
         * Operation is executed within a namespace and can be granted per namespace.
         */
        Namespace
    }

    /**
     * Compile-time constants of the enum names, usable in annotations.
     */
    public static final class Names {
        public static final String NS_READ = "NS_READ";
        public static final String NS_CREATE = "NS_CREATE";
        public static final String NS_DELETE = "NS_DELETE";
        public static final String PKG_INSTALL = "PKG_INSTALL";
        public static final String PKG_REINSTALL = "PKG_REINSTALL";
        public static final String PKG_INSTALL_FILE = "PKG_INSTALL_FILE";
        public static final String PKG_REINSTALL_FILE = "PKG_REINSTALL_FILE";
        public static final String PKG_READ = "PKG_READ";
        public static final String TX_READ = "TX_READ";
        public static final String TX_CANCEL = "TX_CANCEL";
        public static final String DAEMON_STOP = "DAEMON_STOP";

        private Names() {
        }
    }

    private static final String WILDCARD = "*";

    private final String id;
    private final Scope scope;

    Operation( String id, Scope scope ) {
        this.id = id;
        this.scope = scope;
    }

    public String id() {
        return id;
    }

    public Scope scope() {
        return scope;
    }

    /**
     * Resolves an operation pattern as used in role definitions.
     *
     * @param  pattern                  an operation id ({@code pkg:install}), a resource wildcard ({@code pkg:*}) or
     *                                  {@code *}
     * @return                          the matching operations, never empty
     * @throws IllegalArgumentException if the pattern does not match any operation
     */
    public static Set<Operation> resolve( String pattern ) {
        final String p = pattern == null ? "" : pattern.trim();
        final Set<Operation> result = EnumSet.noneOf( Operation.class );
        if ( WILDCARD.equals( p ) ) {
            result.addAll( EnumSet.allOf( Operation.class ) );
        }
        else if ( p.endsWith( ":" + WILDCARD ) ) {
            final String prefix = p.substring( 0, p.length() - WILDCARD.length() );
            Arrays.stream( values() ).filter( op -> op.id.startsWith( prefix ) ).forEach( result::add );
        }
        else {
            Arrays.stream( values() ).filter( op -> op.id.equals( p ) ).forEach( result::add );
        }
        if ( result.isEmpty() ) {
            throw new IllegalArgumentException( String.format( "Unknown operation '%s'.", pattern ) );
        }
        return result;
    }

}
