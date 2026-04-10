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
package io.installr.installrd;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain( name = "TestMain" )
public class TestMain {

    public static void main( String[] args ) {
        Quarkus.run( MainApp.class, args );
    }

    public static class MainApp implements QuarkusApplication {

        @Override
        public int run( String ... args ) throws Exception {
            System.out.println( "Running installrd in test mode... (TestMain)" );
            Quarkus.waitForExit();
            return 0;
        }
    }

}
