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

import static io.installr.lib.pkg.packager.InstallrPackager.MEDIA_TYPE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ContentAssignmentMatcherTest {

    @Test
    void testDetermineContentAssignment() {
        final List<ContentAssignment> contentAssignments = List.of(
            new ContentAssignment( "docker/*.network.yml", MEDIA_TYPE_PREFIX + "docker-network", "UTF-8", 1 ),
            new ContentAssignment( "docker/**", MEDIA_TYPE_PREFIX + "docker", "UTF-8", 1 ),
            new ContentAssignment( "sql/**", MEDIA_TYPE_PREFIX + "sql", "UTF-8", 2 )

        );
        final ContentAssignmentMatcher contentAssignmentMatcher = new ContentAssignmentMatcher( contentAssignments );
        ContentAssignment matchedAssignment;
        matchedAssignment = contentAssignmentMatcher
            .determineContentAssignment( Path.of( "docker/my-app.network.yml" ) );
        assertEquals( contentAssignments.get( 0 ), matchedAssignment );
        matchedAssignment = contentAssignmentMatcher
            .determineContentAssignment( Path.of( "docker/my-app.yml" ) );
        assertEquals( contentAssignments.get( 1 ), matchedAssignment );
        matchedAssignment = contentAssignmentMatcher
            .determineContentAssignment( Path.of( "docker/stack/my-app.yml" ) );
        assertEquals( contentAssignments.get( 1 ), matchedAssignment );
        matchedAssignment = contentAssignmentMatcher
            .determineContentAssignment( Path.of( "sql/0001-tables.sql" ) );
        assertEquals( contentAssignments.get( 2 ), matchedAssignment );
        matchedAssignment = contentAssignmentMatcher
            .determineContentAssignment( Path.of( "sql/foo/0001-tables.sql" ) );
        assertEquals( contentAssignments.get( 2 ), matchedAssignment );
        final IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> contentAssignmentMatcher
                .determineContentAssignment( Path.of( "not-defined-pattern/file.txt" ) )
        );
        assertEquals( ex.getMessage(), "No content assignment found: not-defined-pattern/file.txt" );
    }
}
