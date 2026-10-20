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
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Shared "this item is bound to a colony town hall" storage.
 *
 * <p>Both the Work Order Scroll and the Road Surveyor are bound the same way:
 * shift + right-click a town hall. Only the position is stored; the colony is
 * resolved from it on demand, so a destroyed/removed town hall simply becomes
 * unbound instead of pointing at stale data.</p>
 */
public final class TownHallBinding {
    private static final String TAG_TOWN_HALL = "mcreconstruction:bound_townhall";

    /** Bound town hall position, or {@code null} when the stack is unbound. */
    public static BlockPos get(final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        final CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_TOWN_HALL)) {
            return null;
        }
        final BlockPos pos = BlockPos.of(tag.getLong(TAG_TOWN_HALL));
        return pos.equals(BlockPos.ZERO) ? null : pos;
    }

    public static void set(final ItemStack stack, final BlockPos pos) {
        final CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        stack.set(DataComponents.CUSTOM_DATA, data.update(tag -> tag.putLong(TAG_TOWN_HALL, pos.asLong())));
    }

    public static void clear(final ItemStack stack) {
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }
        stack.set(DataComponents.CUSTOM_DATA, data.update(tag -> tag.remove(TAG_TOWN_HALL)));
    }

    private TownHallBinding() {
    }
}
