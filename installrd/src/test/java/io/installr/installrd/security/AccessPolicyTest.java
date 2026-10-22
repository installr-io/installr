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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.Test;

import io.installr.installrd.config.InstallrdConfiguration.DeployerConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.RepositoryConfiguration;
import io.installr.installrd.config.NamespaceConfiguration;
import io.installr.installrd.config.SecurityConfiguration;
import io.installr.installrd.config.SecurityConfiguration.RoleBinding;
import io.installr.installrd.config.SecurityConfiguration.RoleDefinition;

class AccessPolicyTest {

    static final X500Principal ADMIN = new X500Principal( "CN=admin,O=test" );
    static final X500Principal ALICE = new X500Principal( "CN=alice,O=test" );
    static final X500Principal BOB = new X500Principal( "CN=bob,O=test" );

    // --- admin ---

    @Test
    void admin_should_be_allowed_everything() {
        final AccessPolicy policy = AccessPolicy.of(
            security( List.of( "CN=admin,O=test" ), List.of(), List.of() ),
            Map.of()
        );

        for ( final Operation operation : Operation.values() ) {
            assertTrue( policy.isAllowed( ADMIN, operation, "any" ) );
            assertTrue( policy.isAllowed( ADMIN, operation, null ) );
        }
        assertTrue( policy.hasAdmins() );
    }

    @Test
    void admin_role_name_should_be_reserved() {
        assertThrows(
            InvalidSecurityConfigurationException.class,
            () -> AccessPolicy.of( security( List.of(), List.of( role( "Admin", "*" ) ), List.of() ), Map.of() )
        );
    }

    // --- validation ---

    @Test
    void unknown_operation_should_be_rejected() {
        assertThrows(
            InvalidSecurityConfigurationException.class,
            () -> AccessPolicy.of( security( List.of(), List.of( role( "dev", "pkg:unknown" ) ), List.of() ), Map.of() )
        );
    }

    @Test
    void duplicate_role_should_be_rejected() {
        assertThrows(
            InvalidSecurityConfigurationException.class,
            () -> AccessPolicy.of(
                security( List.of(), List.of( role( "dev", "pkg:read" ), role( "dev", "tx:read" ) ), List.of() ),
                Map.of()
            )
        );
    }

    @Test
    void binding_to_unknown_role_should_be_rejected() {
        assertThrows(
            InvalidSecurityConfigurationException.class,
            () -> AccessPolicy.of(
                security( List.of(), List.of(), List.of( binding( "CN=alice,O=test", "dev" ) ) ),
                Map.of()
            )
        );
        assertThrows(
            InvalidSecurityConfigurationException.class,
            () -> AccessPolicy.of(
                security( List.of(), List.of(), List.of() ),
                Map.of( "ns1", namespace( binding( "CN=alice,O=test", "dev" ) ) )
            )
        );
    }

    @Test
    void invalid_subject_should_be_rejected() {
        assertThrows(
            InvalidSecurityConfigurationException.class,
            () -> AccessPolicy.of( security( List.of( "not a dn" ), List.of(), List.of() ), Map.of() )
        );
    }

    // --- namespace bindings ---

    @Test
    void namespace_binding_should_only_apply_to_its_namespace() {
        final AccessPolicy policy = AccessPolicy.of(
            security( List.of(), List.of( role( "dev", "pkg:install", "tx:*" ) ), List.of() ),
            Map.of( "ns1", namespace( binding( "CN=alice,O=test", "dev" ) ), "ns2", namespace() )
        );

        assertTrue( policy.isAllowed( ALICE, Operation.PKG_INSTALL, "ns1" ) );
        assertTrue( policy.isAllowed( ALICE, Operation.TX_CANCEL, "ns1" ) );
        assertFalse( policy.isAllowed( ALICE, Operation.PKG_READ, "ns1" ) );
        assertFalse( policy.isAllowed( ALICE, Operation.PKG_INSTALL, "ns2" ) );
        assertFalse( policy.isAllowed( ALICE, Operation.PKG_INSTALL, null ) );
        assertFalse( policy.isAllowed( BOB, Operation.PKG_INSTALL, "ns1" ) );
    }

