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

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.security.auth.x500.X500Principal;

import io.installr.installrd.config.InstallrdConfiguration;
import io.installr.lib.installrd.model.NamespaceInfo;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Answers authorization questions for a {@link SecurityIdentity} based on the current {@link AccessPolicy}.
 */
@ApplicationScoped
public class AccessService {

    @Inject
    InstallrdConfiguration config;

    public boolean isAllowed( SecurityIdentity identity, Operation operation, String namespace ) {
        return subjectOf( identity )
            .map( subject -> config.accessPolicy().isAllowed( subject, operation, namespace ) )
            .orElse( false );
    }

    /**
     * Filters the namespaces to those the identity is allowed to read.
     */
    public List<NamespaceInfo> visibleNamespaces( SecurityIdentity identity, List<NamespaceInfo> namespaces ) {
        return namespaces.stream()
            .filter( ns -> isAllowed( identity, Operation.NS_READ, ns.getName() ) )
            .toList();
    }

    /**
     * Returns the subject DN of the identity, e.g. to record who executed a transaction.
     */
    public Optional<String> subjectName( SecurityIdentity identity ) {
        return subjectOf( identity ).map( X500Principal::getName );
    }

    static Optional<X500Principal> subjectOf( SecurityIdentity identity ) {
        if ( identity == null || identity.isAnonymous() ) {
            return Optional.empty();
        }
        final Principal principal = identity.getPrincipal();
        if ( principal instanceof X500Principal x500 ) {
            return Optional.of( x500 );
        }
        try {
            return Optional.of( new X500Principal( principal.getName() ) );
        }
        catch ( IllegalArgumentException ex ) {
            return Optional.empty();
        }
    }

}
