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
package io.installr.installrd.pkg;

import java.util.List;

import io.installr.installrd.logstore.PackageLog;
import io.installr.installrd.security.NamespacePermission;
import io.installr.installrd.security.Operation.Names;
import io.installr.lib.installrd.controller.PackageLogController;
import io.installr.lib.installrd.pkg.PackageLogRecord;
import io.installr.lib.installrd.pkg.PackageLogRecord.Filter;
import io.installr.lib.installrd.util.Pagination;
import io.quarkus.security.PermissionsAllowed;
import jakarta.inject.Inject;

public class PackageLogControllerImpl implements PackageLogController {

    @Inject
    PackageLog packageLog;

    @Override
    @PermissionsAllowed( value = Names.PKG_READ, permission = NamespacePermission.class, params = "namespace" )
    public List<PackageLogRecord> packageLog( String namespace, Pagination pagination, Filter filter ) {
        if ( pagination.getPage() == -1 ) {
            return packageLog.getAll( namespace, filter );
        }
        return packageLog.getPaged( namespace, pagination, filter );
    }

}
