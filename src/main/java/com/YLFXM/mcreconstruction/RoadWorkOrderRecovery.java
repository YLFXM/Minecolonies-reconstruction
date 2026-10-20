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

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.workorders.IServerWorkOrder;
import com.minecolonies.core.colony.workorders.AbstractWorkOrder;
import com.YLFXM.mcreconstruction.road.RoadBlueprintGenerator;
import com.YLFXM.mcreconstruction.road.RoadManager;
import com.YLFXM.mcreconstruction.road.RoadPlan;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Recovers road work-order blueprints from the persistent RoadManager index.
 * It no longer polls every second: recovery runs when a player enters the world
 * and when the Road Surveyor requests the road list.
 */
@EventBusSubscriber(modid = MCReconstruction.MODID)
public final class RoadWorkOrderRecovery {
    @SubscribeEvent
    public static void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> {
                recoverAll(player.server);
                // Push the road index on join: besides filling the panels, this
                // is what tells the client which blueprints it is still allowed
                // to keep, so stale copies from a previous session get pruned
                // before the player opens the native build tool.
                com.YLFXM.mcreconstruction.network.Network.sendRoads(player);
            });
        }
    }

    public static void recoverAll(final MinecraftServer server) {
        for (final IColony colony : IColonyManager.getInstance().getAllColonies()) {
            if (!(colony.getWorld() instanceof ServerLevel level)) {
                continue;
            }
            for (final IServerWorkOrder workOrder : colony.getWorkManager().getWorkOrders().values()) {
                if (!(workOrder instanceof AbstractWorkOrder order)) {
                    continue;
                }
                final String path = order.getStructurePath();
                if (path == null) {
                    continue;
                }

                if (path.startsWith("mcreconstruction:road/")) {
                    if (order.getBlueprint() != null) {
                        continue;
                    }
                    final String key = path.substring("mcreconstruction:road/".length());
                    RoadPlan plan = RoadManager.get(level).findByName(key);
                    UUID id = plan == null ? null : plan.id();
                    if (plan == null) {
                        try {
                            id = UUID.fromString(key);
                            plan = RoadManager.get(level).get(id);
                        } catch (final IllegalArgumentException ex) {
                            continue;
                        }
                    }
                    if (plan == null) {
                        continue;
                    }
                    final Blueprint blueprint = RoadBlueprintGenerator.generate(plan, level,
                            Config.defaultLampReserveHeight());
                    if (blueprint != null) {
                        order.setBlueprint(blueprint, level);
                        MCReconstruction.LOGGER.info("Recovered road blueprint for work order {}", id);
                    }
                    continue;
                }

                // Native Survival work orders point at generated/<road name>.blueprint.
                if (path.endsWith(".blueprint")) {
                    final int slash = path.lastIndexOf('/');
                    final String fileName = path.substring(slash + 1, path.length() - ".blueprint".length());
                    RoadPlan plan = RoadManager.get(level).findByBlueprintName(fileName);
                    if (plan == null) {
                        plan = RoadManager.get(level).findByName(fileName);
                    }
                    if (plan == null && fileName.startsWith("road_")) {
                        try {
                            plan = RoadManager.get(level).get(
                                    UUID.fromString(fileName.substring("road_".length())));
                        } catch (final IllegalArgumentException ignored) {
                        }
                    }
                    if (plan != null) {
                        plan.setState(com.YLFXM.mcreconstruction.road.RoadState.BUILDING);
                        RoadManager.get(level).add(plan);
                        com.YLFXM.mcreconstruction.road.RoadBuildWatcher.watch(level, plan);
                        MCReconstruction.LOGGER.info("Tracking native road work order {}", plan.id());
                    }
                }
            }
        }
    }

    private RoadWorkOrderRecovery() {
    }
}
