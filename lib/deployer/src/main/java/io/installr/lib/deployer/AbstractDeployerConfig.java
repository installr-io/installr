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
package io.installr.lib.deployer;

import java.util.Map;
import java.util.Optional;

public abstract class AbstractDeployerConfig implements DeployerConfig {

    private Map<String, String> env;

    @Override
    public Optional<Map<String, String>> env() {
        return Optional.ofNullable( env );
    }

    public void setEnv( Map<String, String> env ) {
        this.env = env;
    }

}
