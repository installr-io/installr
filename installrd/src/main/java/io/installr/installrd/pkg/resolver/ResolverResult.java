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
package io.installr.installrd.pkg.resolver;

import io.installr.lib.pkg.Package;
import io.installr.lib.pkg.PackageId;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors( fluent = true )
public class ResolverResult {

    private final PackageId packageId;
    private final ResolverStatus status;
    private final Package resolvedPackage;

}
