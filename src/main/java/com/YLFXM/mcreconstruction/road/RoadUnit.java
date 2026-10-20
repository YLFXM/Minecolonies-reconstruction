/*
 * Copyright (C) 2026 ZhuanZ (YLFXM)
 *
 * MineColonies: Reconstruction Plan - a feature expansion addon for MineColonies.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.YLFXM.mcreconstruction.road;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Direction;

/**
 * Data-driven road unit definition. A unit is extracted from a style pack
 * blueprint by slice; no runtime blueprint is stored here.
 */
public record RoadUnit(
        String id,
        UnitType type,
        String pack,
        String category,
        String source,
        Direction.Axis axis,
        int slice,
        int width,
        int walkWidth,
        int surfaceY,
        int rise,
        int lampSideOffset) {

    public enum UnitType {
        ROAD,
        STAIR,
        LAMP
    }

    public static final class Registry {
        private static final Map<String, RoadUnit> UNITS = new LinkedHashMap<>();

        public static void register(final RoadUnit unit) {
            UNITS.put(unit.id(), unit);
        }

        public static RoadUnit get(final String id) {
            return UNITS.get(id);
        }

        public static Collection<RoadUnit> all() {
            return Collections.unmodifiableCollection(UNITS.values());
        }

        private Registry() {
        }
    }
}
