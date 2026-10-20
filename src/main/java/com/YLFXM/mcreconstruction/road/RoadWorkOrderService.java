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

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.core.colony.workorders.WorkOrderDecoration;
import net.minecraft.server.level.ServerLevel;

public final class RoadWorkOrderService {
    public static boolean createAndAdd(final ServerLevel level,
                                       final RoadPlan plan,
                                       final Blueprint blueprint,
                                       final IColony colony) {
        if (colony == null || blueprint == null) {
            return false;
        }
        RoadBlueprintStore.save(level, plan, blueprint);
        RoadTemporaryPack.install(level, plan, blueprint);
        plan.setState(RoadState.BUILDING);
        RoadManager.get(level).add(plan);
        final WorkOrderDecoration order = WorkOrderDecoration.create(
                WorkOrderType.BUILD,
                plan.pack(),
                "mcreconstruction:road/" + plan.displayName(),
                plan.displayName(),
                plan.anchorPos(),
                RotationMirror.NONE,
                0);
        order.setBlueprint(blueprint, level);
        order.setColony(colony);
        colony.getWorkManager().addWorkOrder(order, false);
        // The builder path has no build-tool window, so nothing else would
        // notice the finished road and move the controller inside.
        RoadBuildWatcher.watch(level, plan);
        return true;
    }

    private RoadWorkOrderService() {
    }
}
