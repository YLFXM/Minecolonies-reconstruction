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

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.util.BlockInfo;
import com.YLFXM.mcreconstruction.Config;
import com.YLFXM.mcreconstruction.MCReconstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Removes only blocks explicitly specified by the road blueprint. Air and all
 * Structurize substitution placeholder families are skipped.
 */
public final class RoadRemover {
    public static boolean remove(final ServerLevel level, final RoadPlan plan) {
        RoadDataCleaner.clear(level, plan, true);
        return true;
    }

    public static int removeBlocks(final ServerLevel level, final RoadPlan plan) {
        Blueprint blueprint = RoadBlueprintStore.load(level, plan);
        if (blueprint == null) {
            blueprint = RoadBlueprintGenerator.generate(plan, level, Config.defaultLampReserveHeight());
        }
        if (blueprint == null) {
            MCReconstruction.LOGGER.warn("No blueprint available to remove road {}", plan.id());
            return 0;
        }

        // Blueprint placement is anchored at the visible anchor block, not the
        // decorative controller (which may be one block to the side).
        final BlockPos origin = plan.anchorPos().subtract(blueprint.getPrimaryBlockOffset());
        int removed = 0;
        for (final BlockInfo info : blueprint.getBlockInfoAsList()) {
            final BlockState state = info.getState();
            if (!isRemovable(state)) {
                continue;
            }
            final BlockPos world = origin.offset(info.getPos());
            if (!level.getBlockState(world).isAir()) {
                level.setBlock(world, Blocks.AIR.defaultBlockState(), 3);
                removed++;
            }
        }
        return removed;
    }

    static boolean isRemovable(final BlockState state) {
        if (state.isAir()) {
            return false;
        }
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) {
            return false;
        }
        return !"structurize".equals(id.getNamespace()) || !id.getPath().contains("substitution");
    }

    private RoadRemover() {
    }
}
