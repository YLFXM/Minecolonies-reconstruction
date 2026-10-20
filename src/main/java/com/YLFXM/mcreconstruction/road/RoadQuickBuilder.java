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
import com.ldtteam.structurize.util.BlockUtils;
import com.YLFXM.mcreconstruction.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creative-mode instant road placement. Mirrors the build tool's Complete and
 * Pretty modes: Complete writes the blueprint exactly, Pretty clears the whole
 * target volume first and then writes it.
 */
public final class RoadQuickBuilder {
    public static void place(final ServerLevel level, final RoadPlan plan, final Blueprint blueprint,
                             final boolean pretty) {
        final BlockPos origin = plan.anchorPos().subtract(blueprint.getPrimaryBlockOffset());
        final BlockState filler = RoadUnitExtractor.blockState(Config.roadFillBlock());

        // Do NOT clear the whole AABB here. Only explicit air cells in the
        // blueprint may clear space; otherwise an off-road controller would
        // carve a hole below itself.

        for (final BlockInfo info : blueprint.getBlockInfoAsList()) {
            BlockState state = info.getState();
            final BlockPos world = origin.offset(info.getPos());
            final ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (blockId != null && "structurize".equals(blockId.getNamespace())
                    && ("blocksubstitution".equals(blockId.getPath())
                    || "blockfluidsubstitution".equals(blockId.getPath())
                    || "blocktagsubstitution".equals(blockId.getPath()))) {
                // Leave unspecified/placeholder cells untouched.
                continue;
            }
            if (blockId != null && "structurize".equals(blockId.getNamespace())
                    && "blocksolidsubstitution".equals(blockId.getPath())) {
                // Use Structurize's own substitution choice (worldgen/existing
                // solid, dirt fallback) instead of hardcoding stone.
                state = BlockUtils.getSubstitutionBlockAtWorld(level, world,
                        p -> level.getBlockState(p));
                if (state.isAir()) {
                    state = filler;
                }
            }

            level.setBlock(world, state, 3);

            if (!state.isAir() && info.hasTileEntityData() && info.getTileEntityData() != null) {
                final BlockEntity blockEntity = level.getBlockEntity(world);
                if (blockEntity != null) {
                    final CompoundTag tag = info.getTileEntityData().copy();
                    tag.putInt("x", world.getX());
                    tag.putInt("y", world.getY());
                    tag.putInt("z", world.getZ());
                    blockEntity.loadWithComponents(tag, level.registryAccess());
                    blockEntity.setChanged();
                }
            }
        }

        plan.setState(RoadState.BUILT);
    }

    private RoadQuickBuilder() {
    }
}
