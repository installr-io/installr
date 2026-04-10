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
package io.installr.lib.pkg.packager;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

public class ContentAssignmentMatcher {

    private final List<ContentAssignment> contentAssignments;

    public ContentAssignmentMatcher( List<ContentAssignment> contentAssignments ) {
        this.contentAssignments = contentAssignments;
    }

    ContentAssignment determineContentAssignment( Path relativePath ) {
        if ( contentAssignments != null && !contentAssignments.isEmpty() ) {
            for ( ContentAssignment ca : contentAssignments ) {
                final PathMatcher matcher = FileSystems.getDefault().getPathMatcher( "glob:" + ca.getPathPattern() );
                if ( matcher.matches( relativePath ) ) {
                    return ca;
                }
            }
        }
        throw new IllegalStateException( "No content assignment found: " + relativePath );
    }

}
