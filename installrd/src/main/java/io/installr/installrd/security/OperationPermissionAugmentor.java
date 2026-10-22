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

import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Adds a permission checker to every authenticated identity, which delegates to the {@link AccessService}.
 * <ul>
 * <li>{@link NamespacePermission}: namespace scoped operations, e.g.
 * {@code @PermissionsAllowed( value = Operation.Names.PKG_INSTALL, permission = NamespacePermission.class, params =
 * "namespace" )}</li>
 * <li>{@link StringPermission}: operations outside a namespace, e.g.
 * {@code @PermissionsAllowed( Operation.Names.DAEMON_STOP )}</li>
 * </ul>
 * Since the policy is evaluated on every check, configuration changes apply immediately.
 */
@ApplicationScoped
public class OperationPermissionAugmentor implements SecurityIdentityAugmentor {

    @Inject
    AccessService accessService;

    @Override
    public Uni<SecurityIdentity> augment( SecurityIdentity identity, AuthenticationRequestContext context ) {
        if ( identity.isAnonymous() ) {
            return Uni.createFrom().item( identity );
        }
        return Uni.createFrom()
            .item(
                QuarkusSecurityIdentity.builder( identity )
                    .addPermissionChecker( permission -> Uni.createFrom().item( check( identity, permission ) ) )
                    .build()
            );
    }

    boolean check( SecurityIdentity identity, Permission permission ) {
        if ( permission instanceof NamespacePermission nsPermission ) {
            return accessService.isAllowed( identity, nsPermission.operation(), nsPermission.namespace() );
        }
        if ( permission instanceof StringPermission ) {
            try {
                return accessService.isAllowed( identity, Operation.valueOf( permission.getName() ), null );
            }
            catch ( IllegalArgumentException ex ) {
                return false;
            }
        }
        return false;
    }

}
