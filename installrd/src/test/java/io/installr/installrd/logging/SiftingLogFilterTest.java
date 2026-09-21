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
package io.installr.installrd.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SiftingLogFilterTest {

    private static final String LOG_FORMAT = "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n";
    private static final String DISC_KEY = "namespace";
    private static final String DISC_DEFAULT = "installrd";

    private Path logDir;

    @BeforeAll
    static void setupClass() {
        TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
    }

    @BeforeEach
    void setup() throws IOException {
        logDir = Files.createTempDirectory( "sifting-log-filter-test" );
    }

    @AfterEach
    void teardown() throws IOException {
        try ( var files = Files.walk( logDir ) ) {
            files.sorted( Comparator.reverseOrder() ).forEach( p -> p.toFile().delete() );
        }
    }

    @Test
    void isLoggable_returns_true_when_disabled() {
        assertTrue( createFilter( false ).isLoggable( record( "some.logger", "test" ) ) );
    }

    @Test
    void no_file_written_when_disabled() throws IOException {
        createFilter( false ).isLoggable( record( "some.logger", "test message" ) );
        try ( var files = Files.list( logDir ) ) {
            assertFalse( files.findAny().isPresent(), "No log file should be written when disabled" );
        }
    }

    @Test
    void isLoggable_returns_false_when_enabled() {
        assertFalse( createFilter( true ).isLoggable( record( "some.logger", "test message" ) ) );
    }

    @Test
    void writes_to_default_file_when_mdc_absent() throws IOException {
        createFilter( true ).isLoggable( record( "some.logger", "test message" ) );
        assertTrue( Files.exists( logDir.resolve( DISC_DEFAULT + ".log" ) ) );
    }

    @Test
    void writes_to_file_named_after_mdc_value() throws IOException {
        MDC.put( DISC_KEY, "foo" );
        try {
            createFilter( true ).isLoggable( record( "some.logger", "test message" ) );
        }
        finally {
            MDC.remove( DISC_KEY );
        }
        assertTrue( Files.exists( logDir.resolve( "foo.log" ) ) );
    }

    @Test
    void appends_multiple_records_to_same_file() throws IOException {
        SiftingLogFilter filter = createFilter( true );
        filter.isLoggable( record( "some.logger", "first message" ) );
        filter.isLoggable( record( "some.logger", "second message" ) );
        String content = Files.readString( logDir.resolve( DISC_DEFAULT + ".log" ) );
        assertTrue( content.contains( "first message" ) );
        assertTrue( content.contains( "second message" ) );
    }

    @Test
    void access_log_records_are_written_without_log_pattern() throws IOException {
        String message = "127.0.0.1 - - [01/Jan/2026:12:00:00 +0000] \"GET /health HTTP/1.1\" 200 0";
        createFilter( true ).isLoggable( record( "io.quarkus.http.access-log", message ) );
        String content = Files.readString( logDir.resolve( DISC_DEFAULT + ".log" ) );
        assertTrue( content.contains( message ) );
    }

    @Test
    void access_log_timestamp_is_converted_to_configured_timezone() throws IOException {
        ZoneId systemTz = ZoneId.systemDefault();
        ZoneId filterTz = ZoneId.of( "America/Los_Angeles" );

        Instant instant = Instant.parse( "2026-01-01T12:00:00Z" );
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern( "dd/MMM/yyyy:HH:mm:ss Z", Locale.US );
        String systemTimestamp = fmt.withZone( systemTz ).format( instant );
        String filterTimestamp = fmt.withZone( filterTz ).format( instant );

        String message = "127.0.0.1 - - [" + systemTimestamp + "] \"GET /api HTTP/1.1\" 200 42";
        ExtLogRecord record = record( "io.quarkus.http.access-log", message );
        record.setInstant( instant );

        createFilter( true, filterTz.getId() ).isLoggable( record );

        String content = Files.readString( logDir.resolve( DISC_DEFAULT + ".log" ) );
        assertTrue( content.contains( filterTimestamp ), "Converted timestamp must be present in log file" );
    }

    private SiftingLogFilter createFilter( boolean enabled ) {
        return createFilter( enabled, ZoneId.systemDefault().getId() );
    }

    private SiftingLogFilter createFilter( boolean enabled, String timezone ) {
        return new SiftingLogFilter(
            LOG_FORMAT,
            timezone,
            logDir,
            DISC_KEY,
            DISC_DEFAULT,
            enabled
        );
    }

    private ExtLogRecord record( String loggerName, String message ) {
        ExtLogRecord record = new ExtLogRecord( Level.INFO, message, SiftingLogFilterTest.class.getName() );
        record.setLoggerName( loggerName );
        return record;
    }

}
