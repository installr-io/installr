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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import io.installr.installrd.config.InstallrdConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * The H2 TCP listener accepts other hosts ({@code -tcpAllowOthers}), so the database must be protected by the generated
 * credentials.
 */
@QuarkusTest
class DatabaseListenerIT {

    static final String URL = "jdbc:h2:tcp://localhost:9421/" + DatabaseCredentials.DATABASE_NAME;

    @Inject
    InstallrdConfiguration config;

    @Test
    void connection_without_password_should_be_rejected() {
        assertThrows( SQLException.class, () -> DriverManager.getConnection( URL, DatabaseCredentials.USERNAME, "" ) );
        assertThrows( SQLException.class, () -> DriverManager.getConnection( URL, "", "" ) );
    }

    @Test
    void connection_with_wrong_password_should_be_rejected() {
        final DatabaseCredentials credentials = credentials();
        assertThrows(
            SQLException.class,
            () -> DriverManager.getConnection( URL, DatabaseCredentials.USERNAME, credentials.password() + "x" )
        );
    }

    @Test
    void connection_with_generated_credentials_should_succeed() throws SQLException {
        final DatabaseCredentials credentials = credentials();
        try (
            Connection connection = DriverManager.getConnection(
                URL,
                DatabaseCredentials.USERNAME,
                credentials.password()
            );
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery( "SELECT COUNT(*) FROM TRANSACTION_LOG" )
        ) {
            assertTrue( rs.next() );
        }
    }

    private DatabaseCredentials credentials() {
        try {
            return DatabaseCredentials.read( config.location().resolve( DatabaseCredentials.FILE_NAME ) );
        }
        catch ( Exception ex ) {
            throw new IllegalStateException( ex );
        }
    }

}
