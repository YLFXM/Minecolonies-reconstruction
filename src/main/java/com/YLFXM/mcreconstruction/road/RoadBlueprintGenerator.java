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

import com.ldtteam.structurize.blocks.ModBlocks;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.core.blocks.BlockDecorationController;
import com.YLFXM.mcreconstruction.Config;
import com.YLFXM.mcreconstruction.MCReconstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds an in-memory blueprint from a RoadPlan and a one-block slice template.
 * The blueprint is regenerated after a server restart from the controller's
 * RoadPlan; it is never written to a structure pack.
 */
public final class RoadBlueprintGenerator {
    private static final RoadUnit DEFAULT_UNIT = new RoadUnit(
            "minecolonies/street",
            RoadUnit.UnitType.ROAD,
            "Minecolonies Original",
            "infrastructure/roads/streets",
            "infrastructure/roads/streets/longroad1.blueprint",
            Direction.Axis.X,
            0,
            4,
            2,
            1,
            0,
            1);

    public static Blueprint generate(final RoadPlan plan, final ServerLevel level, final int lampReserveHeight) {
        final RoadUnit unit = resolveUnit(plan);
        final var packMeta = StructurePacks.getStructurePack(unit.pack());
        final Blueprint source = StructurePacks.getBlueprint(unit.pack(), unit.source(), level.registryAccess());
        if (source == null) {
            MCReconstruction.LOGGER.error("Road source blueprint not found: pack='{}' path='{}' packMeta={} packPath={}",
                    unit.pack(), unit.source(), packMeta != null, packMeta == null ? "null" : packMeta.getPath());
            return null;
        }

        final UnitTemplate template = RoadUnitExtractor.extract(source, unit);
        if (template == null) {
            return null;
        }

        final BlockPos anchorPos = resolveAnchor(level, plan);
        final RoadBounds bounds = RoadBounds.ofRoadOnly(plan, lampReserveHeight);
        final int sizeX = bounds.sizeX();
        // The blueprint origin is the road surface minus surfaceY so the
        // template's foundation is preserved and the visible surface stays at
        // the road's Y level. The anchor/controller may be one block above it.
        final int originY = bounds.minY() - template.surfaceY();
        final int sizeY = bounds.maxY() - originY + 1;
        final int sizeZ = bounds.sizeZ();
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return null;
        }

        final Blueprint out = new Blueprint((short) sizeX, (short) sizeY, (short) sizeZ, level.registryAccess());

        // Keep the Structurize solid-substitution placeholder for support fill.
        // The native builder/build-tool resolves it using the colony/build-tool
        // fill settings instead of a hardcoded block.
        final BlockState filler = ModBlocks.blockSolidSubstitution.get().defaultBlockState();

        // Clear only the reserved volume above the road footprint. Filling the
        // whole AABB with air would carve holes around an off-road controller.
        final BlockState air = Blocks.AIR.defaultBlockState();
        for (final RoadSegment segment : plan.segments()) {
            final int surfaceY = segment.start().getY();
            for (int y = surfaceY + 1; y <= bounds.maxY(); y++) {
                fillSegmentSlice(out, plan, bounds, originY, segment, y, air);
            }
        }

        // Fill the supporting volume below every higher road cell. Flat roads
        // have no extra blocks here.
        for (final RoadSegment segment : plan.segments()) {
            final int surfaceY = segment.start().getY();
            for (int y = bounds.minY(); y < surfaceY; y++) {
                fillSegmentSlice(out, plan, bounds, originY, segment, y, filler);
            }
        }

        // Place the actual road template blocks.
        for (final RoadSegment segment : plan.segments()) {
            final int length = segment.length();
            for (int i = 0; i < length; i++) {
                final int baseX = segment.axis() == Direction.Axis.X ? segment.start().getX() + i : segment.start().getX();
                final int baseZ = segment.axis() == Direction.Axis.Z ? segment.start().getZ() + i : segment.start().getZ();
                final int baseY = segment.start().getY();

                for (final UnitTemplate.Entry entry : template.entries()) {
                    final BlockPos local = entry.pos();
                    final int worldX;
                    final int worldZ;
                    final int side = plan.widthSide() >= 0 ? 1 : -1;
                    if (segment.axis() == template.axis()) {
                        if (segment.axis() == Direction.Axis.X) {
                            worldX = baseX;
                            worldZ = baseZ + side * local.getZ();
                        } else {
                            worldZ = baseZ;
                            worldX = baseX + side * local.getX();
                        }
                    } else if (template.axis() == Direction.Axis.X) {
                        // Rotate the X-authored slice so its width runs along Z.
                        worldZ = baseZ;
                        worldX = baseX + side * local.getZ();
                    } else {
                        // Rotate the Z-authored slice so its width runs along X.
                        worldX = baseX;
                        worldZ = baseZ + side * local.getX();
                    }
                    // The source slice has its road surface at local surfaceY;
                    // shift it down so the built surface is exactly A's Y.
                    final int worldY = baseY + local.getY() - template.surfaceY();
                    put(out, bounds, originY, new BlockPos(worldX, worldY, worldZ),
                            entry.state(), entry.tileEntity());
                }
            }
        }

