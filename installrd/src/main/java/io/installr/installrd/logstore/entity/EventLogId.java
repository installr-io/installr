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
package io.installr.installrd.logstore.entity;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Technical JPA identity for EVENT_LOG, which has no natural primary key. Safe for insert-only use because each
 * EventLog.put() call is its own
 *
 * @Transactional boundary (no outer transaction in callers), so each insert gets a fresh JPA session with no L1-cache
 *                collision.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventLogId implements Serializable {
    private String namespace;
    private UUID txId;
    private UUID id;
}
