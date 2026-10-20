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

/**
 * Integer AABB used by the road preview. The box already includes the lamp
 * reservation height.
 */
public record RoadBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static RoadBounds empty(final BlockPos pos) {
        return new RoadBounds(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static RoadBounds of(final RoadPlan plan, final int lampReserveHeight) {
        return of(plan, lampReserveHeight, true);
    }

    /** Strict road footprint: includes the temporary anchor but not the controller. */
    public static RoadBounds ofRoadOnly(final RoadPlan plan, final int lampReserveHeight) {
        return of(plan, lampReserveHeight, false);
    }

    /**
     * Pure segment footprint: no anchor and no controller padding. Used to
     * decide whether a cell really lives inside the paved road body instead of
     * inside a box stretched to reach an off-road anchor.
     */
    public static RoadBounds ofBody(final RoadPlan plan) {
        return of(plan, 0, false, false);
    }

    private static RoadBounds of(final RoadPlan plan, final int lampReserveHeight,
                                 final boolean includeController) {
        return of(plan, lampReserveHeight, includeController, true);
    }

    private static RoadBounds of(final RoadPlan plan, final int lampReserveHeight,
                                 final boolean includeController, final boolean includeAnchor) {
        if (plan.segments().isEmpty()) {
            return empty(plan.controllerPos());
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (final RoadSegment segment : plan.segments()) {
            final int x0 = Math.min(segment.start().getX(), segment.endX());
            final int x1 = Math.max(segment.start().getX(), segment.endX());
            final int z0 = Math.min(segment.start().getZ(), segment.endZ());
            final int z1 = Math.max(segment.start().getZ(), segment.endZ());
            final int y0 = segment.start().getY();
            final int y1 = segment.start().getY() + Math.max(0, segment.rise());

            // Width is perpendicular to the segment axis; side is +1 or -1.
            final int sideOffset = plan.widthSide() * (plan.width() - 1);
            final int w0x;
            final int w1x;
            final int w0z;
            final int w1z;
            if (segment.axis() == net.minecraft.core.Direction.Axis.X) {
                w0x = x0;
                w1x = x1;
                w0z = Math.min(segment.start().getZ(), segment.start().getZ() + sideOffset);
                w1z = Math.max(segment.start().getZ(), segment.start().getZ() + sideOffset);
            } else {
                w0z = z0;
                w1z = z1;
                w0x = Math.min(segment.start().getX(), segment.start().getX() + sideOffset);
                w1x = Math.max(segment.start().getX(), segment.start().getX() + sideOffset);
            }

            minX = Math.min(minX, w0x);
            maxX = Math.max(maxX, w1x);
            minZ = Math.min(minZ, w0z);
            maxZ = Math.max(maxZ, w1z);
            minY = Math.min(minY, y0);
            maxY = Math.max(maxY, y1);
        }

        // The anchor is the placement origin and is always included. The
        // controller is excluded for strict-width preview/blueprints.
        for (final BlockPos extra : includeAnchor
                ? (includeController
                        ? new BlockPos[]{plan.anchorPos(), plan.controllerPos()}
                        : new BlockPos[]{plan.anchorPos()})
                : new BlockPos[0]) {
            minX = Math.min(minX, extra.getX());
            maxX = Math.max(maxX, extra.getX());
            minY = Math.min(minY, extra.getY());
            minZ = Math.min(minZ, extra.getZ());
            maxZ = Math.max(maxZ, extra.getZ());
        }

        return new RoadBounds(minX, minY, minZ, maxX, maxY + Math.max(0, lampReserveHeight), maxZ);
    }

    public int sizeX() {
        return maxX - minX + 1;
    }

    public int sizeY() {
        return maxY - minY + 1;
    }

    public int sizeZ() {
        return maxZ - minZ + 1;
    }

    public boolean intersects(final RoadBounds other) {
        return minX <= other.maxX && maxX >= other.minX
                && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }
}
