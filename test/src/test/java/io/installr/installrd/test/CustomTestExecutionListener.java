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
package io.installr.installrd.test;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class CustomTestExecutionListener implements TestExecutionListener {

    private String currentContainer;

    @Override
    public void executionStarted( TestIdentifier testIdentifier ) {
        if ( testIdentifier.isContainer() ) {
            currentContainer = testIdentifier.getDisplayName();
            System.out.printf( "\n---\n--- %s ---\n---\n", testIdentifier.getDisplayName() );
        }
        else {
            System.out.printf( "\n--- %s#%s ---\n\n", currentContainer, testIdentifier.getDisplayName() );
        }
        TestExecutionListener.super.executionStarted( testIdentifier );
    }

}
