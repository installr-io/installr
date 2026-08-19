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

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.jbosslog.JBossLog;

/**
 * This class is used to explicitly log to the console. This is useful to output boot or state information that should
 * be shown in the terminal resp. captured by journald.
 */
@JBossLog( topic = "console-log" )
@ApplicationScoped
public class ConsoleLog implements BasicLogger {

    @Override
    public boolean isEnabled( Level level ) {
        return log.isEnabled( level );
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    public void trace( Object message ) {
        log.trace( message );
    }

    @Override
    public void trace( Object message, Throwable t ) {
        log.trace( message, t );
    }

    @Override
    public void trace( String loggerFqcn, Object message, Throwable t ) {
        log.trace( loggerFqcn, message, t );
    }

    @Override
    public void trace( String loggerFqcn, Object message, Object[] params, Throwable t ) {
        log.trace( loggerFqcn, message, params, t );
    }

    @Override
    public void tracev( String format, Object ... params ) {
        log.tracev( format, params );
    }

    @Override
    public void tracev( String format, Object param1 ) {
        log.tracev( format, param1 );
    }

    @Override
    public void tracev( String format, Object param1, Object param2 ) {
        log.tracev( format, param1, param2 );
    }

    @Override
    public void tracev( String format, Object param1, Object param2, Object param3 ) {
        log.tracev( format, param1, param2, param3 );
    }

    @Override
    public void tracev( Throwable t, String format, Object ... params ) {
        log.tracev( t, format, params );
    }

    @Override
    public void tracev( Throwable t, String format, Object param1 ) {
        log.tracev( t, format, param1 );
    }

    @Override
    public void tracev( Throwable t, String format, Object param1, Object param2 ) {
        log.tracev( t, format, param1, param2 );
    }

    @Override
    public void tracev( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.tracev( t, format, param1, param2, param3 );
    }

    @Override
    public void tracef( String format, Object ... params ) {
        log.tracef( format, params );
    }

    @Override
    public void tracef( String format, Object param1 ) {
        log.tracef( format, param1 );
    }

    @Override
    public void tracef( String format, Object param1, Object param2 ) {
        log.tracef( format, param1, param2 );
    }

    @Override
    public void tracef( String format, Object param1, Object param2, Object param3 ) {
        log.tracef( format, param1, param2, param3 );
    }

    @Override
    public void tracef( Throwable t, String format, Object ... params ) {
        log.tracef( t, format, params );
    }

    @Override
    public void tracef( Throwable t, String format, Object param1 ) {
        log.tracef( t, format, param1 );
    }

    @Override
    public void tracef( Throwable t, String format, Object param1, Object param2 ) {
        log.tracef( t, format, param1, param2 );
    }

    @Override
    public void tracef( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.tracef( t, format, param1, param2, param3 );
    }

    @Override
    public void tracef( String format, int arg ) {
        log.tracef( format, arg );
    }

    @Override
    public void tracef( String format, int arg1, int arg2 ) {
        log.tracef( format, arg1, arg2 );
    }

    @Override
    public void tracef( String format, int arg1, Object arg2 ) {
        log.tracef( format, arg1, arg2 );
    }

    @Override
    public void tracef( String format, int arg1, int arg2, int arg3 ) {
        log.tracef( format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( String format, int arg1, int arg2, Object arg3 ) {
        log.tracef( format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( String format, int arg1, Object arg2, Object arg3 ) {
        log.tracef( format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( Throwable t, String format, int arg ) {
        log.tracef( t, format, arg );
    }

    @Override
    public void tracef( Throwable t, String format, int arg1, int arg2 ) {
        log.tracef( t, format, arg1, arg2 );
    }

    @Override
    public void tracef( Throwable t, String format, int arg1, Object arg2 ) {
        log.tracef( t, format, arg1, arg2 );
    }

    @Override
    public void tracef( Throwable t, String format, int arg1, int arg2, int arg3 ) {
        log.tracef( t, format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( Throwable t, String format, int arg1, int arg2, Object arg3 ) {
        log.tracef( t, format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( Throwable t, String format, int arg1, Object arg2, Object arg3 ) {
        log.tracef( t, format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( String format, long arg ) {
        log.tracef( format, arg );
    }

    @Override
    public void tracef( String format, long arg1, long arg2 ) {
        log.tracef( format, arg1, arg2 );
    }

    @Override
    public void tracef( String format, long arg1, Object arg2 ) {
        log.tracef( format, arg1, arg2 );
    }

    @Override
    public void tracef( String format, long arg1, long arg2, long arg3 ) {
        log.tracef( format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( String format, long arg1, long arg2, Object arg3 ) {
        log.tracef( format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( String format, long arg1, Object arg2, Object arg3 ) {
        log.tracef( format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( Throwable t, String format, long arg ) {
        log.tracef( t, format, arg );
    }

    @Override
    public void tracef( Throwable t, String format, long arg1, long arg2 ) {
        log.tracef( t, format, arg1, arg2 );
    }

    @Override
    public void tracef( Throwable t, String format, long arg1, Object arg2 ) {
        log.tracef( t, format, arg1, arg2 );
    }

    @Override
    public void tracef( Throwable t, String format, long arg1, long arg2, long arg3 ) {
        log.tracef( t, format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( Throwable t, String format, long arg1, long arg2, Object arg3 ) {
        log.tracef( t, format, arg1, arg2, arg3 );
    }

    @Override
    public void tracef( Throwable t, String format, long arg1, Object arg2, Object arg3 ) {
        log.tracef( t, format, arg1, arg2, arg3 );
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug( Object message ) {
        log.debug( message );
    }

    @Override
    public void debug( Object message, Throwable t ) {
        log.debug( message, t );
    }

    @Override
    public void debug( String loggerFqcn, Object message, Throwable t ) {
        log.debug( loggerFqcn, message, t );
    }

    @Override
    public void debug( String loggerFqcn, Object message, Object[] params, Throwable t ) {
        log.debug( loggerFqcn, message, params, t );
    }

    @Override
    public void debugv( String format, Object ... params ) {
        log.debugv( format, params );
    }

    @Override
    public void debugv( String format, Object param1 ) {
        log.debugv( format, param1 );
    }

    @Override
    public void debugv( String format, Object param1, Object param2 ) {
        log.debugv( format, param1, param2 );
    }

    @Override
    public void debugv( String format, Object param1, Object param2, Object param3 ) {
        log.debugv( format, param1, param2, param3 );
    }

    @Override
    public void debugv( Throwable t, String format, Object ... params ) {
        log.debugv( t, format, params );
    }

    @Override
    public void debugv( Throwable t, String format, Object param1 ) {
        log.debugv( t, format, param1 );
    }

    @Override
    public void debugv( Throwable t, String format, Object param1, Object param2 ) {
        log.debugv( t, format, param1, param2 );
    }

    @Override
    public void debugv( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.debugv( t, format, param1, param2, param3 );
    }

    @Override
    public void debugf( String format, Object ... params ) {
        log.debugf( format, params );
    }

    @Override
    public void debugf( String format, Object param1 ) {
        log.debugf( format, param1 );
    }

    @Override
    public void debugf( String format, Object param1, Object param2 ) {
        log.debugf( format, param1, param2 );
    }

    @Override
    public void debugf( String format, Object param1, Object param2, Object param3 ) {
        log.debugf( format, param1, param2, param3 );
    }

    @Override
    public void debugf( Throwable t, String format, Object ... params ) {
        log.debugf( t, format, params );
    }

    @Override
    public void debugf( Throwable t, String format, Object param1 ) {
        log.debugf( t, format, param1 );
    }

    @Override
    public void debugf( Throwable t, String format, Object param1, Object param2 ) {
        log.debugf( t, format, param1, param2 );
    }

    @Override
    public void debugf( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.debugf( t, format, param1, param2, param3 );
    }

    @Override
    public void debugf( String format, int arg ) {
        log.debugf( format, arg );
    }

    @Override
    public void debugf( String format, int arg1, int arg2 ) {
        log.debugf( format, arg1, arg2 );
    }

    @Override
    public void debugf( String format, int arg1, Object arg2 ) {
        log.debugf( format, arg1, arg2 );
    }

    @Override
    public void debugf( String format, int arg1, int arg2, int arg3 ) {
        log.debugf( format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( String format, int arg1, int arg2, Object arg3 ) {
        log.debugf( format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( String format, int arg1, Object arg2, Object arg3 ) {
        log.debugf( format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( Throwable t, String format, int arg ) {
        log.debugf( t, format, arg );
    }

    @Override
    public void debugf( Throwable t, String format, int arg1, int arg2 ) {
        log.debugf( t, format, arg1, arg2 );
    }

    @Override
    public void debugf( Throwable t, String format, int arg1, Object arg2 ) {
        log.debugf( t, format, arg1, arg2 );
    }

    @Override
    public void debugf( Throwable t, String format, int arg1, int arg2, int arg3 ) {
        log.debugf( t, format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( Throwable t, String format, int arg1, int arg2, Object arg3 ) {
        log.debugf( t, format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( Throwable t, String format, int arg1, Object arg2, Object arg3 ) {
        log.debugf( t, format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( String format, long arg ) {
        log.debugf( format, arg );
    }

    @Override
    public void debugf( String format, long arg1, long arg2 ) {
        log.debugf( format, arg1, arg2 );
    }

    @Override
    public void debugf( String format, long arg1, Object arg2 ) {
        log.debugf( format, arg1, arg2 );
    }

    @Override
    public void debugf( String format, long arg1, long arg2, long arg3 ) {
        log.debugf( format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( String format, long arg1, long arg2, Object arg3 ) {
        log.debugf( format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( String format, long arg1, Object arg2, Object arg3 ) {
        log.debugf( format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( Throwable t, String format, long arg ) {
        log.debugf( t, format, arg );
    }

    @Override
    public void debugf( Throwable t, String format, long arg1, long arg2 ) {
        log.debugf( t, format, arg1, arg2 );
    }

    @Override
    public void debugf( Throwable t, String format, long arg1, Object arg2 ) {
        log.debugf( t, format, arg1, arg2 );
    }

    @Override
    public void debugf( Throwable t, String format, long arg1, long arg2, long arg3 ) {
        log.debugf( t, format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( Throwable t, String format, long arg1, long arg2, Object arg3 ) {
        log.debugf( t, format, arg1, arg2, arg3 );
    }

    @Override
    public void debugf( Throwable t, String format, long arg1, Object arg2, Object arg3 ) {
        log.debugf( t, format, arg1, arg2, arg3 );
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void info( Object message ) {
        log.info( message );
    }

    @Override
    public void info( Object message, Throwable t ) {
        log.info( message, t );
    }

    @Override
    public void info( String loggerFqcn, Object message, Throwable t ) {
        log.info( loggerFqcn, message, t );
    }

    @Override
    public void info( String loggerFqcn, Object message, Object[] params, Throwable t ) {
        log.info( loggerFqcn, message, params, t );
    }

    @Override
    public void infov( String format, Object ... params ) {
        log.infov( format, params );
    }

    @Override
    public void infov( String format, Object param1 ) {
        log.infov( format, param1 );
    }

    @Override
    public void infov( String format, Object param1, Object param2 ) {
        log.infov( format, param1, param2 );
    }

    @Override
    public void infov( String format, Object param1, Object param2, Object param3 ) {
        log.infov( format, param1, param2, param3 );
    }

    @Override
    public void infov( Throwable t, String format, Object ... params ) {
        log.infov( t, format, params );
    }

    @Override
    public void infov( Throwable t, String format, Object param1 ) {
        log.infov( t, format, param1 );
    }

    @Override
    public void infov( Throwable t, String format, Object param1, Object param2 ) {
        log.infov( t, format, param1, param2 );
    }

    @Override
    public void infov( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.infov( t, format, param1, param2, param3 );
    }

    @Override
    public void infof( String format, Object ... params ) {
        log.infof( format, params );
    }

    @Override
    public void infof( String format, Object param1 ) {
        log.infof( format, param1 );
    }

    @Override
    public void infof( String format, Object param1, Object param2 ) {
        log.infof( format, param1, param2 );
    }

    @Override
    public void infof( String format, Object param1, Object param2, Object param3 ) {
        log.infof( format, param1, param2, param3 );
    }

    @Override
    public void infof( Throwable t, String format, Object ... params ) {
        log.infof( t, format, params );
    }

    @Override
    public void infof( Throwable t, String format, Object param1 ) {
        log.infof( t, format, param1 );
    }

    @Override
    public void infof( Throwable t, String format, Object param1, Object param2 ) {
        log.infof( t, format, param1, param2 );
    }

    @Override
    public void infof( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.infof( t, format, param1, param2, param3 );
    }

    @Override
    public void warn( Object message ) {
        log.warn( message );
    }

    @Override
    public void warn( Object message, Throwable t ) {
        log.warn( message, t );
    }

    @Override
    public void warn( String loggerFqcn, Object message, Throwable t ) {
        log.warn( loggerFqcn, message, t );
    }

    @Override
    public void warn( String loggerFqcn, Object message, Object[] params, Throwable t ) {
        log.warn( loggerFqcn, message, params, t );
    }

    @Override
    public void warnv( String format, Object ... params ) {
        log.warnv( format, params );
    }

    @Override
    public void warnv( String format, Object param1 ) {
        log.warnv( format, param1 );
    }

    @Override
    public void warnv( String format, Object param1, Object param2 ) {
        log.warnv( format, param1, param2 );
    }

    @Override
    public void warnv( String format, Object param1, Object param2, Object param3 ) {
        log.warnv( format, param1, param2, param3 );
    }

    @Override
    public void warnv( Throwable t, String format, Object ... params ) {
        log.warnv( t, format, params );
    }

    @Override
    public void warnv( Throwable t, String format, Object param1 ) {
        log.warnv( t, format, param1 );
    }

    @Override
    public void warnv( Throwable t, String format, Object param1, Object param2 ) {
        log.warnv( t, format, param1, param2 );
    }

    @Override
    public void warnv( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.warnv( t, format, param1, param2, param3 );
    }

    @Override
    public void warnf( String format, Object ... params ) {
        log.warnf( format, params );
    }

    @Override
    public void warnf( String format, Object param1 ) {
        log.warnf( format, param1 );
    }

    @Override
    public void warnf( String format, Object param1, Object param2 ) {
        log.warnf( format, param1, param2 );
    }

    @Override
    public void warnf( String format, Object param1, Object param2, Object param3 ) {
        log.warnf( format, param1, param2, param3 );
    }

    @Override
    public void warnf( Throwable t, String format, Object ... params ) {
        log.warnf( t, format, params );
    }

    @Override
    public void warnf( Throwable t, String format, Object param1 ) {
        log.warnf( t, format, param1 );
    }

    @Override
    public void warnf( Throwable t, String format, Object param1, Object param2 ) {
        log.warnf( t, format, param1, param2 );
    }

    @Override
    public void warnf( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.warnf( t, format, param1, param2, param3 );
    }

    @Override
    public void error( Object message ) {
        log.error( message );
    }

    @Override
    public void error( Object message, Throwable t ) {
        log.error( message, t );
    }

    @Override
    public void error( String loggerFqcn, Object message, Throwable t ) {
        log.error( loggerFqcn, message, t );
    }

    @Override
    public void error( String loggerFqcn, Object message, Object[] params, Throwable t ) {
        log.error( loggerFqcn, message, params, t );
    }

    @Override
    public void errorv( String format, Object ... params ) {
        log.errorv( format, params );
    }

    @Override
    public void errorv( String format, Object param1 ) {
        log.errorv( format, param1 );
    }

    @Override
    public void errorv( String format, Object param1, Object param2 ) {
        log.errorv( format, param1, param2 );
    }

    @Override
    public void errorv( String format, Object param1, Object param2, Object param3 ) {
        log.errorv( format, param1, param2, param3 );
    }

    @Override
    public void errorv( Throwable t, String format, Object ... params ) {
        log.errorv( t, format, params );
    }

    @Override
    public void errorv( Throwable t, String format, Object param1 ) {
        log.errorv( t, format, param1 );
    }

    @Override
    public void errorv( Throwable t, String format, Object param1, Object param2 ) {
        log.errorv( t, format, param1, param2 );
    }

    @Override
    public void errorv( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.errorv( t, format, param1, param2, param3 );
    }

    @Override
    public void errorf( String format, Object ... params ) {
        log.errorf( format, params );
    }

    @Override
    public void errorf( String format, Object param1 ) {
        log.errorf( format, param1 );
    }

    @Override
    public void errorf( String format, Object param1, Object param2 ) {
        log.errorf( format, param1, param2 );
    }

    @Override
    public void errorf( String format, Object param1, Object param2, Object param3 ) {
        log.errorf( format, param1, param2, param3 );
    }

    @Override
    public void errorf( Throwable t, String format, Object ... params ) {
        log.errorf( t, format, params );
    }

    @Override
    public void errorf( Throwable t, String format, Object param1 ) {
        log.errorf( t, format, param1 );
    }

    @Override
    public void errorf( Throwable t, String format, Object param1, Object param2 ) {
        log.errorf( t, format, param1, param2 );
    }

    @Override
    public void errorf( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.errorf( t, format, param1, param2, param3 );
    }

    @Override
    public void fatal( Object message ) {
        log.fatal( message );
    }

    @Override
    public void fatal( Object message, Throwable t ) {
        log.fatal( message, t );
    }

    @Override
    public void fatal( String loggerFqcn, Object message, Throwable t ) {
        log.fatal( loggerFqcn, message, t );
    }

    @Override
    public void fatal( String loggerFqcn, Object message, Object[] params, Throwable t ) {
        log.fatal( loggerFqcn, message, params, t );
    }

    @Override
    public void fatalv( String format, Object ... params ) {
        log.fatalv( format, params );
    }

    @Override
    public void fatalv( String format, Object param1 ) {
        log.fatalv( format, param1 );
    }

    @Override
    public void fatalv( String format, Object param1, Object param2 ) {
        log.fatalv( format, param1, param2 );
    }

    @Override
    public void fatalv( String format, Object param1, Object param2, Object param3 ) {
        log.fatalv( format, param1, param2, param3 );
    }

    @Override
    public void fatalv( Throwable t, String format, Object ... params ) {
        log.fatalv( t, format, params );
    }

    @Override
    public void fatalv( Throwable t, String format, Object param1 ) {
        log.fatalv( t, format, param1 );
    }

    @Override
    public void fatalv( Throwable t, String format, Object param1, Object param2 ) {
        log.fatalv( t, format, param1, param2 );
    }

    @Override
    public void fatalv( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.fatalv( t, format, param1, param2, param3 );
    }

    @Override
    public void fatalf( String format, Object ... params ) {
        log.fatalf( format, params );
    }

    @Override
    public void fatalf( String format, Object param1 ) {
        log.fatalf( format, param1 );
    }

    @Override
    public void fatalf( String format, Object param1, Object param2 ) {
        log.fatalf( format, param1, param2 );
    }

    @Override
    public void fatalf( String format, Object param1, Object param2, Object param3 ) {
        log.fatalf( format, param1, param2, param3 );
    }

    @Override
    public void fatalf( Throwable t, String format, Object ... params ) {
        log.fatalf( t, format, params );
    }

    @Override
    public void fatalf( Throwable t, String format, Object param1 ) {
        log.fatalf( t, format, param1 );
    }

    @Override
    public void fatalf( Throwable t, String format, Object param1, Object param2 ) {
        log.fatalf( t, format, param1, param2 );
    }

    @Override
    public void fatalf( Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.fatalf( t, format, param1, param2, param3 );
    }

    @Override
    public void log( Level level, Object message ) {
        log.log( level, message );
    }

    @Override
    public void log( Level level, Object message, Throwable t ) {
        log.log( level, message, t );
    }

    @Override
    public void log( Level level, String loggerFqcn, Object message, Throwable t ) {
        log.log( level, loggerFqcn, message, t );
    }

    @Override
    public void log( String loggerFqcn, Level level, Object message, Object[] params, Throwable t ) {
        log.log( loggerFqcn, level, message, params, t );
    }

    @Override
    public void logv( Level level, String format, Object ... params ) {
        log.logv( level, format, params );
    }

    @Override
    public void logv( Level level, String format, Object param1 ) {
        log.logv( level, format, param1 );
    }

    @Override
    public void logv( Level level, String format, Object param1, Object param2 ) {
        log.logv( level, format, param1, param2 );
    }

    @Override
    public void logv( Level level, String format, Object param1, Object param2, Object param3 ) {
        log.logv( level, format, param1, param2, param3 );
    }

    @Override
    public void logv( Level level, Throwable t, String format, Object ... params ) {
        log.logv( level, t, format, params );
    }

    @Override
    public void logv( Level level, Throwable t, String format, Object param1 ) {
        log.logv( level, t, format, param1 );
    }

    @Override
    public void logv( Level level, Throwable t, String format, Object param1, Object param2 ) {
        log.logv( level, t, format, param1, param2 );
    }

    @Override
    public void logv( Level level, Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.logv( level, t, format, param1, param2, param3 );
    }

    @Override
    public void logv( String loggerFqcn, Level level, Throwable t, String format, Object ... params ) {
        log.logv( loggerFqcn, level, t, format, params );
    }

    @Override
    public void logv( String loggerFqcn, Level level, Throwable t, String format, Object param1 ) {
        log.logv( loggerFqcn, level, t, format, param1 );
    }

    @Override
    public void logv( String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2 ) {
        log.logv( loggerFqcn, level, t, format, param1, param2 );
    }

    @Override
    public void logv(
        String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2, Object param3
    ) {
        log.logv( loggerFqcn, level, t, format, param1, param2, param3 );
    }

    @Override
    public void logf( Level level, String format, Object ... params ) {
        log.logf( level, format, params );
    }

    @Override
    public void logf( Level level, String format, Object param1 ) {
        log.logf( level, format, param1 );
    }

    @Override
    public void logf( Level level, String format, Object param1, Object param2 ) {
        log.logf( level, format, param1, param2 );
    }

    @Override
    public void logf( Level level, String format, Object param1, Object param2, Object param3 ) {
        log.logf( level, format, param1, param2, param3 );
    }

    @Override
    public void logf( Level level, Throwable t, String format, Object ... params ) {
        log.logf( level, t, format, params );
    }

    @Override
    public void logf( Level level, Throwable t, String format, Object param1 ) {
        log.logf( level, t, format, param1 );
    }

    @Override
    public void logf( Level level, Throwable t, String format, Object param1, Object param2 ) {
        log.logf( level, t, format, param1, param2 );
    }

    @Override
    public void logf( Level level, Throwable t, String format, Object param1, Object param2, Object param3 ) {
        log.logf( level, t, format, param1, param2, param3 );
    }

    @Override
    public void logf( String loggerFqcn, Level level, Throwable t, String format, Object param1 ) {
        log.logf( loggerFqcn, level, t, format, param1 );
    }

    @Override
    public void logf( String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2 ) {
        log.logf( loggerFqcn, level, t, format, param1, param2 );
    }

    @Override
    public void logf(
        String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2, Object param3
    ) {
        log.logf( loggerFqcn, level, t, format, param1, param2, param3 );
    }

    @Override
    public void logf( String loggerFqcn, Level level, Throwable t, String format, Object ... params ) {
        log.logf( loggerFqcn, level, t, format, params );
    }

}
