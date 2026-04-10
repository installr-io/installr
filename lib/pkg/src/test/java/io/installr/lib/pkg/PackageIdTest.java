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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PackageIdTest {

    @Test
    void ofShouldThrowInvalidPackageIdExceptionForEmptyString() {
        assertThrows(
            InvalidPackageIdException.class,
            () -> PackageId.valueOf( "" )
        );
    }

    @Test
    void ofShouldThrowInvalidPackageIdExceptionForNameWithoutVersion() {
        assertThrows(
            InvalidPackageIdException.class,
            () -> PackageId.valueOf( "name-without-version" )
        );
    }

    @Test
    void ofShouldThrowInvalidPackageIdExceptionForVersionWithoutName() {
        assertThrows(
            InvalidPackageIdException.class,
            () -> PackageId.valueOf( "1.0.0" )
        );
    }

    @Test
    void ofShouldThrowInvalidPackageIdExceptionForIllegalVersionString() {
        assertThrows(
            InvalidPackageIdException.class,
            () -> PackageId.valueOf( "name:1..0.0-foo" )
        );
    }

    @Test
    void ofShouldThrowInvalidPackageIdExceptionForIllegalCharacters() {
        assertThrows(
            InvalidPackageIdException.class,
            () -> PackageId.valueOf( "illegal-$name-%:1.0.0" )
        );
    }

}
