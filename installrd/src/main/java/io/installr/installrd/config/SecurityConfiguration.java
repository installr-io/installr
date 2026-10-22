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
import java.util.Optional;

/**
 * Security section of the global {@code config.yaml}.
 */
public interface SecurityConfiguration {

    /**
     * Subject DNs of the client certificates, which are granted the built-in {@code admin} role.
     */
    Optional<List<String>> admins();

    Optional<List<RoleDefinition>> roles();

    /**
     * Global role bindings; they apply to all namespaces and are the only way to grant global operations.
     */
    Optional<List<RoleBinding>> bindings();

    interface RoleDefinition {
        String name();

        List<String> operations();
    }

    interface RoleBinding {
        /**
         * Subject DN of the client certificate.
         */
        String subject();

        List<String> roles();
    }

}
