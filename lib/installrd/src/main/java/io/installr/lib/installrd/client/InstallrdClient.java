/*
 * Installr.io
 *
 * Copyright (c) 2025-present Dorian Vallant
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.installr.lib.installrd.client;

import io.installr.lib.installrd.controller.DaemonController;
import io.installr.lib.installrd.controller.InstallController;
import io.installr.lib.installrd.controller.NamespaceController;
import io.installr.lib.installrd.controller.PackageLogController;
import io.installr.lib.installrd.controller.TransactionController;
import jakarta.ws.rs.Path;

@Path( "/" )
public interface InstallrdClient
    extends DaemonController, InstallController, NamespaceController, TransactionController, PackageLogController {
}
