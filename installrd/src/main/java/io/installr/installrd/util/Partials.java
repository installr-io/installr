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
package io.installr.installrd.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Partials {

    private Partials() {
    }

    /**
     * Partially applies the first argument of a {@link BiFunction} and returns a {@link Function}.
     * <p>
     * Example:
     *
     * <pre>{@code
     * Stream.of( 1, 2, 3 )
     *     .map( Partials.function( this::asString, "prefix" ) )
     *     .toList();
     * }</pre>
     */
    public static <T, U, R> Function<U, R> toFunction( BiFunction<T, U, R> f, T t ) {
        return u -> f.apply( t, u );
    }

    public static <T, U, V, R> Function<V, R> toFunction( TriFunction<T, U, V, R> f, T t, U u ) {
        return v -> f.apply( t, u, v );
    }

    /**
     * Partially applies the first argument of a {@link BiFunction} returning {@code Boolean} and returns a
     * {@link Predicate}.
     * <p>
     * Example:
     *
     * <pre>{@code
     * Stream.of( 1, 2, 3 )
     *     .filter( Partials.predicate( this::isGreaterThan, "prefix" ) )
     *     .toList();
     * }</pre>
     */
    public static <T, U> Predicate<U> toPredicate( BiFunction<T, U, Boolean> f, T t ) {
        return u -> f.apply( t, u );
    }

    public static <T, U, V> Predicate<V> toPredicate( TriFunction<T, U, V, Boolean> f, T t, U u ) {
        return v -> f.apply( t, u, v );
    }

    /**
     * Partially applies the first argument of a {@link BiConsumer} and returns a {@link Consumer}.
     * <p>
     * Example:
     *
     * <pre>{@code
     * Stream.of( "Alice", "Bob" )
     *     .forEach( Partials.consumer( this::printWithPrefix, "Hello" ) );
     * }</pre>
     */
    public static <T, U> Consumer<U> toConsumer( BiConsumer<T, U> v, T t ) {
        return u -> v.accept( t, u );
    }

    public static <T, U, V> Consumer<V> toConsumer( TriConsumer<T, U, V> f, T t, U u ) {
        return v -> f.accept( t, u, v );
    }

    /**
     * Returns the logical negation of the given {@link Predicate}.
     * <p>
     * Example:
     *
     * <pre>{@code
     * Stream.of( 1, 2, 3, 4, 5 )
     *     .filter( Partials.not( Partials.predicate( this::isGreaterThan, "hi" ) ) )
     *     .toList();
     * }</pre>
     */
    public static <T> Predicate<T> not( Predicate<T> p ) {
        return p.negate();
    }

}
