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
package io.installr.installrd.deployment.deployer.http;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.installr.installrd.common.http.HttpClientConfig;
import io.installr.installrd.util.MapUtil;
import io.installr.lib.deployer.DeployerConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode( callSuper = false )
public class HttpDeployerConfig extends HttpClientConfig implements DeployerConfig {

    private Map<String, Object> env;

    @Override
    public Optional<Map<String, String>> env() {
        return Optional.ofNullable( MapUtil.flatten( env ) );
    }

    public void setEnv( Map<String, Object> env ) {
        this.env = env;
    }

    @JsonProperty( "polling-interval" )
    private Duration pollingInterval = Duration.ofMillis( 500 );

    @JsonProperty( "polling-timeout" )
    private Duration pollingTimeout = Duration.ofSeconds( 60 );

}
