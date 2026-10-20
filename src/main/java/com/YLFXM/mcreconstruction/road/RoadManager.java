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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-dimension road index. MVP only uses the overworld, but the data is stored
 * on the level it belongs to.
 */
public final class RoadManager extends SavedData {
    private static final String DATA_NAME = "mcreconstruction_roads";
    private static final SavedData.Factory<RoadManager> FACTORY =
            new SavedData.Factory<>(RoadManager::new, RoadManager::load);

    private final Map<UUID, RoadPlan> roads = new HashMap<>();

    public static RoadManager get(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Collection<RoadPlan> all() {
        return Collections.unmodifiableCollection(roads.values());
    }

    public RoadPlan get(final UUID id) {
        return roads.get(id);
    }

    public RoadPlan findByController(final net.minecraft.core.BlockPos controllerPos) {
        for (final RoadPlan plan : roads.values()) {
            if (plan.controllerPos().equals(controllerPos)) {
                return plan;
            }
        }
        return null;
    }

    public RoadPlan findByBlueprintName(final String fileName) {
        for (final RoadPlan plan : roads.values()) {
            if (RoadNames.fileName(plan).equals(fileName)) {
                return plan;
            }
        }
        return null;
    }

    public RoadPlan findByName(final String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (final RoadPlan plan : roads.values()) {
            if (name.equals(plan.displayName())) {
                return plan;
            }
        }
        return null;
    }

    public RoadPlan findByAnchor(final net.minecraft.core.BlockPos anchorPos) {
        for (final RoadPlan plan : roads.values()) {
            if (plan.anchorPos().equals(anchorPos)) {
                return plan;
            }
        }
        return null;
    }

    public void add(final RoadPlan plan) {
        roads.put(plan.id(), plan);
        setDirty();
    }

    public void remove(final UUID id) {
        if (roads.remove(id) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider provider) {
        final ListTag list = new ListTag();
        for (final RoadPlan plan : roads.values()) {
            list.add(plan.save());
        }
        tag.put("roads", list);
        return tag;
    }

    private static RoadManager load(final CompoundTag tag, final HolderLookup.Provider provider) {
        final RoadManager manager = new RoadManager();
        final ListTag list = tag.getList("roads", 10);
        for (int i = 0; i < list.size(); i++) {
            final RoadPlan plan = RoadPlan.load(list.getCompound(i));
            manager.roads.put(plan.id(), plan);
        }
        return manager;
    }
}
