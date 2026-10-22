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
package io.installr.installrd.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class OperationTest {

    @Test
    void resolve_should_return_single_operation() {
        assertEquals( Set.of( Operation.PKG_INSTALL ), Operation.resolve( "pkg:install" ) );
    }

    @Test
    void resolve_should_expand_resource_wildcard() {
        assertEquals( Set.of( Operation.TX_READ, Operation.TX_CANCEL ), Operation.resolve( "tx:*" ) );
    }

    @Test
    void resolve_should_expand_global_wildcard() {
        assertEquals( EnumSet.allOf( Operation.class ), Operation.resolve( "*" ) );
    }

    @Test
    void resolve_should_ignore_surrounding_whitespace() {
        assertEquals( Set.of( Operation.NS_READ ), Operation.resolve( " ns:read " ) );
    }

    @Test
    void resolve_should_throw_for_unknown_operation() {
        assertThrows( IllegalArgumentException.class, () -> Operation.resolve( "pkg:uninstall" ) );
        assertThrows( IllegalArgumentException.class, () -> Operation.resolve( "foo:*" ) );
        assertThrows( IllegalArgumentException.class, () -> Operation.resolve( "" ) );
    }

    @Test
    void names_should_match_enum_names() {
        assertEquals( Operation.NS_READ, Operation.valueOf( Operation.Names.NS_READ ) );
        assertEquals( Operation.NS_CREATE, Operation.valueOf( Operation.Names.NS_CREATE ) );
        assertEquals( Operation.NS_DELETE, Operation.valueOf( Operation.Names.NS_DELETE ) );
        assertEquals( Operation.PKG_INSTALL, Operation.valueOf( Operation.Names.PKG_INSTALL ) );
        assertEquals( Operation.PKG_REINSTALL, Operation.valueOf( Operation.Names.PKG_REINSTALL ) );
        assertEquals( Operation.PKG_INSTALL_FILE, Operation.valueOf( Operation.Names.PKG_INSTALL_FILE ) );
        assertEquals( Operation.PKG_REINSTALL_FILE, Operation.valueOf( Operation.Names.PKG_REINSTALL_FILE ) );
        assertEquals( Operation.PKG_READ, Operation.valueOf( Operation.Names.PKG_READ ) );
        assertEquals( Operation.TX_READ, Operation.valueOf( Operation.Names.TX_READ ) );
        assertEquals( Operation.TX_CANCEL, Operation.valueOf( Operation.Names.TX_CANCEL ) );
        assertEquals( Operation.DAEMON_STOP, Operation.valueOf( Operation.Names.DAEMON_STOP ) );
    }

}
