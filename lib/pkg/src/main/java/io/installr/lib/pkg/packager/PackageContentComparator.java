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

import java.util.Comparator;

import io.installr.lib.pkg.PackageContent;

public class PackageContentComparator implements Comparator<PackageContent> {

    @Override
    public int compare( PackageContent c1, PackageContent c2 ) {
        int compare = Integer.compare( c1.getOrder(), c2.getOrder() );
        if ( compare == 0 ) {
            return c1.getPath().compareTo( c2.getPath() );
        }
        return compare;
    }

}
