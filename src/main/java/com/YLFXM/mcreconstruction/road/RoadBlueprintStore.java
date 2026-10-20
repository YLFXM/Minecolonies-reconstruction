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

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.blueprints.v1.BlueprintUtil;
import com.YLFXM.mcreconstruction.MCReconstruction;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Per-save snapshot of a generated road blueprint. RoadPlan can regenerate a
 * blueprint, but the snapshot is the canonical block set used for removal.
 */
public final class RoadBlueprintStore {
    public static Path directory(final ServerLevel level) {
        final MinecraftServer server = level.getServer();
        return server.getWorldPath(LevelResource.ROOT).resolve("mcreconstruction").resolve("blueprints");
    }

    public static Path file(final ServerLevel level, final RoadPlan plan) {
        return directory(level).resolve("road_" + plan.id() + ".blueprint");
    }

    public static void save(final ServerLevel level, final RoadPlan plan, final Blueprint blueprint) {
        try {
            final Path path = file(level, plan);
            Files.createDirectories(path.getParent());
            final CompoundTag tag = BlueprintUtil.writeBlueprintToNBT(blueprint);
            NbtIo.writeCompressed(tag, path);
        } catch (final IOException ex) {
            MCReconstruction.LOGGER.error("Could not save road blueprint for {}", plan.id(), ex);
        }
    }

    public static Blueprint load(final ServerLevel level, final RoadPlan plan) {
        final Path path = file(level, plan);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (InputStream input = Files.newInputStream(path)) {
            final CompoundTag tag = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            return BlueprintUtil.readBlueprintFromNBT(tag, level.registryAccess());
        } catch (final IOException ex) {
            MCReconstruction.LOGGER.error("Could not load road blueprint for {}", plan.id(), ex);
            return null;
        }
    }

    public static void delete(final ServerLevel level, final RoadPlan plan) {
        try {
            Files.deleteIfExists(file(level, plan));
        } catch (final IOException ex) {
            MCReconstruction.LOGGER.warn("Could not delete road blueprint for {}", plan.id(), ex);
        }
    }

    private RoadBlueprintStore() {
    }
}
