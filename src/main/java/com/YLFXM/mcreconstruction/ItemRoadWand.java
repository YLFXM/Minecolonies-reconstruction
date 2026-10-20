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

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Selection wand for road endpoints. Left click sets A; shift-right click sets
 * B on the client; right-click air opens the wand screen with a Build button.
 */
public class ItemRoadWand extends Item {
    public ItemRoadWand(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean canAttackBlock(final BlockState state, final Level level, final BlockPos pos, final Player player) {
        // The wand uses left-click to select endpoint A; never break blocks.
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            // Shift-right-click is reserved for endpoint B selection.
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            new com.YLFXM.mcreconstruction.client.WindowRoadWand().open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
