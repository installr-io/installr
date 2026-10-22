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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import io.installr.installrd.config.NamespaceConfiguration;
import io.installr.installrd.config.SecurityConfiguration;
import io.installr.installrd.config.SecurityConfiguration.RoleBinding;
import io.installr.installrd.config.SecurityConfiguration.RoleDefinition;
import lombok.extern.jbosslog.JBossLog;

/**
 * Immutable, validated view of the configured roles and role bindings.
 * <p>
 * Subjects are compared as {@link X500Principal}, i.e. in canonical form (case and whitespace insensitive).
 */
@JBossLog
public final class AccessPolicy {

    public static final String ADMIN_ROLE = "admin";

    private static final AccessPolicy EMPTY = new AccessPolicy( Set.of(), Map.of(), Map.of() );

    private final Set<X500Principal> admins;
    private final Map<X500Principal, Set<Operation>> globalGrants;
    private final Map<String, Map<X500Principal, Set<Operation>>> namespaceGrants;

    private AccessPolicy(
        Set<X500Principal> admins,
        Map<X500Principal, Set<Operation>> globalGrants,
        Map<String, Map<X500Principal, Set<Operation>>> namespaceGrants
    ) {
        this.admins = admins;
        this.globalGrants = globalGrants;
        this.namespaceGrants = namespaceGrants;
    }

    /**
     * A policy which denies everything.
     */
    public static AccessPolicy empty() {
        return EMPTY;
    }

    /**
     * Builds and validates the policy.
     *
     * @throws InvalidSecurityConfigurationException if the configuration is invalid
     */
    public static AccessPolicy of(
        Optional<SecurityConfiguration> security, Map<String, NamespaceConfiguration> namespaceConfigs
    ) {
        final Map<String, Set<Operation>> roles = new HashMap<>();
        security.flatMap( SecurityConfiguration::roles )
            .orElse( List.of() )
            .forEach( role -> roles.put( validateRoleName( role, roles ), resolveOperations( role ) ) );

        final Set<X500Principal> admins = new HashSet<>();
        security.flatMap( SecurityConfiguration::admins )
            .orElse( List.of() )
            .forEach( subject -> admins.add( parseSubject( subject, "security.admins" ) ) );

        final Map<X500Principal, Set<Operation>> globalGrants = new HashMap<>();
        security.flatMap( SecurityConfiguration::bindings )
            .orElse( List.of() )
            .forEach( binding -> grant( globalGrants, binding, roles, "security.bindings", null ) );

        final Map<String, Map<X500Principal, Set<Operation>>> namespaceGrants = new HashMap<>();
        namespaceConfigs.forEach(
            ( namespace, config ) -> {
                final Map<X500Principal, Set<Operation>> grants = new HashMap<>();
                config.access()
                    .orElse( List.of() )
                    .forEach( binding -> grant( grants, binding, roles, "namespace '" + namespace + "'", namespace ) );
                namespaceGrants.put( namespace, freeze( grants ) );
            }
        );

        return new AccessPolicy(
            Collections.unmodifiableSet( admins ),
            freeze( globalGrants ),
            Collections.unmodifiableMap( namespaceGrants )
        );
    }

    private static String validateRoleName( RoleDefinition role, Map<String, Set<Operation>> roles ) {
        final String name = role.name() == null ? "" : role.name().trim();
        if ( name.isEmpty() ) {
            throw new InvalidSecurityConfigurationException( "role without name" );
        }
        if ( ADMIN_ROLE.equalsIgnoreCase( name ) ) {
            throw new InvalidSecurityConfigurationException(
                "role name '%s' is reserved; use 'security.admins' instead",
                name
            );
        }
        if ( roles.containsKey( name ) ) {
            throw new InvalidSecurityConfigurationException( "role '%s' is defined more than once", name );
        }
        return name;
    }

    private static Set<Operation> resolveOperations( RoleDefinition role ) {
        final Set<Operation> operations = EnumSet.noneOf( Operation.class );
        for ( final String pattern : role.operations() ) {
            try {
                operations.addAll( Operation.resolve( pattern ) );
            }
            catch ( IllegalArgumentException ex ) {
                throw new InvalidSecurityConfigurationException( "role '%s': %s", role.name(), ex.getMessage() );
            }
        }
        return Collections.unmodifiableSet( operations );
    }

    private static X500Principal parseSubject( String subject, String context ) {
        try {
            return new X500Principal( subject );
        }
        catch ( IllegalArgumentException | NullPointerException ex ) {
            throw new InvalidSecurityConfigurationException( "%s: invalid subject DN '%s'", context, subject );
        }
    }

    /*
     * Adds the operations of the bound roles to the grants. If a namespace is given, global operations are skipped.
     */
    private static void grant(
        Map<X500Principal, Set<Operation>> grants,
        RoleBinding binding,
        Map<String, Set<Operation>> roles,
        String context,
        String namespace
    ) {
        final X500Principal subject = parseSubject( binding.subject(), context );
        final Set<Operation> operations = grants.computeIfAbsent( subject, k -> EnumSet.noneOf( Operation.class ) );
        for ( final String roleName : binding.roles() ) {
            final Set<Operation> roleOperations = roles.get( roleName );
            if ( roleOperations == null ) {
                throw new InvalidSecurityConfigurationException(
                    "%s: subject '%s' is bound to unknown role '%s'",
                    context,
                    binding.subject(),
                    roleName
                );
            }
            for ( final Operation operation : roleOperations ) {
                if ( namespace != null && operation.scope() == Operation.Scope.Global ) {
                    log.warnf(
                        "%s: global operation '%s' of role '%s' is ignored; grant it via 'security.bindings'.",
                        context,
                        operation.id(),
                        roleName
                    );
                    continue;
                }
                operations.add( operation );
            }
        }
    }

    private static Map<X500Principal, Set<Operation>> freeze( Map<X500Principal, Set<Operation>> grants ) {
        final Map<X500Principal, Set<Operation>> result = new HashMap<>();
        grants.forEach( ( k, v ) -> result.put( k, Collections.unmodifiableSet( v ) ) );
        return Collections.unmodifiableMap( result );
    }

    public boolean hasAdmins() {
        return !admins.isEmpty();
    }

    public boolean isAdmin( X500Principal subject ) {
        return admins.contains( subject );
    }

    /**
     * Checks whether the subject may execute the operation.
     *
     * @param namespace the namespace the operation is executed in; {@code null} for operations outside a namespace
     */
    public boolean isAllowed( X500Principal subject, Operation operation, String namespace ) {
        if ( isAdmin( subject ) ) {
            return true;
        }
        if ( globalGrants.getOrDefault( subject, Set.of() ).contains( operation ) ) {
            return true;
        }
        if ( namespace == null || operation.scope() != Operation.Scope.Namespace ) {
            return false;
        }
        return namespaceGrants.getOrDefault( namespace, Map.of() )
            .getOrDefault( subject, Set.of() )
            .contains( operation );
    }

}
