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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Repairs a road by re-applying the saved/generated blueprint. This is the
 * post-build Repair action; builder-based repair can be added later.
 */
public final class RoadRepair {
    public static boolean repair(final ServerLevel level, final RoadPlan plan) {
        Blueprint blueprint = RoadBlueprintStore.load(level, plan);
        if (blueprint == null) {
            blueprint = RoadBlueprintGenerator.generate(plan, level, Config.defaultLampReserveHeight());
        }
        if (blueprint == null) {
            return false;
        }

        final BlockPos origin = plan.anchorPos().subtract(blueprint.getPrimaryBlockOffset());
        int repaired = 0;
        for (final BlockInfo info : blueprint.getBlockInfoAsList()) {
            final BlockState state = info.getState();
            if (!RoadRemover.isRemovable(state)) {
                continue;
            }
            final BlockPos world = origin.offset(info.getPos());
            if (!level.getBlockState(world).equals(state)) {
                level.setBlock(world, state, 3);
                repaired++;
            }
            if (info.hasTileEntityData() && info.getTileEntityData() != null) {
                final BlockEntity be = level.getBlockEntity(world);
                if (be != null) {
                    final CompoundTag tag = info.getTileEntityData().copy();
                    tag.putInt("x", world.getX());
                    tag.putInt("y", world.getY());
                    tag.putInt("z", world.getZ());
                    be.loadWithComponents(tag, level.registryAccess());
                    be.setChanged();
                }
            }
        }
        plan.setState(RoadState.BUILT);
        RoadManager.get(level).add(plan);
        // Re-applying the blueprint resolves the reserved placeholder, which may
        // sit on the controller cell; put the controller back afterwards.
        RoadControllerMigration.migrate(level, plan);
        MCReconstruction.LOGGER.info("Repaired road {} ({} blocks)", plan.id(), repaired);
        return true;
    }

    private RoadRepair() {
    }
}
