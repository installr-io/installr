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
package io.installr.lib.installrd.pkg;

import java.time.Instant;
import java.util.UUID;

import org.semver4j.Semver;

import io.installr.lib.pkg.PackageContent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode( callSuper = true )
public class ItemLogRecord extends PackageContent {

    private Long id;
    private String namespace;
    private String packageName;
    private Semver version;
    private UUID installedBy;
    private Instant installedOn;

    @Accessors( fluent = true )
    private boolean hasError;

}
