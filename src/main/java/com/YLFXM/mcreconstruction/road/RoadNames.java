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

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Road display-name generator. Chinese/English word lists are bundled; they can
 * be replaced by a larger pack later without changing the plan format.
 */
public final class RoadNames {
    private static final List<String> ZH_NAMES = List.of(
            "梧桐", "青云", "望江", "长乐", "安宁", "白桦", "枫林", "松涛",
            "朝阳", "明月", "清溪", "玉兰", "桂花", "银杏", "春晖", "锦绣",
            "广安", "永宁", "文华", "和风", "远山", "星河", "翠竹", "清河");

    private static final List<String> EN_NAMES = List.of(
            "Oak", "Willow", "Maple", "Pine", "Birch", "Cedar", "Elm", "Aspen",
            "River", "Hill", "Meadow", "Sunset", "Dawn", "Harbor", "Kings",
            "Queens", "Market", "Mill", "Stone", "Green", "Rose", "Lark",
            "Foxglove", "Amber");

    /** Road name without the colony prefix, for narrow native-tool buttons. */
    public static String shortName(final RoadPlan plan) {
        String name = label(plan);
        final int english = name.lastIndexOf(" - ");
        if (english >= 0) {
            name = name.substring(english + 3);
        } else {
            final int chinese = name.lastIndexOf('-');
            if (chinese >= 0) {
                name = name.substring(chinese + 1);
            }
        }
        return name;
    }

    /**
     * Short, unique native-tool label. Native build-tool buttons are narrow and
     * truncate at the first long prefix; a compact road name + short hash stays
     * readable and unique.
     */
    public static String fileName(final RoadPlan plan) {
        String name = shortName(plan);
        name = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (name.isBlank()) {
            name = "road_" + plan.id();
        }
        if (name.length() > 40) {
            name = name.substring(0, 40).trim();
        }
        final String suffix = plan.id().toString().substring(0, 4);
        return name + "-" + suffix;
    }

    public static String label(final RoadPlan plan) {
        return plan.displayName() == null || plan.displayName().isBlank()
                ? plan.id().toString() : plan.displayName();
    }

    public static String generate(final ServerLevel level,
                                  final BlockPos pos,
                                  final ServerPlayer player) {
        final IColony colony = IColonyManager.getInstance().getClosestColony(level, pos);
        final String colonyName = colony == null ? "Colony" : colony.getName();
        final boolean chinese = player.getLanguage() != null
                && player.getLanguage().toLowerCase(java.util.Locale.ROOT).startsWith("zh");

        for (int attempt = 0; attempt < 64; attempt++) {
            final String candidate;
            if (chinese) {
                final String base = ZH_NAMES.get(level.getRandom().nextInt(ZH_NAMES.size()));
                final boolean avenue = level.getRandom().nextInt(4) == 0;
                candidate = colonyName + "-" + base + (avenue ? "大道" : "路");
            } else {
                final String base = EN_NAMES.get(level.getRandom().nextInt(EN_NAMES.size()));
                final boolean avenue = level.getRandom().nextInt(4) == 0;
                candidate = colonyName + " - " + base + (avenue ? " Avenue" : " Road");
            }
            if (isUnique(level, candidate)) {
                return candidate;
            }
        }

        // Extremely unlikely fallback: suffix until unique.
        final String base = (chinese ? colonyName + "-道路" : colonyName + " - Road");
        int suffix = 2;
        while (!isUnique(level, base + " " + suffix)) {
            suffix++;
        }
        return base + " " + suffix;
    }

    private static boolean isUnique(final ServerLevel level, final String name) {
        for (final RoadPlan plan : RoadManager.get(level).all()) {
            if (name.equalsIgnoreCase(plan.displayName())) {
                return false;
            }
        }
        return true;
    }

    private RoadNames() {
    }
}
