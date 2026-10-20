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
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

/**
 * One unit of road along one horizontal axis. Length is measured in blocks
 * along {@link #axis}; a stair unit is always one block long and rises at most
 * one block.
 */
public record RoadSegment(BlockPos start, int length, Direction.Axis axis, int rise, SegmentKind kind) {
    public RoadSegment {
        if (length < 1) {
            throw new IllegalArgumentException("length must be >= 1");
        }
        if (axis != Direction.Axis.X && axis != Direction.Axis.Z) {
            throw new IllegalArgumentException("road segments must be horizontal");
        }
        if (rise < -1 || rise > 1) {
            throw new IllegalArgumentException("rise must be -1, 0 or 1");
        }
        if (kind != SegmentKind.ROAD && length != 1) {
            throw new IllegalArgumentException("stair units must be one block long");
        }
    }

    public int endX() {
        return axis == Direction.Axis.X ? start.getX() + length - 1 : start.getX();
    }

    public int endZ() {
        return axis == Direction.Axis.Z ? start.getZ() + length - 1 : start.getZ();
    }

    public BlockPos end() {
        return new BlockPos(endX(), start.getY() + rise, endZ());
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.put("start", NbtUtils.writeBlockPos(start));
        tag.putInt("length", length);
        tag.putString("axis", axis == Direction.Axis.X ? "x" : "z");
        tag.putInt("rise", rise);
        tag.putString("kind", kind.name());
        return tag;
    }

    public static RoadSegment load(final CompoundTag tag) {
        final BlockPos start = NbtUtils.readBlockPos(tag, "start").orElse(BlockPos.ZERO);
        final int length = Math.max(1, tag.getInt("length"));
        final Direction.Axis axis = "x".equalsIgnoreCase(tag.getString("axis")) ? Direction.Axis.X : Direction.Axis.Z;
        final int rise = Math.max(-1, Math.min(1, tag.getInt("rise")));
        final SegmentKind kind;
        try {
            kind = SegmentKind.valueOf(tag.getString("kind"));
        } catch (final IllegalArgumentException ex) {
            return new RoadSegment(start, length, axis, 0, SegmentKind.ROAD);
        }
        return new RoadSegment(start, length, axis, rise, kind);
    }
}
