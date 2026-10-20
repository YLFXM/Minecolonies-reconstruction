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

import com.YLFXM.mcreconstruction.Config;
import com.YLFXM.mcreconstruction.MCReconstruction;
import com.YLFXM.mcreconstruction.network.Network;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.util.BlockInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Watches only recently planned roads (not the whole world). When the native
 * build tool finishes placing the generated blueprint, it marks the plan BUILT
 * and moves the controller inside the road.
 *
 * <p>Completion is verified against <em>every</em> road block of the blueprint,
 * not a sample: Structurize's Complete/Pretty/Paste modes and the builder place
 * blocks over many ticks, and marking a road built mid-placement would move the
 * controller into a cell whose placeholder task is still queued.</p>
 */
@EventBusSubscriber(modid = MCReconstruction.MODID)
public final class RoadBuildWatcher {
    private static final Map<UUID, Watch> ACTIVE = new ConcurrentHashMap<>();
    /** Native tool closed; wait a short grace period for queued placement. */
    private static final Map<UUID, Long> CLOSING = new ConcurrentHashMap<>();
    /** Post-completion window in which the controller is re-asserted if something overwrites it. */
    private static final Map<UUID, Guard> GUARD = new ConcurrentHashMap<>();
    /** While the build tool stays open or a work order is running, keep watching. */
    private static final long TIMEOUT_MS = 60L * 60L * 1000L;
    /** After the build tool closes, keep watching this long for a draining queue. */
    private static final long CLOSED_WATCH_MS = 10L * 60L * 1000L;
    /** 20 s of cover for late queued placement tasks after the road is marked built. */
    private static final long GUARD_TICKS = 400L;

    private record Watch(ServerLevel level, long deadline, Blueprint blueprint) {
    }

    private record Guard(ServerLevel level, long untilTick) {
    }

    public static void watch(final ServerLevel level, final RoadPlan plan) {
        // Load the blueprint once: the completion check runs every second and
        // must not re-read and re-parse the NBT from disk each time.
        Blueprint blueprint = RoadBlueprintStore.load(level, plan);
        if (blueprint == null) {
            blueprint = RoadBlueprintGenerator.generate(plan, level, Config.defaultLampReserveHeight());
        }
        ACTIVE.put(plan.id(), new Watch(level, System.currentTimeMillis() + TIMEOUT_MS, blueprint));
        CLOSING.remove(plan.id());
    }

    /**
     * Re-attaches the completion watch to every road that is still in progress.
     *
     * <p>{@link #ACTIVE} is in-memory only. Without this, a server restart drops
     * every watch, and a road being built with the <em>native build tool</em> has
     * no colony work order for {@code RoadWorkOrderRecovery} to find — so the
     * completion check never runs again, the plan stays {@code PLANNED}, and its
     * blueprint stays in the pack forever.</p>
     *
     * <p>{@code DRAFT} plans have no geometry yet and {@code BUILT} ones are
     * finished, so only {@code PLANNED} / {@code BUILDING} are re-armed.</p>
     */
    @SubscribeEvent
    public static void onServerStarted(final ServerStartedEvent event) {
        final MinecraftServer server = event.getServer();
        int restored = 0;
        for (final ServerLevel level : server.getAllLevels()) {
            for (final RoadPlan plan : RoadManager.get(level).all()) {
                if (plan.state() != RoadState.PLANNED && plan.state() != RoadState.BUILDING) {
                    continue;
                }
                watch(level, plan);
                restored++;
            }
        }
        if (restored > 0) {
            MCReconstruction.LOGGER.info("Re-watching {} in-progress road(s) after restart", restored);
        }
    }

    public static void requestClose(final ServerLevel level, final RoadPlan plan) {
        CLOSING.put(plan.id(), System.currentTimeMillis() + 3000L);
        // Closing the window stops nothing: Complete/Pretty/Paste may still be
        // draining. Give the plan a fresh window instead of letting the watch
        // created at planning time expire from under it.
        ACTIVE.computeIfPresent(plan.id(), (key, watch) ->
                new Watch(watch.level(), System.currentTimeMillis() + CLOSED_WATCH_MS, watch.blueprint()));
    }

    public static void markBuilt(final ServerLevel level, final RoadPlan plan) {
        RoadControllerMigration.migrate(level, plan);
        plan.setState(RoadState.BUILT);
        RoadManager.get(level).add(plan);
        level.players().forEach(Network::sendRoads);
        // The road is finished: drop its blueprint from the server pack. The
        // original build tool no longer needs it, and a blueprint sitting in the
        // pack would be broadcast to every player who joins. Repair / Remove use
        // the RoadBlueprintStore snapshot instead.
        RoadTemporaryPack.delete(level, plan);
        ACTIVE.remove(plan.id());
        CLOSING.remove(plan.id());
        GUARD.put(plan.id(), new Guard(level, level.getGameTime() + GUARD_TICKS));
        MCReconstruction.LOGGER.info("Road {} marked built", plan.id());
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        if (!ACTIVE.isEmpty()) {
            pollActive();
        }
        if (!GUARD.isEmpty()) {
            pollGuard();
        }
    }

