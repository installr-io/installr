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
package io.installr.lib.installrd.client;

/**
 * Thrown if a client configuration file is missing, unreadable or invalid.
 */
public class InvalidClientConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidClientConfigurationException( String message, Object ... args ) {
        super( String.format( message, args ) );
    }

    public InvalidClientConfigurationException( Throwable cause, String message, Object ... args ) {
        super( String.format( message, args ), cause );
    }

}
