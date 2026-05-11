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

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ExceptionUtil {

    private ExceptionUtil() {
    }

    public static boolean isCausedBy( Throwable ex, Class<? extends Throwable> type ) {
        for ( Throwable cause = ex; cause != null; cause = cause.getCause() ) {
            if ( type.isInstance( cause ) ) {
                return true;
            }
        }
        return false;
    }

    public static <T> Supplier<T> wrap( Callable<T> callable, Function<Exception, RuntimeException> translator ) {
        return () -> {
            try {
                return callable.call();
            }
            catch ( Exception ex ) {
                throw translator.apply( ex );
            }
        };
    }

}