    private static void pollActive() {
        final long now = System.currentTimeMillis();
        for (final Map.Entry<UUID, Watch> entry : new ArrayList<>(ACTIVE.entrySet())) {
            final UUID id = entry.getKey();
            final Watch watch = entry.getValue();
            final RoadPlan plan = RoadManager.get(watch.level()).get(id);
            if (plan == null || plan.state() == RoadState.REMOVED) {
                ACTIVE.remove(id);
                continue;
            }
            if (plan.state() == RoadState.BUILT) {
                ACTIVE.remove(id);
                CLOSING.remove(id);
                continue;
            }
            if (isBuilt(watch.level(), plan, watch.blueprint())) {
                markBuilt(watch.level(), plan);
                continue;
            }

            final boolean workOrder = hasWorkOrder(plan);
            if (workOrder) {
                if (plan.state() != RoadState.BUILDING) {
                    plan.setState(RoadState.BUILDING);
                    RoadManager.get(watch.level()).add(plan);
                    watch.level().players().forEach(Network::sendRoads);
                }
                // A builder work order can run for a long time; keep watching
                // instead of timing out and never migrating the controller.
                ACTIVE.put(id, new Watch(watch.level(), now + TIMEOUT_MS, watch.blueprint()));
                continue;
            }

            final Long closeAt = CLOSING.get(id);
            if (closeAt != null && now > closeAt) {
                // The tool closed without finishing: unlock the plan UI, but
                // keep watching. A large Paste can still be draining its queue
                // at this point and must be allowed to complete and migrate.
                plan.setState(RoadState.PLANNED);
                RoadManager.get(watch.level()).add(plan);
                CLOSING.remove(id);
                watch.level().players().forEach(Network::sendRoads);
                MCReconstruction.LOGGER.info("Road {} native placement closed; still watching", id);
            } else if (closeAt == null && now > watch.deadline()) {
                ACTIVE.remove(id);
                CLOSING.remove(id);
                MCReconstruction.LOGGER.info("Road {} native placement timed out; blueprint kept", id);
            }
        }
    }

    /**
     * Re-places the controller when a late placement task (Paste/Complete/Pretty)
     * resolves the reserved placeholder over it.
     */
    private static void pollGuard() {
        for (final Map.Entry<UUID, Guard> entry : new ArrayList<>(GUARD.entrySet())) {
            final UUID id = entry.getKey();
            final Guard guard = entry.getValue();
            final RoadPlan plan = RoadManager.get(guard.level()).get(id);
            if (plan == null || plan.state() == RoadState.REMOVED
                    || plan.state() != RoadState.BUILT
                    || guard.level().getGameTime() > guard.untilTick()) {
                GUARD.remove(id);
                continue;
            }
            if (!RoadControllerMigration.isController(guard.level().getBlockState(plan.controllerPos()))) {
                MCReconstruction.LOGGER.warn(
                        "Road {}: controller missing at {} after completion; re-placing",
                        id, plan.controllerPos());
                RoadControllerMigration.migrate(guard.level(), plan);
                guard.level().players().forEach(Network::sendRoads);
            }
        }
    }

    private static boolean hasWorkOrder(final RoadPlan plan) {
        final String id = plan.id().toString();
        for (final var colony : com.minecolonies.api.colony.IColonyManager.getInstance().getAllColonies()) {
            for (final var order : colony.getWorkManager().getWorkOrders().values()) {
                if (order == null) {
                    continue;
                }
                final String path = order.getStructurePath();
                if (path != null && (path.contains(id) || path.contains(RoadNames.fileName(plan)))) {
                    return true;
                }
                if (plan.anchorPos().equals(order.getLocation())
                        || plan.controllerPos().equals(order.getLocation())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isBuilt(final ServerLevel level, final RoadPlan plan, final Blueprint cached) {
        final Blueprint blueprint = cached;
        if (blueprint == null) {
            return false;
        }
        final BlockPos origin = plan.anchorPos().subtract(blueprint.getPrimaryBlockOffset());
        final List<BlockInfo> sample = new ArrayList<>();
        for (final BlockInfo info : blueprint.getBlockInfoAsList()) {
            final var state = info.getState();
            // Placeholders and air carry no information about build progress.
            if (state.isAir() || !RoadRemover.isRemovable(state)) {
                continue;
            }
            final BlockPos world = origin.offset(info.getPos());
            if (world.equals(plan.anchorPos()) || world.equals(plan.controllerPos())) {
                continue;
            }
            sample.add(info);
        }
        if (sample.isEmpty()) {
            return false;
        }
        // Full check: the first missing block proves the build is not finished.
        // Compare block types, not full states: the builder may legitimately
        // place the same block with a different waterlogged/axis value.
        for (final BlockInfo info : sample) {
            final BlockPos world = origin.offset(info.getPos());
            if (level.getBlockState(world).getBlock() != info.getState().getBlock()) {
                if (Config.debugLog()) {
                    MCReconstruction.LOGGER.debug(
                            "Road {} not built yet: {} is {} instead of {}",
                            plan.id(), world, level.getBlockState(world), info.getState());
                }
                return false;
            }
        }
        return true;
    }

    private RoadBuildWatcher() {
    }
}
