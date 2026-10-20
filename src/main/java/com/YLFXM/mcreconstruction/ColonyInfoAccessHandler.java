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

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.permissions.IPermissions;
import com.minecolonies.api.items.component.ColonyId;
import com.minecolonies.core.items.ItemClipboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Closes the one hole the original leaves open in "colony internals are for the
 * colony": the resource scroll ({@code minecolonies:clipboard}).
 *
 * <p>{@code ItemClipboard#use} / {@code #useOn} open
 * {@code WindowClipBoard} with <b>no permission check at all</b>, so anybody
 * holding a scroll bound to a colony can read its request list. Everything else
 * is already covered by the original: right-clicking a hut (town hall and the
 * builder's huts included) is denied without {@code ACCESS_HUTS} by
 * {@code ColonyPermissionEventHandler}, and opening the town hall GUI is denied
 * in {@code BlockHutTownHall} — which also makes the request list unreachable,
 * since it lives inside that GUI.</p>
 *
 * <p>The check has to run on <b>both sides</b>: the window is opened by the
 * client's own {@code Item#use}, so a server-only cancellation would let a
 * normal client show the window anyway. Cancelling the interact event on the
 * client stops {@code use} from ever being invoked.</p>
 *
 * <p><b>Limit, by design:</b> MineColonies pushes work orders to every
 * {@code closeSubscriber} — players who merely walk near a colony — so the data
 * is already in the client's memory. This handler closes the UI, it does not
 * remove the data. Truly hiding it would mean patching
 * {@code ColonyPackageManager}, i.e. a mixin, which this project does not do.</p>
 */
@EventBusSubscriber(modid = MCReconstruction.MODID)
public final class ColonyInfoAccessHandler {
    @SubscribeEvent
    public static void onRightClickItem(final PlayerInteractEvent.RightClickItem event) {
        guard(event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        guard(event);
    }

    /**
     * Denies using a bound resource scroll without {@code ACCESS_HUTS}.
     *
     * <p>An unbound scroll is left alone on purpose: binding it to a hut is the
     * vanilla way to create one, and binding itself is already gated because
     * right-clicking the hut requires {@code ACCESS_HUTS}.</p>
     */
    private static void guard(final PlayerInteractEvent event) {
        final ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ItemClipboard)) {
            return;
        }
        final Player player = event.getEntity();
        final IPermissions permissions = permissionsOf(event, stack);
        if (permissions == null) {
            // Not bound to a colony yet; vanilla prints its own "need colony".
            return;
        }
        if (permissions.hasPermission(player, Action.ACCESS_HUTS)) {
            return;
        }
        // The base PlayerInteractEvent does not expose setCanceled; both
        // concrete events do, through ICancellableEvent.
        ((ICancellableEvent) event).setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            RoadPermissions.notifyDenied(serverPlayer);
        }
    }

    /** Server colonies and client colony views expose the same permission interface. */
    private static IPermissions permissionsOf(final PlayerInteractEvent event, final ItemStack stack) {
        if (event.getLevel().isClientSide()) {
            final IColonyView view = ColonyId.readColonyViewFromItemStack(stack);
            return view == null ? null : view.getPermissions();
        }
        final IColony colony = ColonyId.readColonyFromItemStack(stack);
        return colony == null ? null : colony.getPermissions();
    }

    private ColonyInfoAccessHandler() {
    }
}
