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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PackageContent {

    private String path;
    private String mediaType;
    @JsonInclude( Include.NON_NULL )
    private String encoding;
    @JsonInclude( Include.NON_NULL )
    private String hash;
    @JsonIgnore
    private Integer order;
    @JsonInclude( Include.NON_NULL )
    private Map<String, String> labels;
    @JsonInclude( Include.NON_NULL )
    @JsonProperty( "x-metadata" )
    private Map<String, Object> xmetadata;
    @JsonInclude( Include.NON_NULL )
    @JsonProperty( "location" )
    private String locationInArchive;

    @JsonIgnore
    public String getLocationInArchive() {
        if ( locationInArchive == null ) {
            return path;
        }
        return locationInArchive;
    }

}
