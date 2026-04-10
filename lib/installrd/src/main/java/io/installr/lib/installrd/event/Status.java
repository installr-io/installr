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
package io.installr.lib.installrd.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class Status {

    public static enum Type {
        Success,
        Skipped,
        Failed,
        Fatal,
        Canceled
    }

    public static final Status Success = new Status( Type.Success );
    public static final Status Canceled = new Status( Type.Canceled );
    public static final Status Failed = new Status( Type.Failed );
    public static final Status Fatal = new Status( Type.Fatal );

    public static final Status Skipped( String message, Object ... args ) {
        return create( Type.Skipped, message, args );
    };

    public static final Status Failed( String message, Object ... args ) {
        return create( Type.Failed, message, args );
    };

    public static final Status Failed( Throwable t ) {
        return create( Type.Failed, t );
    };

    public static final Status Fatal( String message, Object ... args ) {
        return create( Type.Fatal, message, args );
    };

    public static final Status Fatal( Throwable t ) {
        return create( Type.Fatal, t );
    };

    private static Status create( Type type, String message, Object[] args ) {
        return new Status( type, args == null ? message : String.format( message, args ) );
    }

    private static Status create( Type type, Throwable t ) {
        return new Status( type, t == null ? null : t.getMessage() );
    }

    private final Type type;
    private final String message;

    public Status( Type type ) {
        this( type, null );
    }

    @JsonCreator( mode = Mode.PROPERTIES )
    public Status( Type type, String message ) {
        this.type = type;
        this.message = message;
    }

    @JsonGetter( value = "type" )
    public Type type() {
        return type;
    }

    @JsonGetter( value = "message" )
    @JsonInclude( Include.NON_EMPTY )
    public String message() {
        return message;
    }

}
