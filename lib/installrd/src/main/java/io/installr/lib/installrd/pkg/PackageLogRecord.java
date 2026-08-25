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
package io.installr.lib.installrd.pkg;

import java.time.Instant;
import java.util.UUID;

import org.jboss.resteasy.reactive.RestQuery;

import io.installr.lib.installrd.util.QueryBuilder;
import io.installr.lib.pkg.PackageInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageLogRecord {

    private String namespace;
    private UUID txId;
    private String packageName;
    private PackageInfo packageInfo;
    private String repositoryId;
    private Action action;
    private Instant actionTimestamp;
    private PackageStatus status;

    @Data
    public static class Filter {
        @RestQuery
        private String packagename;
        @RestQuery
        private Action action;
        @RestQuery
        private PackageStatus status;
        @RestQuery
        private Instant timestamp;
        @RestQuery
        private UUID transactionId;
        @RestQuery
        private String repositoryId;

        public QueryBuilder buildQuery() {
            final QueryBuilder qb = new QueryBuilder();
            qb.put( "package_name", packagename );
            qb.put( "action", action );
            qb.put( "status", status );
            qb.put( "action_timestamp", timestamp );
            qb.put( "txId", transactionId );
            qb.put( "repository_id", repositoryId );
            return qb;
        }
    }

}
