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
package io.installr.installrd.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseCredentialsTest {

    @TempDir
    Path configLocation;

    // --- new database ---

    @Test
    void ensure_should_create_credentials_file() throws IOException {
        final DatabaseCredentials credentials = DatabaseCredentials.ensure( configLocation );

        final Path file = configLocation.resolve( DatabaseCredentials.FILE_NAME );
        assertTrue( Files.exists( file ) );
        assertFalse( credentials.password().isEmpty() );
        assertFalse( Files.readString( file ).contains( "username" ) );
        if ( FileSystems.getDefault().supportedFileAttributeViews().contains( "posix" ) ) {
            assertEquals( "rw-r-----", PosixFilePermissions.toString( Files.getPosixFilePermissions( file ) ) );
        }
    }

    @Test
    void ensure_should_return_stored_credentials() throws IOException {
        final DatabaseCredentials first = DatabaseCredentials.ensure( configLocation );

        assertEquals( first, DatabaseCredentials.ensure( configLocation ) );
        assertEquals( first, DatabaseCredentials.read( configLocation.resolve( DatabaseCredentials.FILE_NAME ) ) );
    }

    @Test
    void new_database_should_be_created_with_generated_credentials() throws Exception {
        final DatabaseCredentials credentials = DatabaseCredentials.ensure( configLocation );

        // the datasource creates the database on first connect
        try ( Connection connection = connect( DatabaseCredentials.USERNAME, credentials.password() ) ) {
            assertEquals( DatabaseCredentials.USERNAME, currentUser( connection ) );
        }
        assertThrows( SQLException.class, () -> connect( DatabaseCredentials.USERNAME, "" ).close() );
    }

    // --- existing database ---

    @Test
    void existing_database_without_password_should_be_protected() throws Exception {
        try (
            Connection connection = connect( "", "" );
            Statement statement = connection.createStatement()
        ) {
            statement.execute( "CREATE TABLE DATA (ID INT)" );
            statement.execute( "INSERT INTO DATA VALUES (42)" );
        }

        final DatabaseCredentials credentials = DatabaseCredentials.ensure( configLocation );

        try (
            Connection connection = connect( DatabaseCredentials.USERNAME, credentials.password() );
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery( "SELECT ID FROM DATA" )
        ) {
            assertTrue( rs.next() );
            assertEquals( 42, rs.getInt( 1 ) );
        }
        // the previous user is locked
        assertThrows( SQLException.class, () -> connect( "", "" ).close() );
        assertThrows( SQLException.class, () -> connect( "", credentials.password() ).close() );
    }

    @Test
    void existing_protected_database_without_credentials_file_should_fail() throws Exception {
        connect( "SA", "secret" ).close();

        assertThrows( IllegalStateException.class, () -> DatabaseCredentials.ensure( configLocation ) );
        assertFalse( Files.exists( configLocation.resolve( DatabaseCredentials.FILE_NAME ) ) );
    }

    // --- read ---

    @Test
    void read_should_fail_without_password() throws IOException {
        final Path file = configLocation.resolve( DatabaseCredentials.FILE_NAME );
        Files.writeString( file, "# empty\n" );

        assertThrows( IllegalStateException.class, () -> DatabaseCredentials.read( file ) );
    }

    // --- helpers ---

    private Connection connect( String user, String password ) throws SQLException {
        final Path database = configLocation.resolve( DatabaseCredentials.DATABASE_NAME ).toAbsolutePath();
        return DriverManager.getConnection( "jdbc:h2:" + database, user, password );
    }

    private static String currentUser( Connection connection ) throws SQLException {
        try (
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery( "SELECT CURRENT_USER" )
        ) {
            rs.next();
            return rs.getString( 1 );
        }
    }

}
