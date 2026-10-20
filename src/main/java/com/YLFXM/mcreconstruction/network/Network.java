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
package com.YLFXM.mcreconstruction.network;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.permissions.Action;
import com.YLFXM.mcreconstruction.Config;
import com.YLFXM.mcreconstruction.MCReconstruction;
import com.YLFXM.mcreconstruction.ModRegistry;
import com.YLFXM.mcreconstruction.RoadWorkOrderRecovery;
import com.YLFXM.mcreconstruction.road.RoadBlueprintGenerator;
import com.YLFXM.mcreconstruction.road.RoadBlueprintStore;
import com.YLFXM.mcreconstruction.road.RoadBuildWatcher;
import com.YLFXM.mcreconstruction.road.RoadDataCleaner;
import com.YLFXM.mcreconstruction.road.RoadManager;
import com.YLFXM.mcreconstruction.road.RoadRepair;
import com.YLFXM.mcreconstruction.road.RoadNames;
import com.YLFXM.mcreconstruction.road.RoadPlacementMode;
import com.YLFXM.mcreconstruction.road.RoadPlan;
import com.YLFXM.mcreconstruction.road.RoadPlanBuilder;
import com.YLFXM.mcreconstruction.road.RoadQuickBuilder;
import com.YLFXM.mcreconstruction.road.RoadSegment;
import com.YLFXM.mcreconstruction.road.RoadState;
import com.YLFXM.mcreconstruction.road.RoadTemporaryPack;
import com.YLFXM.mcreconstruction.road.RoadWorkOrderService;
import com.YLFXM.mcreconstruction.road.SegmentKind;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class Network {
    public static void register(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("1");
        registrar.playToServer(RequestRoadsPayload.TYPE, RequestRoadsPayload.CODEC, Network::handleRequest);
        registrar.playToServer(ConfigureRoadPayload.TYPE, ConfigureRoadPayload.CODEC, Network::handleConfigure);
        registrar.playToServer(RoadBulkActionPayload.TYPE, RoadBulkActionPayload.CODEC, Network::handleBulkAction);
        registrar.playToServer(RoadControllerActionPayload.TYPE, RoadControllerActionPayload.CODEC,
                Network::handleControllerAction);
        registrar.playToServer(NativeBuildClosedPayload.TYPE, NativeBuildClosedPayload.CODEC,
                Network::handleNativeBuildClosed);
        registrar.playToServer(CancelWorkOrdersPayload.TYPE, CancelWorkOrdersPayload.CODEC,
                Network::handleCancelWorkOrders);
        registrar.playToClient(RoadSyncPayload.TYPE, RoadSyncPayload.CODEC, Network::handleSync);
        registrar.playToClient(OpenRoadPlanPayload.TYPE, OpenRoadPlanPayload.CODEC, Network::handleOpenPlan);
        registrar.playToClient(BindRoadWandPayload.TYPE, BindRoadWandPayload.CODEC, Network::handleBindWand);
        registrar.playToClient(OpenBuildToolPayload.TYPE, OpenBuildToolPayload.CODEC, Network::handleOpenBuildTool);
        registrar.playToClient(WorkOrderListPayload.TYPE, WorkOrderListPayload.CODEC, Network::handleWorkOrderList);
    }

    private static void handleConfigure(final ConfigureRoadPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            final ServerLevel level = player.serverLevel();
            final BlockPos controllerPos = payload.controllerPos();
            // No player-to-controller distance gate: A/B are picked from both
            // ends of the road, so the player is legitimately far from the
            // controller on anything longer than a few blocks. The plan is
            // already authorized by the wand binding below, and the segment is
            // bounded by Config.maxRoadLength.

            final RoadPlacementMode mode = payload.mode();
            final boolean creativeInstant = mode != RoadPlacementMode.SURVIVAL
                    && mode != RoadPlacementMode.PLAN
                    && Config.creativeInstantBuild() && player.isCreative();
            // Permission is about authority, not cost. RoadPermissions mirrors
            // MineColonies' own check. No chat message on denial: the colony
            // permission system already reports it to the player.
            final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, controllerPos);
            if (!com.YLFXM.mcreconstruction.RoadPermissions.canManage(player, colony)) {
                MCReconstruction.LOGGER.debug("Denied road configure for {} at {} (no manage-huts)",
                        player.getName().getString(), controllerPos);
                return;
            }
            if (mode == RoadPlacementMode.SURVIVAL && colony == null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "No colony covers this road; a builder order needs a colony."), false);
                return;
            }

            final RoadPlan old = RoadManager.get(level).findByController(controllerPos);
            if (old == null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "No road plan is bound to this controller. Right-click it with the Road Wand first."), false);
                return;
            }
            if (findBoundWand(player, controllerPos) == null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Road Wand is not bound to this controller."), false);
                return;
            }
            if (old.state() != RoadState.DRAFT && old.state() != RoadState.PLANNED) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "This road is already built; construction options are locked."), false);
                return;
            }
            // Road width is determined by the style-pack blueprint, not by a
            // free-form UI value. Reuse the plan's width.
            final int width = Math.max(1, old.width());
            final int lampSpacing = Math.max(0, old.lampSpacing());

            final Direction facing = old != null ? old.controllerFacing()
                    : (payload.hasEndpoints() ? directionBetween(payload.a(), payload.b()) : Direction.NORTH);
            final BlockPos anchorPos = old != null ? old.anchorPos() : controllerPos;
            final String pack = old != null ? old.pack() : "Minecolonies Original";
            final String variant = old != null ? old.variant() : "street";
            final int widthSide = old != null ? old.widthSide() : 1;
            final boolean mirror = payload.mirror();
            final int effectiveWidthSide = mirror ? -Math.abs(widthSide) : Math.abs(widthSide);

            final RoadPlan plan;
            final int intersections;
            try {
                if (payload.hasEndpoints()) {
                    final RoadPlanBuilder.Result result = RoadPlanBuilder.build(level, anchorPos,
                            payload.controllerPos(), facing, payload.a(), payload.b(), pack, variant,
                            width, Math.min(2, width), effectiveWidthSide, lampSpacing);
                    plan = result.plan();
                    plan.setMirror(mirror);
                    intersections = result.intersections().size();
                } else {
                    final int length = Math.max(1, Math.min(payload.length(), Config.maxRoadLength()));
                    final Direction.Axis axis = facing.getAxis().isHorizontal() ? facing.getAxis() : Direction.Axis.X;
                    plan = new RoadPlan(UUID.randomUUID(), level.dimension(), anchorPos,
                            payload.controllerPos(), facing, mirror, pack, variant,
                            width, Math.min(2, width), effectiveWidthSide, lampSpacing);
                    plan.addSegment(new RoadSegment(payload.controllerPos(), length, axis, 0, SegmentKind.ROAD));
                    plan.setState(RoadState.PLANNED);
                    intersections = 0;
                }
            } catch (final IllegalArgumentException ex) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Road endpoints invalid: " + ex.getMessage()), false);
                return;
            }

            // Keep the road identity stable across planning/building so Wand
            // bindings and blueprint names do not change after construction.
            plan.setId(old.id());
            // Ownership must survive re-planning: re-selecting A/B on a road
            // somebody else planned must not transfer it to the current player.
            plan.setCreator(old.creator() != null ? old.creator() : player.getUUID());
            if (!old.displayName().isBlank()) {
                plan.setDisplayName(old.displayName());
            }
            if (plan.displayName().isEmpty()) {
                plan.setDisplayName(RoadNames.generate(level, anchorPos, player));
            }

            final Blueprint blueprint = RoadBlueprintGenerator.generate(plan, level,
                    Config.defaultLampReserveHeight());
            if (blueprint == null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Road blueprint generation failed."), false);
                return;
            }
            if (old != null) {
                // Re-selecting A/B keeps this plan's UUID and display name, so
                // the generated file name is stable and gets overwritten in
                // place. Anything queued for the previous geometry must be
                // dropped first, otherwise a stale order would build the new
                // shape at the old anchor.
                RoadDataCleaner.releaseWorkOrders(old);
                RoadDataCleaner.releaseWorkOrders(plan);
            }
            final long packVersion = RoadTemporaryPack.replace(level, old, plan, blueprint);

            if (old != null) {
                RoadManager.get(level).remove(old.id());
            }

            if (mode == RoadPlacementMode.PLAN) {
                RoadBlueprintStore.save(level, plan, blueprint);
                RoadManager.get(level).add(plan);
                RoadBuildWatcher.watch(level, plan);
                PacketDistributor.sendToPlayer(player, new OpenBuildToolPayload(
                        plan.id(), RoadTemporaryPack.pathFor(plan),
                        com.ldtteam.structurize.blueprints.v1.BlueprintUtil.writeBlueprintToNBT(blueprint),
                        packVersion));
                sendRoads(player, colony);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Road " + RoadNames.label(plan)
                                + " planned. Choose Complete/Pretty/Survival in the native build tool."), false);
                return;
            }

            if (creativeInstant) {
                com.YLFXM.mcreconstruction.road.RoadBlueprintStore.save(level, plan, blueprint);
                RoadQuickBuilder.place(level, plan, blueprint, mode == RoadPlacementMode.PRETTY);
                // Complete/Pretty write the road body over the old controller
                // when it sat inside the footprint, and never move it. Recreate
                // it on the reserved inner cell with the same migration the
                // builder path uses. The legacy custom controller is no longer
                // placed here.
                com.YLFXM.mcreconstruction.road.RoadControllerMigration.migrate(level, plan);
                plan.setState(RoadState.BUILT);
                RoadManager.get(level).add(plan);
                com.YLFXM.mcreconstruction.road.RoadTemporaryPack.delete(level, plan);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Road " + RoadNames.label(plan) + ": bounds "
                                + plan.bounds(Config.defaultLampReserveHeight()).sizeX() + "x"
                                + plan.bounds(Config.defaultLampReserveHeight()).sizeY() + "x"
                                + plan.bounds(Config.defaultLampReserveHeight()).sizeZ()
                                + ", intersections " + intersections + ", placed instantly (" + mode + ")."), false);
            } else {
                RoadWorkOrderService.createAndAdd(level, plan, blueprint, colony);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Road " + RoadNames.label(plan) + ": bounds "
                                + plan.bounds(Config.defaultLampReserveHeight()).sizeX() + "x"
                                + plan.bounds(Config.defaultLampReserveHeight()).sizeY() + "x"
                                + plan.bounds(Config.defaultLampReserveHeight()).sizeZ()
                                + ", intersections " + intersections + ", work order created."), false);
            }

            // Push the new state (BUILT / BUILDING) so the client panel and
            // preview do not keep using the pre-build DRAFT/PLANNED plan.
            sendRoads(player, colony);
        });
    }

    private static net.minecraft.world.item.ItemStack findBoundWand(final ServerPlayer player,
                                                                    final BlockPos controllerPos) {
        final var main = player.getMainHandItem();
        if (main.is(ModRegistry.ROAD_WAND.get())
                && controllerPos.equals(com.YLFXM.mcreconstruction.road.RoadWandBinding.getBoundController(main))) {
            return main;
        }
        final var offhand = player.getOffhandItem();
        if (offhand.is(ModRegistry.ROAD_WAND.get())
                && controllerPos.equals(com.YLFXM.mcreconstruction.road.RoadWandBinding.getBoundController(offhand))) {
            return offhand;
        }
        for (final var stack : player.getInventory().items) {
            if (stack.is(ModRegistry.ROAD_WAND.get())
                    && controllerPos.equals(com.YLFXM.mcreconstruction.road.RoadWandBinding.getBoundController(stack))) {
                return stack;
            }
        }
        // Fallback to the server-side player binding. This keeps validation
        // working even if a client inventory sync lags behind.
        final long fallback = player.getPersistentData().getLong("mcreconstruction:road_wand_controller");
        if (fallback == controllerPos.asLong()) {
            if (main.is(ModRegistry.ROAD_WAND.get())) {
                return main;
            }
            if (offhand.is(ModRegistry.ROAD_WAND.get())) {
                return offhand;
            }
            for (final var stack : player.getInventory().items) {
                if (stack.is(ModRegistry.ROAD_WAND.get())) {
                    return stack;
                }
            }
        }
        return null;
    }

    private static Direction directionBetween(final BlockPos a, final BlockPos b) {
        if (a.getX() == b.getX()) {
            return b.getZ() > a.getZ() ? Direction.SOUTH : Direction.NORTH;
        }
        return b.getX() > a.getX() ? Direction.EAST : Direction.WEST;
    }

    private static void handleRequest(final RequestRoadsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            RoadWorkOrderRecovery.recoverAll(player.server);
            sendRoads(player);
            MCReconstruction.LOGGER.debug("Sent road index to {}", player.getName().getString());
        });
    }

    private static void handleBulkAction(final RoadBulkActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            final ServerLevel level = player.serverLevel();
            int cleared = 0;
            int denied = 0;
            com.minecolonies.api.colony.IColony scope = null;
            for (final java.util.UUID id : payload.roadIds()) {
                final RoadPlan plan = RoadManager.get(level).get(id);
                if (plan == null) {
                    continue;
                }
                if (!canManageRoad(level, player, plan)) {
                    denied++;
                    continue;
                }
                if (scope == null) {
                    // Keep the refreshed view scoped to the colony being acted on.
                    scope = IColonyManager.getInstance()
                            .getColonyByPosFromWorld(level, plan.controllerPos());
                }
                RoadDataCleaner.clear(level, plan, payload.removeBlocks());
                cleared++;
            }
            sendRoads(player, scope);
            final int count = cleared;
            if (denied > 0) {
                // No permission prompt in chat: the colony system already covers
                // it. Keep the count in the debug log for diagnosis.
                MCReconstruction.LOGGER.debug("Skipped {} road(s) for {} (no manage-huts)",
                        denied, player.getName().getString());
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Processed " + count + " road(s)."), false);
        });
    }

    /** Permission for destructive road actions: manage-huts in the owning colony. */
    private static boolean canManageRoad(final ServerLevel level, final ServerPlayer player,
                                         final RoadPlan plan) {
        return com.YLFXM.mcreconstruction.RoadPermissions.canManage(level, player, plan.controllerPos());
    }

    private static void handleControllerAction(final RoadControllerActionPayload payload,
                                               final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            final ServerLevel level = player.serverLevel();
            final RoadPlan plan = RoadManager.get(level).findByController(payload.controllerPos());
            if (plan == null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "No road plan on this controller."), false);
                return;
            }
            if (!canManageRoad(level, player, plan)) {
                MCReconstruction.LOGGER.debug("Denied controller action {} for {} (no manage-huts)",
                        payload.action(), player.getName().getString());
                return;
            }
            switch (payload.action()) {
                case REMOVE -> com.YLFXM.mcreconstruction.road.RoadRemover.remove(level, plan);
                case REPAIR -> RoadRepair.repair(level, plan);
                case UPGRADE -> player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Road upgrades are not implemented yet."), false);
            }
            sendRoads(player, IColonyManager.getInstance()
                    .getColonyByPosFromWorld(level, plan.controllerPos()));
        });
    }

    private static void handleSync(final RoadSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientRoadCache.set(payload.data());
            // Drop local blueprint copies the server no longer vouches for. This
            // only ever removes files under generated/ that this client is not
            // supposed to see, never anything the player is entitled to.
            final ListTag allowed = payload.data().getList("blueprints", 8);
            final java.util.Set<String> keep = new java.util.HashSet<>();
            for (int i = 0; i < allowed.size(); i++) {
                keep.add(allowed.getString(i));
            }
            com.YLFXM.mcreconstruction.client.ClientRoadPackWriter.retainOnly(keep);
        });
    }

    private static void handleBindWand(final BindRoadWandPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            if (player.getMainHandItem().is(ModRegistry.ROAD_WAND.get())) {
                com.YLFXM.mcreconstruction.road.RoadWandBinding.bind(player.getMainHandItem(), payload.controllerPos());
            }
            if (player.getOffhandItem().is(ModRegistry.ROAD_WAND.get())) {
                com.YLFXM.mcreconstruction.road.RoadWandBinding.bind(player.getOffhandItem(), payload.controllerPos());
            }
            for (final var stack : player.getInventory().items) {
                if (stack.is(ModRegistry.ROAD_WAND.get())) {
                    com.YLFXM.mcreconstruction.road.RoadWandBinding.bind(stack, payload.controllerPos());
                }
            }
        });
    }

    private static void handleOpenBuildTool(final OpenBuildToolPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            // The server pushes the blueprint with the payload: packs are only
            // synced on join, so a freshly generated one does not exist here yet.
            com.YLFXM.mcreconstruction.client.ClientRoadPackWriter.ensure(
                    payload.blueprintPath(), payload.blueprint(), payload.packVersion());
            com.YLFXM.mcreconstruction.client.ClientNativeBuildTracker.begin(
                    payload.planId(), payload.blueprintPath());
            com.ldtteam.structurize.storage.StructurePacks.switchSelectedPack(
                    com.YLFXM.mcreconstruction.CustomRoadPack.PACK_NAME);
            new com.ldtteam.structurize.client.gui.WindowExtendedBuildTool(
                    null, 1, minecraft.level.registryAccess()).open();
        });
    }

    public static void sendWorkOrders(final ServerPlayer player,
                                      final com.minecolonies.api.colony.IColony colony) {
        sendWorkOrders(player, colony, true);
    }

    /**
     * @param requireView false only for the refresh after an action the player
     *                    was already allowed to perform, so the window cannot
     *                    go stale just because a custom rank was given
     *                    {@code MANAGE_HUTS} without {@code ACCESS_HUTS}
     */
    public static void sendWorkOrders(final ServerPlayer player,
                                      final com.minecolonies.api.colony.IColony colony,
                                      final boolean requireView) {
        // Colony internals: the work order list needs ACCESS_HUTS, the same bit
        // the original uses to let a player into a hut at all. Vanilla's
        // resource scroll actually ships no check, but shipping colony internals
        // to anyone holding a bound item is a leak, not a feature.
        if (requireView && !com.YLFXM.mcreconstruction.RoadPermissions.canView(player, colony)) {
            // Nothing is sent: the client opens the window only when this
            // payload arrives, so staying silent keeps it closed -- the same
            // outcome as vanilla denying a hut click.
            com.YLFXM.mcreconstruction.RoadPermissions.notifyDenied(player);
            return;
        }
        final java.util.List<WorkOrderListPayload.Entry> entries = new java.util.ArrayList<>();
        for (final var order : colony.getWorkManager().getWorkOrders().values()) {
            if (order == null) {
                continue;
            }
            // A single unreadable order must never take down the whole list:
            // decoration orders can have no stage yet, and getStage() returns
            // null until a builder picks the order up.
            try {
                final var stage = order.getStage();
                final String status = !order.isClaimed()
                        ? "Unclaimed"
                        : "Claimed" + (stage == null ? "" : " / " + stage.name());
                String displayName = order.getDisplayName().getString();
                final RoadPlan road = roadForPath(player.serverLevel(), order.getStructurePath());
                if (road != null) {
                    displayName = RoadNames.label(road);
                }
                entries.add(new WorkOrderListPayload.Entry(order.getID(),
                        order.getWorkOrderType() == null ? "UNKNOWN" : order.getWorkOrderType().name(),
                        displayName,
                        order.getLocation().toShortString(),
                        status,
                        order.getPriority(),
                        order.getCurrentLevel(),
                        order.getTargetLevel()));
            } catch (final RuntimeException ex) {
                MCReconstruction.LOGGER.warn("Skipping unreadable work order in colony {}",
                        colony.getID(), ex);
            }
        }
        entries.sort(java.util.Comparator.comparingInt(WorkOrderListPayload.Entry::priority)
                .thenComparingInt(WorkOrderListPayload.Entry::id));
        PacketDistributor.sendToPlayer(player, new WorkOrderListPayload(colony.getID(), colony.getName(),
                entries, true, com.YLFXM.mcreconstruction.RoadPermissions.canManage(player, colony)));
    }

    /** Unbound scroll: open an empty list that tells the player how to bind. */
    public static void sendUnboundWorkOrders(final ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new WorkOrderListPayload(-1, "", java.util.List.of(), false, false));
    }

    private static RoadPlan roadForPath(final ServerLevel level, final String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("mcreconstruction:road/")) {
            final String key = path.substring("mcreconstruction:road/".length());
            final RoadPlan byName = RoadManager.get(level).findByName(key);
            if (byName != null) {
                return byName;
            }
            try {
                return RoadManager.get(level).get(UUID.fromString(key));
            } catch (final IllegalArgumentException ex) {
                return null;
            }
        }
        if (!path.endsWith(".blueprint")) {
            return null;
        }
        final int slash = path.lastIndexOf('/');
        final String fileName = path.substring(slash + 1, path.length() - ".blueprint".length());
        final RoadPlan byBlueprint = RoadManager.get(level).findByBlueprintName(fileName);
        if (byBlueprint != null) {
            return byBlueprint;
        }
        final RoadPlan byName = RoadManager.get(level).findByName(fileName);
        if (byName != null) {
            return byName;
        }
        if (fileName.startsWith("road_")) {
            try {
                return RoadManager.get(level).get(
                        UUID.fromString(fileName.substring("road_".length())));
            } catch (final IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private static void handleWorkOrderList(final WorkOrderListPayload payload,
                                            final IPayloadContext context) {
        context.enqueueWork(() -> com.YLFXM.mcreconstruction.client.WindowWorkOrderList
                .show(payload.colonyId(), payload.colonyName(), payload.entries(),
                        payload.bound(), payload.canManage()));
    }

    /**
     * Cancels the selected work orders and pushes a fresh list back so the open
     * window refreshes from authoritative data.
     */
    private static void handleCancelWorkOrders(final CancelWorkOrdersPayload payload,
                                               final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            IColony colony = null;
            for (final IColony candidate : IColonyManager.getInstance().getAllColonies()) {
                if (candidate.getID() == payload.colonyId()) {
                    colony = candidate;
                    break;
                }
            }
            if (colony == null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "That colony no longer exists."), false);
                return;
            }
            if (!com.YLFXM.mcreconstruction.RoadPermissions.canManage(player, colony)) {
                MCReconstruction.LOGGER.debug("Denied work order cancel for {} in colony {}",
                        player.getName().getString(), payload.colonyId());
                return;
            }
            int cancelled = 0;
            for (final int orderId : payload.orderIds()) {
                try {
                    if (colony.getWorkManager().getWorkOrders().containsKey(orderId)) {
                        colony.getWorkManager().removeWorkOrder(orderId);
                        cancelled++;
                    }
                } catch (final RuntimeException ex) {
                    MCReconstruction.LOGGER.warn("Could not cancel work order {} in colony {}",
                            orderId, payload.colonyId(), ex);
                }
            }
            final int count = cancelled;
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Cancelled " + count + " work order(s)."), false);
            // Already authorised by canManage above; refresh unconditionally.
            sendWorkOrders(player, colony, false);
        });
    }

    private static void handleNativeBuildClosed(final NativeBuildClosedPayload payload,
                                                final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            final ServerLevel level = player.serverLevel();
            final RoadPlan plan = RoadManager.get(level).get(payload.planId());
            if (plan != null) {
                RoadBuildWatcher.requestClose(level, plan);
            }
        });
    }

    private static void handleOpenPlan(final OpenRoadPlanPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            com.YLFXM.mcreconstruction.client.RoadSelectionClient.setControllerPos(payload.controllerPos());
            new com.YLFXM.mcreconstruction.client.WindowRoadController(payload.controllerPos()).open();
        });
    }

    public static void sendRoads(final ServerPlayer player) {
        sendRoads(player, null);
    }

    /**
     * Sends the road index for a player.
     *
     * <p>With a colony, only that colony's roads are included and the client
     * marks the view as bound. Without one (the Road Wand panel) every road the
     * player is allowed to know about is sent.</p>
     *
     * <p><b>Visibility rules.</b> A road is only sent to a player who could
     * actually see it in the world:</p>
     * <ul>
     *   <li>Colony road — members of that colony may see it (viewing is not
     *       restricted; the house-manage permission only gates the buttons).</li>
     *   <li>Wilderness road — <em>creator only</em>. Wilderness is nobody's
     *       business, and a road's name plus controller position already leaks
     *       where someone is building.</li>
     * </ul>
     * <p>Roads from another colony, and wilderness roads planned by somebody
     * else, are omitted entirely rather than greyed out — a greyed row would
     * still tell the player a road exists there.</p>
     */
    public static void sendRoads(final ServerPlayer player,
                                 final com.minecolonies.api.colony.IColony colony) {
        final ServerLevel level = player.serverLevel();
        final CompoundTag tag = new CompoundTag();
        tag.putString("dimension", level.dimension().location().toString());
        tag.putBoolean("bound", colony != null);
        tag.putString("colony", colony == null ? "" : colony.getName());
        final ListTag list = new ListTag();
        // Which roads this player may act on; the client only uses it to grey out
        // buttons, every action is re-checked server side.
        final ListTag manageable = new ListTag();
        for (final RoadPlan plan : RoadManager.get(level).all()) {
            final com.minecolonies.api.colony.IColony owner = IColonyManager.getInstance()
                    .getColonyByPosFromWorld(level, plan.controllerPos());
            if (colony != null) {
                // Bound view: the roads of the tool's colony that this player is
                // allowed to see. Without the visibility check here, binding to
                // any town hall would bypass the colony-road permission gate.
                if (owner == null || owner.getID() != colony.getID()
                        || !isVisibleTo(plan, owner, player)) {
                    continue;
                }
            } else if (!isVisibleTo(plan, owner, player)) {
                continue;
            }
            list.add(plan.save());
            if (com.YLFXM.mcreconstruction.RoadPermissions.canManage(level, player, plan.controllerPos())) {
                manageable.add(net.minecraft.nbt.StringTag.valueOf(plan.id().toString()));
            }
        }
        tag.put("roads", list);
        tag.put("manageable", manageable);
        // Every blueprint file name this player is entitled to, across all
        // dimensions. The client prunes anything else from its local
        // blueprints/Reconstruction Plan/generated/ so blueprints pushed in an
        // earlier session (or for a road the player lost access to) cannot
        // linger in the native build tool.
        tag.put("blueprints", entitledBlueprintNames(player));
        PacketDistributor.sendToPlayer(player, new RoadSyncPayload(tag));
    }

    /**
     * Names of the road blueprints {@code player} is entitled to, in every
     * dimension.
     *
     * <p>Names rather than ids: the client stores blueprints as
     * {@code <name>.blueprint}, and the name is what the build tool lists.</p>
     *
     * <p>Deliberately stricter than {@link #isVisibleTo}: seeing that a road
     * exists is not the same as being handed its blueprint, which describes the
     * colony's layout block by block.</p>
     */
    private static ListTag entitledBlueprintNames(final ServerPlayer player) {
        final ListTag names = new ListTag();
        final java.util.Set<String> seen = new java.util.HashSet<>();
        for (final ServerLevel other : player.getServer().getAllLevels()) {
            for (final RoadPlan plan : RoadManager.get(other).all()) {
                // Finished roads have no blueprint anywhere: the server removed it
                // from the pack when the road was marked built. Leaving the name
                // in this list would let the client keep its copy, and the native
                // build tool would go on listing a road that no longer needs one.
                if (plan.state() == RoadState.BUILT || plan.state() == RoadState.REMOVED) {
                    continue;
                }
                final com.minecolonies.api.colony.IColony owner = IColonyManager.getInstance()
                        .getColonyByPosFromWorld(other, plan.controllerPos());
                if (isEntitledToBlueprint(plan, owner, player) && seen.add(RoadNames.fileName(plan))) {
                    names.add(net.minecraft.nbt.StringTag.valueOf(RoadNames.fileName(plan)));
                }
            }
        }
        return names;
    }

    /**
     * Who may receive the blueprint file of a road.
     *
     * <ul>
     *   <li>The player who planned it, always.</li>
     *   <li>Colony roads: the owner and anyone with {@code MANAGE_HUTS} in that
     *       colony — i.e. the people who can actually authorise work there.</li>
     *   <li>Wilderness roads: the creator only. Never a colony manager from
     *       somewhere else.</li>
     * </ul>
     */
    private static boolean isEntitledToBlueprint(final RoadPlan plan,
                                                 final com.minecolonies.api.colony.IColony owner,
                                                 final ServerPlayer player) {
        if (plan.isCreatedBy(player.getUUID())) {
            return true;
        }
        if (owner == null) {
            // Wilderness roads planned before the creator field existed are
            // unowned, and unowned means public.
            return plan.creator() == null;
        }
        return com.YLFXM.mcreconstruction.RoadPermissions.canManage(player, owner);
    }

    /**
     * Whether {@code player} is allowed to know that {@code plan} exists.
     *
     * <p><b>Colony roads are not filtered at all.</b> MineColonies has no
     * "member of a colony" notion to gate this on: {@code Permissions#getPlayers()}
     * is a rank roster, and it also holds <em>hostile</em> players (a colony adds
     * an attacker's owner with {@code HOSTILE_RANK_ID}, see
     * {@code EntityCitizen}), so using "is in the roster" as "is a member" would
     * hand a raider the colony's whole road index. Vanilla does not gate this
     * kind of view either: {@code ItemClipboard} opens the resource scroll with
     * no permission check whatsoever, and this mod's own Work Order Scroll is
     * viewable by anyone by design. A road is made of blocks in the world —
     * walking the colony already shows it — so hiding the index would protect
     * nothing while making the tool useless to visitors.</p>
     *
     * <p>Wilderness roads are the exception: no colony covers them, so there is
     * no permission to consult, and an unbuilt plan would otherwise reveal where
     * somebody intends to build. They are limited to their creator.</p>
     */
    private static boolean isVisibleTo(final RoadPlan plan,
                                       final com.minecolonies.api.colony.IColony owner,
                                       final ServerPlayer player) {
        if (owner != null) {
            return com.YLFXM.mcreconstruction.RoadPermissions.canView(player, owner);
        }
        // Wilderness: unowned roads (creator == null, i.e. saved before the
        // field existed) are public; owned ones belong to their creator.
        return plan.creator() == null || plan.isCreatedBy(player.getUUID());
    }

    private Network() {
    }
}
