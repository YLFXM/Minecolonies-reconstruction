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
package com.YLFXM.mcreconstruction;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<String> ROAD_FILL_BLOCK = BUILDER
            .comment("Fallback filler block for road support fill. Prefer the original fill material selection when available.")
            .define("roadFillBlock", "minecraft:stone");

    private static final ModConfigSpec.ConfigValue<String> ROAD_ANCHOR_BLOCK = BUILDER
            .comment("Temporary visible anchor/post block next to the decoration controller; replaced by a lamp in the final road.")
            .define("roadAnchorBlock", "minecraft:stone_bricks");

    private static final ModConfigSpec.IntValue LAMP_SPACING = BUILDER
            .comment("Default lamp spacing in road units (0 disables lamps).")
            .defineInRange("lampSpacing", 4, 0, 8);

    private static final ModConfigSpec.IntValue DEFAULT_LAMP_RESERVE_HEIGHT = BUILDER
            .comment("Reserved lamp height used by the road preview when no lampunit template is available.")
            .defineInRange("defaultLampReserveHeight", 6, 0, 32);

    private static final ModConfigSpec.IntValue MAX_ROAD_LENGTH = BUILDER
            .comment("Maximum road length in road units for one plan.")
            .defineInRange("maxRoadLength", 256, 1, 2048);

    private static final ModConfigSpec.BooleanValue CREATIVE_INSTANT_BUILD = BUILDER
            .comment("In creative mode, Build places the road immediately instead of creating a WorkOrder.")
            .define("creativeInstantBuild", true);

    private static final ModConfigSpec.BooleanValue DEBUG_LOG = BUILDER
            .comment("Log road plan/AABB generation details.")
            .define("debugLog", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static String roadFillBlock() {
        return ROAD_FILL_BLOCK.get();
    }

    public static String roadAnchorBlock() {
        return ROAD_ANCHOR_BLOCK.get();
    }

    public static int lampSpacing() {
        return LAMP_SPACING.get();
    }

    public static int defaultLampReserveHeight() {
        return DEFAULT_LAMP_RESERVE_HEIGHT.get();
    }

    public static int maxRoadLength() {
        return MAX_ROAD_LENGTH.get();
    }

    public static boolean creativeInstantBuild() {
        return CREATIVE_INSTANT_BUILD.get();
    }

    public static boolean debugLog() {
        return DEBUG_LOG.get();
    }

    private Config() {
    }
}
