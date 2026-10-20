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
import com.minecolonies.api.colony.workorders.IServerWorkOrder;
import com.minecolonies.core.blocks.BlockDecorationController;
import com.YLFXM.mcreconstruction.Config;
import com.YLFXM.mcreconstruction.CustomRoadPack;
import com.YLFXM.mcreconstruction.MCReconstruction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Full lifecycle data cleanup for one road: work orders, controller/anchor links,
 * RoadManager entry, blueprint snapshots and the published blueprint in the
 * structure pack.
 */
public final class RoadDataCleaner {
    public static void clear(final ServerLevel level, final RoadPlan plan, final boolean removeBlocks) {
        final int removed = removeBlocks ? RoadRemover.removeBlocks(level, plan) : 0;

        removeWorkOrders(plan);
        removeControllerAndAnchor(level, plan);

        RoadManager.get(level).remove(plan.id());
        RoadBlueprintStore.delete(level, plan);
        RoadTemporaryPack.delete(level, plan);


        plan.setState(RoadState.REMOVED);
        CustomRoadPack.install(level.getServer());
        MCReconstruction.LOGGER.info("Cleared road data {} ({} blocks removed)", plan.id(), removed);
    }

    /**
     * Drops every pending work order that belongs to this road. Re-planning a
     * road changes its geometry while keeping the same id/name, so an order
     * left over from the previous selection would silently build the new shape
     * at the old anchor.
     *
     * @return number of removed orders
     */
    public static int releaseWorkOrders(final RoadPlan plan) {
        return removeWorkOrders(plan);
    }

    private static int removeWorkOrders(final RoadPlan plan) {
        final String uuidPath = "mcreconstruction:road/" + plan.id();
        final String namePath = "mcreconstruction:road/" + plan.displayName();
        final String fileName = RoadNames.fileName(plan);
        int removed = 0;
        for (final IColony colony : IColonyManager.getInstance().getAllColonies()) {
            final List<Integer> matches = new ArrayList<>();
            for (final var entry : colony.getWorkManager().getWorkOrders().entrySet()) {
                final IServerWorkOrder order = entry.getValue();
                if (order == null) {
                    continue;
                }
                final String path = order.getStructurePath();
                if (path != null && (path.equals(uuidPath) || path.equals(namePath)
                        || path.contains(fileName))) {
                    matches.add(entry.getKey());
                }
            }
            for (final int id : matches) {
                colony.getWorkManager().removeWorkOrder(id);
            }
            removed += matches.size();
        }
        return removed;
    }

    private static void removeControllerAndAnchor(final ServerLevel level, final RoadPlan plan) {
        for (final BlockPos pos : new BlockPos[]{plan.controllerPos(), plan.anchorPos()}) {
            if (level.getBlockState(pos).getBlock() instanceof BlockDecorationController) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        final ResourceLocation anchorId = ResourceLocation.tryParse(Config.roadAnchorBlock());
        final ResourceLocation placedAnchor = BuiltInRegistries.BLOCK.getKey(
                level.getBlockState(plan.anchorPos()).getBlock());
        if (anchorId != null && anchorId.equals(placedAnchor)) {
            level.setBlock(plan.anchorPos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private RoadDataCleaner() {
    }
}
