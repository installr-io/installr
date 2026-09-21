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
package io.installr.installrd.util;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

public class PropertyMapUtil {

    private static final JavaPropsMapper objectMapper;

    static {
        objectMapper = new JavaPropsMapper();
        objectMapper.findAndRegisterModules();
    }

    public static <T> T readPropertyMap( Map<String, String> map, Class<T> javaType ) {
        try {
            return objectMapper.readMapAs( map, javaType );
        }
        catch ( IOException ex ) {
            throw new IllegalStateException( ex );
        }
    }

}
