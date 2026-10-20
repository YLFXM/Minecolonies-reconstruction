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
package com.YLFXM.mcreconstruction.client;

import com.YLFXM.mcreconstruction.ModRegistry;
import com.YLFXM.mcreconstruction.network.ClientRoadCache;
import com.YLFXM.mcreconstruction.road.RoadManager;
import com.YLFXM.mcreconstruction.road.RoadPlan;
import com.YLFXM.mcreconstruction.road.RoadSegment;
import com.YLFXM.mcreconstruction.road.RoadState;
import com.YLFXM.mcreconstruction.road.RoadWandBinding;
import com.YLFXM.mcreconstruction.road.SegmentKind;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * Client-only planning preview built from the current A/B selection and the
 * Wand-bound roadmap. It is rendered as a white AABB before any WorkOrder.
 */
public final class ClientRoadPreview {
    private static RoadPlan preview;

    public static RoadPlan plan() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !RoadSelectionClient.hasBoth()) {
            preview = null;
            return null;
        }
        final BlockPos controller = boundController(minecraft);
        RoadPlan base = null;
        if (controller != null) {
            for (final RoadPlan candidate : ClientRoadCache.roads()) {
                if (candidate.controllerPos().equals(controller)) {
                    base = candidate;
                    break;
                }
            }
        }
        final BlockPos a = RoadSelectionClient.a();
        final BlockPos b = RoadSelectionClient.b();
        if (base == null) {
            final Direction facing = a.getX() == b.getX()
                    ? (b.getZ() > a.getZ() ? Direction.SOUTH : Direction.NORTH)
                    : (b.getX() > a.getX() ? Direction.EAST : Direction.WEST);
            base = new RoadPlan(UUID.randomUUID(), minecraft.level.dimension(), a, a,
                    facing, false, "Minecolonies Original", "street",
                    4, 2, 1, com.YLFXM.mcreconstruction.Config.lampSpacing());
        }

        final boolean sameX = a.getX() == b.getX();
        final boolean sameZ = a.getZ() == b.getZ();
        if (sameX == sameZ) {
            preview = null;
            return null;
        }
        final Direction.Axis axis = sameX ? Direction.Axis.Z : Direction.Axis.X;
        final int minX = Math.min(a.getX(), b.getX());
        final int minZ = Math.min(a.getZ(), b.getZ());
        final int length = (axis == Direction.Axis.X
                ? Math.max(a.getX(), b.getX()) - minX
                : Math.max(a.getZ(), b.getZ()) - minZ) + 1;

        final boolean mirrored = RoadSelectionClient.mirror();
        final int side = mirrored ? -Math.abs(base.widthSide()) : Math.abs(base.widthSide());
        final RoadPlan plan = new RoadPlan(UUID.randomUUID(), base.dimension(), base.anchorPos(),
                base.controllerPos(), base.controllerFacing(), mirrored, base.pack(), base.variant(),
                base.width(), base.walkWidth(), side, base.lampSpacing());
        plan.addSegment(new RoadSegment(new BlockPos(minX, a.getY(), minZ), length, axis, 0, SegmentKind.ROAD));
        plan.setState(RoadState.PLANNED);
        preview = plan;
        return plan;
    }

    private static BlockPos boundController(final Minecraft minecraft) {
        final ItemStack main = minecraft.player.getMainHandItem();
        if (main.is(ModRegistry.ROAD_WAND.get())) {
            final BlockPos bound = RoadWandBinding.getBoundController(main);
            if (bound != null) {
                return bound;
            }
        }
        final ItemStack offhand = minecraft.player.getOffhandItem();
        if (offhand.is(ModRegistry.ROAD_WAND.get())) {
            final BlockPos bound = RoadWandBinding.getBoundController(offhand);
            if (bound != null) {
                return bound;
            }
        }
        for (final ItemStack stack : minecraft.player.getInventory().items) {
            if (stack.is(ModRegistry.ROAD_WAND.get())) {
                final BlockPos bound = RoadWandBinding.getBoundController(stack);
                if (bound != null) {
                    return bound;
                }
            }
        }
        return null;
    }

    private ClientRoadPreview() {
    }
}
