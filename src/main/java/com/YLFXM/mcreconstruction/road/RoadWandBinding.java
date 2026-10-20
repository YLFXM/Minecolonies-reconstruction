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

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Stores the bound road-controller position on the Road Wand itself.
 */
public final class RoadWandBinding {
    private static final String KEY = "mcreconstruction:road_controller";

    public static void bind(final ItemStack stack, final BlockPos controllerPos) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(KEY, controllerPos.asLong()));
    }

    public static BlockPos getBoundController(final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(KEY)) {
            return null;
        }
        final long packed = data.copyTag().getLong(KEY);
        return packed == 0L ? null : BlockPos.of(packed);
    }

    private RoadWandBinding() {
    }
}
