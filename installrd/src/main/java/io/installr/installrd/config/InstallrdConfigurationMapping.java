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
package io.installr.installrd.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.installr.installrd.config.InstallrdConfiguration.DeployerConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.PackageCacheConfiguration;
import io.installr.installrd.config.InstallrdConfiguration.RepositoryConfiguration;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping
public interface InstallrdConfigurationMapping {

    @WithName( "deployer" )
    Map<String, DeployerConfiguration> deployerConfigurations();

    @WithName( "package-cache" )
    PackageCacheConfiguration packageCache();

    @WithName( "repositories" )
    Optional<List<RepositoryConfiguration>> repositories();

    @WithName( "selector" )
    Map<String, String> selector();

    @WithName( "security" )
    Optional<SecurityConfiguration> security();

}
