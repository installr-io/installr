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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Properties;

import lombok.extern.jbosslog.JBossLog;

/**
 * Credentials of the installrd H2 database: the user is always {@value #USERNAME}, its password is stored in
 * {@code <config-location>/.db-credentials}.
 * <p>
 * The file is created on first use with a random password. If the database already exists (created by a version without
 * credentials, i.e. by a user with an empty password), the user {@value #USERNAME} is created and the previous user is
 * locked by setting a random password, which is not stored anywhere.
 */
@JBossLog
public record DatabaseCredentials( String password ) {

    public static final String USERNAME = "INSTALLRD";
    public static final String FILE_NAME = ".db-credentials";
    public static final String DATABASE_NAME = "installrd";

    private static final String PASSWORD_KEY = "password";

    public static synchronized DatabaseCredentials ensure( Path configLocation ) throws IOException {
        final Path file = configLocation.resolve( FILE_NAME );
        if ( Files.exists( file ) ) {
            return read( file );
        }
        Files.createDirectories( configLocation );
        final DatabaseCredentials credentials = new DatabaseCredentials( generatePassword() );
        if ( Files.exists( configLocation.resolve( DATABASE_NAME + ".mv.db" ) ) ) {
            protectExistingDatabase( configLocation.resolve( DATABASE_NAME ), credentials );
        }
        write( file, credentials );
        log.infof( "Database credentials written to %s", file );
        return credentials;
    }

    public static DatabaseCredentials read( Path file ) throws IOException {
        final Properties properties = new Properties();
        try ( Reader reader = Files.newBufferedReader( file ) ) {
            properties.load( reader );
        }
        final String password = properties.getProperty( PASSWORD_KEY );
        if ( password == null || password.isEmpty() ) {
            throw new IllegalStateException( String.format( "No '%s' found in %s", PASSWORD_KEY, file ) );
        }
        return new DatabaseCredentials( password );
    }

    private static void write( Path file, DatabaseCredentials credentials ) throws IOException {
        if ( FileSystems.getDefault().supportedFileAttributeViews().contains( "posix" ) ) {
            Files.createFile(
                file,
                PosixFilePermissions.asFileAttribute( PosixFilePermissions.fromString( "rw-r-----" ) )
            );
        }
        else {
            Files.createFile( file );
        }
        final Properties properties = new Properties();
        properties.setProperty( PASSWORD_KEY, credentials.password() );
        try ( Writer writer = Files.newBufferedWriter( file ) ) {
            properties.store(
                writer,
                String.format( "installrd database credentials (H2, user %s), generated on first start", USERNAME )
            );
        }
    }

    private static String generatePassword() {
        final byte[] bytes = new byte[ 32 ];
        new SecureRandom().nextBytes( bytes );
        // URL-safe alphabet (A-Z, a-z, 0-9, '-', '_'), so it can be used in an SQL string literal
        return Base64.getUrlEncoder().withoutPadding().encodeToString( bytes );
    }

    /*
     * Creates the user INSTALLRD in an existing database, which was created without credentials, and locks the previous
     * user.
     */
    private static void protectExistingDatabase( Path database, DatabaseCredentials credentials ) {
        final String url = "jdbc:h2:" + database.toAbsolutePath() + ";IFEXISTS=TRUE";
        try (
            Connection connection = DriverManager.getConnection( url, "", "" );
            Statement statement = connection.createStatement()
        ) {
            final String previousUser;
            try ( ResultSet rs = statement.executeQuery( "SELECT CURRENT_USER" ) ) {
                rs.next();
                previousUser = rs.getString( 1 );
            }
            statement.execute(
                "CREATE USER IF NOT EXISTS " + USERNAME + " PASSWORD '" + credentials.password() + "' ADMIN"
            );
            // the previous user is not dropped (it may own the schema), but locked with an unknown password
            statement.execute( "SET PASSWORD '" + generatePassword() + "'" );
            log.infof(
                "Existing database %s is now protected by a password (user %s, previous user '%s' locked)",
                database,
                USERNAME,
                previousUser
            );
        }
        catch ( SQLException ex ) {
            throw new IllegalStateException(
                String.format(
                    "Could not protect the existing database %s: %s. If the database is already protected, " + "restore %s with the password of the user %s.",
                    database,
                    ex.getMessage(),
                    database.resolveSibling( FILE_NAME ),
                    USERNAME
                ),
                ex
            );
        }
    }

}
