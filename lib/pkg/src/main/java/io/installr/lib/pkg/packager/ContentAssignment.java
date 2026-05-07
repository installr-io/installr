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
package io.installr.lib.pkg.packager;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentAssignment {

    @NonNull
    String pathPattern;
    @NonNull
    String mediaType;
    String encoding = StandardCharsets.UTF_8.displayName();
    Integer order = 999;
    Map<String, String> labels;

    public ContentAssignment( @NonNull String pathPattern, @NonNull String mediaType ) {
        this.pathPattern = pathPattern;
        this.mediaType = mediaType;
    }

    public ContentAssignment( @NonNull String pathPattern, @NonNull String mediaType, String encoding ) {
        this.pathPattern = pathPattern;
        this.mediaType = mediaType;
        this.encoding = encoding;
    }

    public ContentAssignment( @NonNull String pathPattern, @NonNull String mediaType, String encoding, Integer order ) {
        this.pathPattern = pathPattern;
        this.mediaType = mediaType;
        this.encoding = encoding;
        this.order = order;
    }

}