    @Test
    void namespace_binding_should_ignore_global_operations() {
        final AccessPolicy policy = AccessPolicy.of(
            security( List.of(), List.of( role( "all", "*" ) ), List.of() ),
            Map.of( "ns1", namespace( binding( "CN=alice,O=test", "all" ) ) )
        );

        assertTrue( policy.isAllowed( ALICE, Operation.PKG_INSTALL, "ns1" ) );
        assertTrue( policy.isAllowed( ALICE, Operation.PKG_INSTALL_FILE, "ns1" ) );
        assertFalse( policy.isAllowed( ALICE, Operation.NS_CREATE, null ) );
        assertFalse( policy.isAllowed( ALICE, Operation.NS_DELETE, "ns1" ) );
        assertFalse( policy.isAllowed( ALICE, Operation.DAEMON_STOP, null ) );
    }

    // --- global bindings ---

    @Test
    void global_binding_should_apply_to_all_namespaces_and_global_operations() {
        final AccessPolicy policy = AccessPolicy.of(
            security(
                List.of(),
                List.of( role( "ops", "ns:create", "ns:delete", "ns:read" ) ),
                List.of( binding( "CN=alice,O=test", "ops" ) )
            ),
            Map.of()
        );

        assertTrue( policy.isAllowed( ALICE, Operation.NS_CREATE, null ) );
        assertTrue( policy.isAllowed( ALICE, Operation.NS_READ, "any" ) );
        assertFalse( policy.isAllowed( ALICE, Operation.DAEMON_STOP, null ) );
        assertFalse( policy.hasAdmins() );
    }

    // --- subjects ---

    @Test
    void subjects_should_be_compared_in_canonical_form() {
        final AccessPolicy policy = AccessPolicy.of(
            security( List.of( "cn=Admin, o=Test" ), List.of(), List.of() ),
            Map.of()
        );

        assertTrue( policy.isAdmin( ADMIN ) );
        assertFalse( policy.isAdmin( ALICE ) );
    }

    @Test
    void empty_policy_should_deny_everything() {
        assertFalse( AccessPolicy.empty().isAllowed( ADMIN, Operation.NS_READ, "ns1" ) );
        assertFalse( AccessPolicy.of( Optional.empty(), Map.of() ).isAllowed( ADMIN, Operation.NS_READ, "ns1" ) );
    }

    // --- helpers ---

    static Optional<SecurityConfiguration> security(
        List<String> admins, List<RoleDefinition> roles, List<RoleBinding> bindings
    ) {
        return Optional.of(
            new SecurityConfiguration() {
                @Override
                public Optional<List<String>> admins() {
                    return Optional.of( admins );
                }

                @Override
                public Optional<List<RoleDefinition>> roles() {
                    return Optional.of( roles );
                }

                @Override
                public Optional<List<RoleBinding>> bindings() {
                    return Optional.of( bindings );
                }
            }
        );
    }

    static RoleDefinition role( String name, String ... operations ) {
        return new RoleDefinition() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public List<String> operations() {
                return Arrays.asList( operations );
            }
        };
    }

    static RoleBinding binding( String subject, String ... roles ) {
        return new RoleBinding() {
            @Override
            public String subject() {
                return subject;
            }

            @Override
            public List<String> roles() {
                return Arrays.asList( roles );
            }
        };
    }

    static NamespaceConfiguration namespace( RoleBinding ... bindings ) {
        return new NamespaceConfiguration() {
            @Override
            public Optional<List<RepositoryConfiguration>> repositories() {
                return Optional.empty();
            }

            @Override
            public Map<String, DeployerConfiguration> deployerConfigurations() {
                return Map.of();
            }

            @Override
            public Map<String, Optional<String>> rawEnv() {
                return Map.of();
            }

            @Override
            public Optional<List<RoleBinding>> access() {
                return Optional.of( Arrays.asList( bindings ) );
            }
        };
    }

}
