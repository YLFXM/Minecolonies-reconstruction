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

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A one-block-long slice extracted from a style-pack blueprint. All positions
 * are local to the slice; the controller block is removed from the template.
 */
public final class UnitTemplate {
    public record Entry(BlockPos pos, BlockState state, CompoundTag tileEntity) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final int walkWidth;
    private final int surfaceY;
    private final Direction.Axis axis;

    public UnitTemplate(final int sizeX, final int sizeY, final int sizeZ, final int walkWidth,
                        final int surfaceY, final Direction.Axis axis) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.walkWidth = walkWidth;
        this.surfaceY = surfaceY;
        this.axis = axis;
    }

    public void add(final BlockPos pos, final BlockState state, final CompoundTag tileEntity) {
        entries.add(new Entry(pos, state, tileEntity));
    }

    public List<Entry> entries() {
        return entries;
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public int walkWidth() {
        return walkWidth;
    }

    public int surfaceY() {
        return surfaceY;
    }

    public Direction.Axis axis() {
        return axis;
    }
}
