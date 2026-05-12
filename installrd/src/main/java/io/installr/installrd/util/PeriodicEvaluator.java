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

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PeriodicEvaluator<T> {

    private final Supplier<T> supplier;
    private final Predicate<T> predicate;
    private Duration checkInterval = Duration.ofMillis( 500 );
    private Duration timeout = Duration.ofSeconds( 60 );
    private T value;

    public PeriodicEvaluator( Supplier<T> supplier, Predicate<T> predicate ) {
        this.supplier = supplier;
        this.predicate = predicate;
    }

    public PeriodicEvaluator<T> withInterval( Duration interval ) {
        this.checkInterval = interval;
        return this;
    }

    public PeriodicEvaluator<T> withTimeout( Duration timeout ) {
        this.timeout = timeout;
        return this;
    }

    public boolean evaluate() {
        long startTime = System.nanoTime();
        long timeoutNanos = timeout.toNanos();
        long intervalMillis = checkInterval.toMillis();

        while ( true ) {
            value = supplier.get();
            if ( predicate.test( value ) ) {
                return true;
            }

            long elapsedNanos = System.nanoTime() - startTime;
            if ( elapsedNanos >= timeoutNanos ) {
                return false;
            }

            ThreadUtil.sleep( intervalMillis );
        }
    }

    public Supplier<T> getSupplier() {
        return supplier;
    }

    public Predicate<T> getPredicate() {
        return predicate;
    }

    public Duration getCheckInterval() {
        return checkInterval;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public T getValue() {
        return value;
    }

}
