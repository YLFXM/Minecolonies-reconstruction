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

import com.minecolonies.core.blocks.BlockDecorationController;
import com.YLFXM.mcreconstruction.network.Network;
import com.YLFXM.mcreconstruction.network.OpenRoadPlanPayload;
import com.YLFXM.mcreconstruction.road.RoadManager;
import com.YLFXM.mcreconstruction.road.RoadPlan;
import com.YLFXM.mcreconstruction.road.RoadWandBinding;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Opens the road plan from a native MineColonies decoration controller that has
 * a RoadManager entry, instead of opening the vanilla decoration GUI.
 */
@EventBusSubscriber(modid = MCReconstruction.MODID)
public final class DecorationControllerHandler {
    private static final java.util.Map<java.util.UUID, Long> LAST_INTERACTION =
            new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getBlockState(event.getPos()).getBlock() instanceof BlockDecorationController)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Some interaction paths fire the event twice; suppress the duplicate
        // so binding/open messages are not emitted twice.
        final long now = System.currentTimeMillis();
        final Long last = LAST_INTERACTION.put(player.getUUID(), now);
        if (last != null && now - last < 400L) {
            return;
        }

        RoadPlan plan = RoadManager.get(level).findByController(event.getPos());
        if (plan != null && plan.anchorPos().equals(plan.controllerPos())) {
            final net.minecraft.core.BlockPos resolved = findAnchor(level, event.getPos());
            if (resolved != null) {
                plan.setAnchorPos(resolved);
                RoadManager.get(level).add(plan);
            }
        }
        if (plan == null) {
            final net.minecraft.core.BlockPos anchorPos = findAnchor(level, event.getPos());
            if (anchorPos == null) {
                return;
            }
            // Creating a road plan is a build request: outsiders must not be able
            // to register one inside someone else's colony. No message here on
            // purpose -- leaving the event uncancelled lets MineColonies run its
            // own decoration controller handler, which reports the permission
            // denial to the player in the native way.
            if (!RoadPermissions.canManage(level, player, event.getPos())) {
                MCReconstruction.LOGGER.debug("Denied road plan creation for {} at {} (no manage-huts)",
                        player.getName().getString(), event.getPos());
                return;
            }
            plan = new RoadPlan(java.util.UUID.randomUUID(), level.dimension(), anchorPos, event.getPos(),
                    Direction.NORTH, false, CustomRoadPack.PACK_NAME, "street",
                    4, 2, 1, Config.lampSpacing());
            plan.setState(com.YLFXM.mcreconstruction.road.RoadState.DRAFT);
            plan.setDisplayName(com.YLFXM.mcreconstruction.road.RoadNames.generate(level, anchorPos, player));
            // Ownership decides who may see a wilderness road, so it is recorded
            // at creation time and never changed afterwards.
            plan.setCreator(player.getUUID());
            RoadManager.get(level).add(plan);
        }

        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        event.setCanceled(true);

        // Reading a road is colony internals, so both gestures need
        // ACCESS_HUTS: binding leaks the road's name, and the panel shows its
        // geometry. Acting on it still needs MANAGE_HUTS inside the handlers.
        if (!RoadPermissions.canView(level, player, plan.controllerPos())) {
            RoadPermissions.notifyDenied(player);
            return;
        }

        final boolean hasWand = hasRoadWand(player);
        if (player.isShiftKeyDown() && hasWand) {
            // Shift-right-click is the explicit bind gesture. Do not open any
            // controller or Wand window.
            final ItemStack wand = findWand(player);
            if (!wand.isEmpty()) {
                final var previous = RoadWandBinding.getBoundController(wand);
                RoadWandBinding.bind(wand, plan.controllerPos());
                if (wand == player.getMainHandItem()) {
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
                } else if (wand == player.getOffhandItem()) {
                    player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, wand);
                }
                player.getPersistentData().putLong("mcreconstruction:road_wand_controller",
                        plan.controllerPos().asLong());
                player.getInventory().setChanged();
                if (player.containerMenu != null) {
                    player.containerMenu.broadcastChanges();
                }
                Network.sendRoads(player, com.minecolonies.api.colony.IColonyManager.getInstance()
                        .getColonyByPosFromWorld(level, plan.controllerPos()));
                PacketDistributor.sendToPlayer(player,
                        new com.YLFXM.mcreconstruction.network.BindRoadWandPayload(plan.controllerPos()));
                final boolean already = plan.controllerPos().equals(previous);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(already
                        ? "Road Wand is already bound to " + com.YLFXM.mcreconstruction.road.RoadNames.label(plan)
                        : "Road Wand bound to " + com.YLFXM.mcreconstruction.road.RoadNames.label(plan)), false);
            }
            return;
        }

        // Plain right-click opens the controller management panel, even while a
        // Wand is in the inventory. Binding uses the explicit shift gesture.
        Network.sendRoads(player, com.minecolonies.api.colony.IColonyManager.getInstance()
                .getColonyByPosFromWorld(level, plan.controllerPos()));
        PacketDistributor.sendToPlayer(player, new OpenRoadPlanPayload(plan.controllerPos()));
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Road plan " + com.YLFXM.mcreconstruction.road.RoadNames.label(plan)
                        + " opened."), false);
    }

    private static boolean hasRoadWand(final ServerPlayer player) {
        if (player.getMainHandItem().is(ModRegistry.ROAD_WAND.get())
                || player.getOffhandItem().is(ModRegistry.ROAD_WAND.get())) {
            return true;
        }
        for (final ItemStack stack : player.getInventory().items) {
            if (stack.is(ModRegistry.ROAD_WAND.get())) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack findWand(final ServerPlayer player) {
        if (player.getMainHandItem().is(ModRegistry.ROAD_WAND.get())) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(ModRegistry.ROAD_WAND.get())) {
            return player.getOffhandItem();
        }
        for (final ItemStack stack : player.getInventory().items) {
            if (stack.is(ModRegistry.ROAD_WAND.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * The native controller's FACING points at its support block. That support
     * is the anchor for our blueprint, so this is deterministic. A unique
     * neighbor scan is only a legacy fallback.
     */
    private static net.minecraft.core.BlockPos findAnchor(final ServerLevel level,
                                                          final net.minecraft.core.BlockPos controllerPos) {
        final var anchorId = ResourceLocation.tryParse(Config.roadAnchorBlock());
        if (anchorId == null) {
            return null;
        }

        final var controllerState = level.getBlockState(controllerPos);
        if (controllerState.hasProperty(DirectionalBlock.FACING)) {
            final Direction support = controllerState.getValue(DirectionalBlock.FACING);
            final net.minecraft.core.BlockPos candidate = controllerPos.relative(support);
            final var placedId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(
                    level.getBlockState(candidate).getBlock());
            if (anchorId.equals(placedId)) {
                return candidate;
            }
        }

        net.minecraft.core.BlockPos found = null;
        int matches = 0;
        for (final Direction direction : Direction.values()) {
            final net.minecraft.core.BlockPos candidate = controllerPos.relative(direction);
            final var placedId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(
                    level.getBlockState(candidate).getBlock());
            if (anchorId.equals(placedId)) {
                found = candidate;
                matches++;
            }
        }
        return matches == 1 ? found : null;
    }

    private DecorationControllerHandler() {
    }
}
