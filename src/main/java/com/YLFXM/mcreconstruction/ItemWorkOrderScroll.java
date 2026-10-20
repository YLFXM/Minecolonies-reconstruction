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

import com.YLFXM.mcreconstruction.network.Network;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Work order scroll. Like the MineColonies resource scroll it is bound to a
 * colony building -- here the town hall -- and from then on anyone holding it
 * can read that colony's work order list, member or not. Binding is done with
 * shift + right click on a town hall.
 */
public class ItemWorkOrderScroll extends Item {
    public ItemWorkOrderScroll(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player,
                                                  final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
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

        if (level.isClientSide || !(player instanceof ServerPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            if (level.getBlockEntity(clicked) instanceof AbstractTileEntityColonyBuilding buildingEntity
                    && buildingEntity.getBuilding() instanceof BuildingTownHall) {
                bind(stack, clicked);                player.displayClientMessage(Component.literal(
                        "Work order scroll bound to " + buildingEntity.getColony().getName()
                                + " town hall."), false);
            } else {
                player.displayClientMessage(Component.literal(
                        "Shift + right-click a town hall to bind this scroll."), false);
            }
            return InteractionResult.SUCCESS;
        }

        send((ServerPlayer) player, level, stack);
        return InteractionResult.SUCCESS;
    }

    private static void send(final ServerPlayer player, final Level level, final ItemStack stack) {
        final BlockPos townHall = TownHallBinding.get(stack);
        if (townHall == null) {
            // Open an empty list with a hint instead of silently doing nothing.
            Network.sendUnboundWorkOrders(player);
            return;
        }
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, townHall);
        if (colony == null) {
            player.displayClientMessage(Component.literal(
                    "The bound town hall no longer belongs to a colony."), false);
            Network.sendUnboundWorkOrders(player);
            return;
        }
        Network.sendWorkOrders(player, colony);
    }

    private static void bind(final ItemStack stack, final BlockPos pos) {
        TownHallBinding.set(stack, pos);
    }
}
