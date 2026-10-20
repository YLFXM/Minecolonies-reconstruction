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

import com.YLFXM.mcreconstruction.network.ClientRoadCache;
import com.YLFXM.mcreconstruction.network.Network;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Road overview item. Like the Work Order Scroll it is bound to a colony town
 * hall (shift + right-click), and only lists that colony's roads. Unbound it
 * opens an empty panel with a binding hint.
 */
public class ItemRoadSurveyor extends Item {
    public ItemRoadSurveyor(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player,
                                                  final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // No server round trip when unbound: just clear the cache so the
            // panel shows the hint instead of stale roads from an earlier look.
            if (TownHallBinding.get(stack) == null) {
                ClientRoadCache.clear();
            }
            new com.YLFXM.mcreconstruction.client.WindowRoadSurveyor().open();
        } else if (player instanceof ServerPlayer serverPlayer) {
            send(serverPlayer, level, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final Player player = context.getPlayer();
        final ItemStack stack = context.getItemInHand();
        final BlockPos clicked = context.getClickedPos();

        if (level.isClientSide) {
            // Shift is handled entirely by the server (binding); a plain
            // right-click on a block behaves like right-clicking air.
            if (!player.isShiftKeyDown()) {
                if (TownHallBinding.get(stack) == null) {
                    ClientRoadCache.clear();
                }
                new com.YLFXM.mcreconstruction.client.WindowRoadSurveyor().open();
            }
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            if (level.getBlockEntity(clicked) instanceof AbstractTileEntityColonyBuilding buildingEntity
                    && buildingEntity.getBuilding() instanceof BuildingTownHall) {
                TownHallBinding.set(stack, clicked);
                player.displayClientMessage(Component.literal(
                        "Road surveyor bound to " + buildingEntity.getColony().getName()
                                + " town hall."), false);
                // Refresh immediately so the next open shows the bound colony.
                Network.sendRoads(serverPlayer, buildingEntity.getColony());
            } else {
                player.displayClientMessage(Component.literal(
                        "Shift + right-click a town hall to bind this surveyor."), false);
            }
            return InteractionResult.SUCCESS;
        }

        send(serverPlayer, level, stack);
        return InteractionResult.SUCCESS;
    }

    private static void send(final ServerPlayer player, final Level level, final ItemStack stack) {
        final BlockPos townHall = TownHallBinding.get(stack);
        if (townHall == null) {
            // Unbound: the client shows the hint; do not push any roads.
            return;
        }
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, townHall);
        if (colony == null) {
            player.displayClientMessage(Component.literal(
                    "The bound town hall no longer belongs to a colony."), false);
            return;
        }
        Network.sendRoads(player, colony);
    }
}
