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
package com.YLFXM.mcreconstruction;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.ISurvivalBlueprintHandler;
import com.ldtteam.structurize.storage.SurvivalBlueprintHandlers;
import com.ldtteam.structurize.util.BlockInfo;
import com.minecolonies.core.blocks.BlockDecorationController;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Survival handler for infrastructure/custom/road: places the anchor and
 * decoration controller immediately instead of creating a builder WorkOrder.
 */
public final class RoadAnchorSurvivalHandler implements ISurvivalBlueprintHandler {
    public static final String ID = "mcreconstruction:road_anchor";
    private static boolean registered;

    public static void register() {
        if (!registered) {
            registered = true;
            SurvivalBlueprintHandlers.registerHandler(new RoadAnchorSurvivalHandler());
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.literal("Place Road Anchor");
    }

    @Override
    public boolean canHandle(final Blueprint blueprint,
                             final ClientLevel level,
                             final Player player,
                             final BlockPos pos,
                             final RotationMirror rotationMirror) {
        return isOurAnchorBlueprint(blueprint);
    }

    private static boolean isOurAnchorBlueprint(final Blueprint blueprint) {
        // Matching only on BlockDecorationController would also catch every
        // vanilla decoration blueprint and every generated road blueprint.
        // Restrict this handler to the initial anchor blueprint we ship.
        return CustomRoadPack.PACK_NAME.equals(blueprint.getPackName())
                && "road".equals(blueprint.getFileName())
                && blueprint.getFilePath() != null
                && blueprint.getFilePath().toString().replace('\\', '/')
                        .endsWith("initial");
    }

    @Override
    public void handle(final Blueprint blueprint,
                       final String pack,
                       final String path,
                       final boolean clientPack,
                       final Level level,
                       final Player player,
                       final BlockPos pos,
                       final RotationMirror rotationMirror) {
        if (!(level instanceof ServerLevel serverLevel) || !isOurAnchorBlueprint(blueprint)) {
            return;
        }

        // Placing the anchor is the first step of building a road, so it needs
        // the same authority as planning one.
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        if (!RoadPermissions.canManage(serverLevel, serverPlayer, pos)) {
            // Silent on purpose: the colony permission system reports the denial.
            MCReconstruction.LOGGER.debug("Denied anchor placement for {} at {} (no manage-huts)",
                    player.getName().getString(), pos);
            return;
        }

        final BlockPos origin = pos.subtract(blueprint.getPrimaryBlockOffset());
        for (final BlockInfo info : blueprint.getBlockInfoAsList()) {
            final BlockState state = info.getState();
            if (state.isAir()) {
                continue;
            }
            final BlockPos world = origin.offset(info.getPos());
            serverLevel.setBlock(world, state, 3);
            if (info.hasTileEntityData() && info.getTileEntityData() != null) {
                final BlockEntity be = serverLevel.getBlockEntity(world);
                if (be != null) {
                    final CompoundTag tag = info.getTileEntityData().copy();
                    tag.putInt("x", world.getX());
                    tag.putInt("y", world.getY());
                    tag.putInt("z", world.getZ());
                    be.loadWithComponents(tag, serverLevel.registryAccess());
                    be.setChanged();
                }
            }
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Road anchor placed. Right-click the decoration controller to plan the road."), false);
    }

    private RoadAnchorSurvivalHandler() {
    }
}
