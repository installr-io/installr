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
package io.installr.lib.pkg;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PackageInfo {

    @JsonProperty( required = true )
    private PackageId packageId;

    @JsonInclude( Include.NON_NULL )
    private String description;

    @JsonInclude( Include.NON_NULL )
    private List<PackageContent> contents = List.of();

    @JsonInclude( Include.NON_NULL )
    private List<PackageId> dependencies;

    @JsonInclude( Include.NON_NULL )
    @JsonProperty( "x-metadata" )
    private Map<String, Object> xmetadata;

}