        // Reserve the inner controller cell with a Structurize placeholder, not
        // air. The builder ignores placeholders, so it will neither place a
        // road block nor dig out the future controller cell.
        final BlockPos controllerReserve = RoadControllerMigration.target(plan);
        put(out, bounds, originY, controllerReserve,
                com.ldtteam.structurize.blocks.ModBlocks.blockSubstitution.get().defaultBlockState(), null);

        placeAnchor(out, bounds, originY, anchorPos);

        out.setPackName(unit.pack());
        out.setFilePath(java.nio.file.Path.of(unit.source()));
        out.setFileName(RoadNames.fileName(plan));
        // The work order anchor is the controller position. Without this the
        // Blueprint falls back to the centre of the box, which turns the
        // selected edge point into the middle of the road and shifts the
        // surface by surfaceY.
        out.setCachePrimaryOffset(new BlockPos(
                anchorPos.getX() - bounds.minX(),
                anchorPos.getY() - originY,
                anchorPos.getZ() - bounds.minZ()));
        return out;
    }

    private static void fillSegmentSlice(final Blueprint out,
                                         final RoadPlan plan,
                                         final RoadBounds bounds,
                                         final int originY,
                                         final RoadSegment segment,
                                         final int y,
                                         final BlockState filler) {
        final int length = segment.length();
        for (int i = 0; i < length; i++) {
            final int baseX = segment.axis() == Direction.Axis.X ? segment.start().getX() + i : segment.start().getX();
            final int baseZ = segment.axis() == Direction.Axis.Z ? segment.start().getZ() + i : segment.start().getZ();
            for (int w = 0; w < plan.width(); w++) {
                final int worldX;
                final int worldZ;
                if (segment.axis() == Direction.Axis.X) {
                    worldX = baseX;
                    worldZ = plan.widthSide() >= 0 ? baseZ + w : baseZ - w;
                } else {
                    worldZ = baseZ;
                    worldX = plan.widthSide() >= 0 ? baseX + w : baseX - w;
                }
                put(out, bounds, originY, new BlockPos(worldX, y, worldZ), filler, null);
            }
        }
    }

    private static void put(final Blueprint out,
                            final RoadBounds bounds,
                            final int originY,
                            final BlockPos world,
                            final BlockState state,
                            final CompoundTag tileEntity) {
        final int x = world.getX() - bounds.minX();
        final int y = world.getY() - originY;
        final int z = world.getZ() - bounds.minZ();
        if (x < 0 || y < 0 || z < 0 || x >= out.getSizeX() || y >= out.getSizeY() || z >= out.getSizeZ()) {
            return;
        }

        out.addBlockState(new BlockPos(x, y, z), state);
        if (tileEntity != null) {
            final CompoundTag te = tileEntity.copy();
            te.putInt("x", x);
            te.putInt("y", y);
            te.putInt("z", z);
            out.getTileEntities()[y][z][x] = te;
        }
    }

    private static BlockPos resolveAnchor(final ServerLevel level, final RoadPlan plan) {
        final BlockPos stored = plan.anchorPos();
        if (!stored.equals(plan.controllerPos())) {
            return stored;
        }
        final ResourceLocation anchorId = ResourceLocation.tryParse(Config.roadAnchorBlock());
        if (anchorId == null) {
            return stored;
        }

        final BlockState controllerState = level.getBlockState(plan.controllerPos());
        if (controllerState.hasProperty(DirectionalBlock.FACING)) {
            final Direction support = controllerState.getValue(DirectionalBlock.FACING);
            final BlockPos candidate = plan.controllerPos().relative(support);
            final ResourceLocation placed = BuiltInRegistries.BLOCK.getKey(
                    level.getBlockState(candidate).getBlock());
            if (anchorId.equals(placed)) {
                plan.setAnchorPos(candidate);
                RoadManager.get(level).add(plan);
                return candidate;
            }
        }

        BlockPos found = null;
        int matches = 0;
        for (final Direction direction : Direction.values()) {
            final BlockPos candidate = plan.controllerPos().relative(direction);
            final ResourceLocation placed = BuiltInRegistries.BLOCK.getKey(
                    level.getBlockState(candidate).getBlock());
            if (anchorId.equals(placed)) {
                found = candidate;
                matches++;
            }
        }
        if (matches == 1) {
            plan.setAnchorPos(found);
            RoadManager.get(level).add(plan);
            return found;
        }
        return stored;
    }

    private static void placeAnchor(final Blueprint out, final RoadBounds bounds,
                                    final int originY, final BlockPos anchorPos) {
        final BlockState anchor = RoadUnitExtractor.blockState(Config.roadAnchorBlock());
        put(out, bounds, originY, anchorPos, anchor, null);
    }

    private static RoadUnit resolveUnit(final RoadPlan plan) {
        final RoadUnit registered = RoadUnit.Registry.get(plan.pack() + "/" + plan.variant());
        return registered == null ? DEFAULT_UNIT : registered;
    }

    private RoadBlueprintGenerator() {
    }
}
