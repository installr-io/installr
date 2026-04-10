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
package io.installr.installrd.common.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.installr.installrd.common.http.auth.AuthConfig;
import io.installr.installrd.common.http.auth.NoAuthConfig;
import io.installr.installrd.common.http.truststore.TrustStoreConfig;
import lombok.Data;

@Data
public class HttpClientConfig {

    private String url;

    private AuthConfig auth = new NoAuthConfig(); // default

    @JsonProperty( "trust-store" )
    private TrustStoreConfig trustStore;

}
