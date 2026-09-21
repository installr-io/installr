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
package io.installr.installrd.deployment.deployer.docker;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.installr.lib.deployer.AbstractDeployerConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DockerDeployerConfig extends AbstractDeployerConfig {

    private String host = "unix:///var/run/docker.sock";

    @JsonProperty( "polling-interval" )
    private Duration pollingInterval = Duration.ofMillis( 500 );

    @JsonProperty( "polling-timeout" )
    private Duration pollingTimeout = Duration.ofSeconds( 60 );

}
