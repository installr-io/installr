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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;

import io.quarkus.logging.LoggingFilter;
import lombok.SneakyThrows;

@LoggingFilter( name = "sifting-log-file-filter" )
public final class SiftingLogFilter implements Filter {

    private final Path logPath;
    private final String discriminatorKey;
    private final String discriminatorDefaultValue;
    private final boolean enabled;
    private final PatternFormatter formatter;
    private final PatternFormatter accessLogFormatter;

    @SneakyThrows
    public SiftingLogFilter(
        @ConfigProperty( name = "installrd.log.format" ) String logFormat,
        @ConfigProperty( name = "installrd.log.sifting-logger.log-dir" ) Path logPath,
        @ConfigProperty( name = "installrd.log.sifting-logger.discriminator.key" ) String discriminatorKey,
        @ConfigProperty( name = "installrd.log.sifting-logger.discriminator.default-value" ) String discriminatorDefVal,        
        @ConfigProperty( name = "installrd.log.sifting-logger.enabled", defaultValue = "false" ) boolean enabled
    ) {
        this.logPath = createOrGetLogPath( logPath );
        this.discriminatorKey = discriminatorKey;
        this.discriminatorDefaultValue = discriminatorDefVal;
        this.enabled = enabled;
        this.formatter = new ColorPatternFormatter( logFormat );
        this.accessLogFormatter = new ColorPatternFormatter( "%s%n" );
    }

    @SneakyThrows( IOException.class )
    private Path createOrGetLogPath( Path logPath ) {
        if ( Files.exists( logPath ) ) {
            return logPath;
        }
        return Files.createDirectories( logPath );
    }

    @Override
    public boolean isLoggable( LogRecord record ) {
        return writeLog( (ExtLogRecord)record );
    }

    private synchronized boolean writeLog( ExtLogRecord record ) {
        try {
            if ( enabled ) {
                Files.write(
                    logFilePath( record ),
                    formatLogRecord( record ),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                );
            }
            return !enabled;
        }
        catch ( IOException ex ) {
            throw new IllegalStateException( ex );
        }
    }

    private byte[] formatLogRecord( ExtLogRecord record ) {
        if ( "io.quarkus.http.access-log".equals( record.getLoggerName() ) ) {
            return accessLogFormatter.format( record ).getBytes();
        }
        return ( formatter.format( record ) ).getBytes();
    }

    private Path logFilePath( ExtLogRecord record ) {
        final String discriminatorValue = record.getMdc( discriminatorKey ) == null
            ? discriminatorDefaultValue
            : record.getMdc( discriminatorKey );
        return logPath.resolve( String.format( "%s.log", discriminatorValue ) );
    }

}
