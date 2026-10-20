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
package com.YLFXM.mcreconstruction.client;

import com.YLFXM.mcreconstruction.ModRegistry;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = com.YLFXM.mcreconstruction.MCReconstruction.MODID, value = Dist.CLIENT)
public final class ClientSelectionEvents {
    @SubscribeEvent
    public static void onLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (!event.getEntity().getMainHandItem().is(ModRegistry.ROAD_WAND.get())) {
            return;
        }
        // Shift+left selects B; plain left selects A.
        if (event.getEntity().isShiftKeyDown()) {
            RoadSelectionClient.setB(event.getPos());
            event.getEntity().displayClientMessage(Component.literal("Road endpoint B: "
                    + event.getPos().getX() + ", " + event.getPos().getY() + ", "
                    + event.getPos().getZ()), true);
        } else {
            RoadSelectionClient.setA(event.getPos());
            event.getEntity().displayClientMessage(Component.literal("Road endpoint A: "
                    + event.getPos().getX() + ", " + event.getPos().getY() + ", "
                    + event.getPos().getZ()), true);
        }
        event.setCanceled(true);
    }

    private ClientSelectionEvents() {
    }
}
