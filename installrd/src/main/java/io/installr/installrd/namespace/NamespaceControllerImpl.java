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

import java.util.List;

import org.jboss.resteasy.reactive.RestResponse;

import io.installr.installrd.security.AccessService;
import io.installr.installrd.security.NamespacePermission;
import io.installr.installrd.security.Operation.Names;
import io.installr.lib.installrd.controller.NamespaceController;
import io.installr.lib.installrd.model.NamespaceInfo;
import io.installr.lib.installrd.resource.NamespaceResource;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NamespaceControllerImpl implements NamespaceController {

    @Inject
    NamespaceService namespaceService;

    @Inject
    AccessService accessService;

    @Inject
    SecurityIdentity identity;

    @Override
    @PermissionsAllowed( Names.NS_CREATE )
    public RestResponse<NamespaceInfo> create( NamespaceResource resource ) {
        final NamespaceInfo info = namespaceService.create( resource.getName() );
        return RestResponse.ResponseBuilder
            .<NamespaceInfo>create( RestResponse.Status.CREATED )
            .entity( info )
            .build();
    }

    @Override
    public List<NamespaceInfo> namespaces() {
        return accessService.visibleNamespaces( identity, namespaceService.list() );
    }

    @Override
    @PermissionsAllowed( value = Names.NS_READ, permission = NamespacePermission.class, params = "namespace" )
    public NamespaceInfo namespace( String namespace ) {
        return namespaceService.get( namespace );
    }

    @Override
    @PermissionsAllowed( Names.NS_DELETE )
    public void delete( String namespace ) {
        namespaceService.delete( namespace );
    }

}
