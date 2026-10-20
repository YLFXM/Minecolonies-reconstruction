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
import com.ldtteam.structurize.blueprints.v1.BlueprintUtil;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.core.blocks.BlockDecorationController;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Materializes a small native structure pack containing the initial
 * infrastructure/custom/road blueprint (visible anchor + decoration controller).
 */
@EventBusSubscriber(modid = MCReconstruction.MODID)
public final class CustomRoadPack {
    public static final String PACK_NAME = "Reconstruction Plan";
    private static final String BLUEPRINT_PATH = "initial/road.blueprint";

    @SubscribeEvent
    public static void onServerStarted(final ServerStartedEvent event) {
        install(event.getServer());
        // Re-derive generated/ from the snapshots: it must contain exactly the
        // blueprints of roads that are still being built. Without this a crash
        // between "road finished" and "file removed" would leave that blueprint
        // in the pack permanently, and the pack is broadcast to every joining
        // player.
        com.YLFXM.mcreconstruction.road.RoadTemporaryPack.rebuildFromStore(event.getServer());
    }

    public static Path packRoot(final MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("mcreconstruction").resolve("structurepack");
    }

    /**
     * Rebuilds and re-registers the per-save pack.
     *
     * @return the content version written into pack.json, or {@code -1} on error
     */
    public static long install(final MinecraftServer server) {
        try {
            final Path packRoot = packRoot(server);
            Files.createDirectories(packRoot.resolve("initial"));
            // Remove the old two/three-level layout so the native pack list
            // shows only the new first-level categories.
            deleteRecursively(packRoot.resolve("infrastructure"));

            final Path blueprintPath = packRoot.resolve(BLUEPRINT_PATH);
            // Always refresh the tiny generated blueprint so older tests do not
            // keep the previous top-mounted controller layout.
            writeControllerBlueprint(blueprintPath, server.registryAccess());

            // The pack version must follow the content: Structurize compares it
            // with the client's copy and re-sends the pack when it differs.
            final long version = contentVersion(packRoot);

            final Path packJson = packRoot.resolve("pack.json");
            Files.writeString(packJson, """
                    {
                      "icon": "",
                      "name": "%s",
                      "authors": ["ZhuanZ"],
                      "desc": "Reconstruction Plan: initial anchor and generated road blueprints.",
                      "mods": ["structurize", "minecolonies"],
                      "version": %d,
                      "pack-format": 1
                    }
                    """.formatted(PACK_NAME, version), StandardCharsets.UTF_8);

            // immutable=false is mandatory: ServerStructurePackLoader only ships
            // packs to clients when !pack.isImmutable(). Registering this pack as
            // immutable leaves every client waiting for a pack the server will
            // never send, so the client never finishes StructurePacks loading and
            // the build tool shows no packs at all.
            StructurePacks.discoverPackAtPath(packRoot, false,
                    List.of("structurize", "minecolonies"), false, "mcreconstruction");
            return version;
        } catch (final IOException ex) {
            MCReconstruction.LOGGER.error("Could not create custom road pack", ex);
            return -1L;
        }
    }

    /**
     * Stable numeric version derived from the pack content.
     *
     * <p>Hashes file names and <em>contents</em>, not timestamps: {@link #install}
     * rewrites {@code initial/road.blueprint} on every call, so a timestamp-based
     * version would change on every install and force every client to download
     * the pack again. Content hashing keeps the version stable while the pack
     * actually is, and still changes it when the blueprint really changes.
     * {@code pack.json} is excluded so writing it cannot feed back into the
     * hash.</p>
     */
    private static long contentVersion(final Path packRoot) {
        long hash = 17L;
        for (final String folder : new String[]{"initial", "generated"}) {
            final Path dir = packRoot.resolve(folder);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (var paths = Files.walk(dir)) {
                for (final Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                    hash = hash * 31L + dir.relativize(path).toString().hashCode();
                    try {
                        hash = hash * 31L + java.util.Arrays.hashCode(Files.readAllBytes(path));
                    } catch (final IOException ex) {
                        // Fall back to size so an unreadable file still shifts
                        // the version instead of silently keeping the old one.
                        hash = hash * 31L + Files.size(path);
                    }
                }
            } catch (final IOException ex) {
                MCReconstruction.LOGGER.warn("Could not hash structure pack content", ex);
            }
        }
        return Math.abs(hash % 1_000_000_000L);
    }

    private static void deleteRecursively(final Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (final IOException ignored) {
                }
            });
        } catch (final IOException ignored) {
        }
    }

    private static void writeControllerBlueprint(final Path path, final RegistryAccess registryAccess)
            throws IOException {
        final BlockState anchor = com.YLFXM.mcreconstruction.road.RoadUnitExtractor
                .blockState(Config.roadAnchorBlock());
        final Block controllerBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse("minecolonies:decorationcontroller"));
        if (!(controllerBlock instanceof BlockDecorationController)) {
            MCReconstruction.LOGGER.error("Decoration controller block missing; custom road pack not created.");
            return;
        }
        // Controller sits on the east side and faces west toward the anchor.
        final BlockState controller = controllerBlock.defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.WEST);

        // The initial controller blueprint is a single layer: anchor and
        // controller side by side. Its placement height is chosen by the
        // player/build tool; the road blueprint handles the +1 lamp position.
        final Blueprint blueprint = new Blueprint((short) 2, (short) 1, (short) 1, registryAccess);
        blueprint.addBlockState(new BlockPos(0, 0, 0), anchor);
        blueprint.addBlockState(new BlockPos(1, 0, 0), controller);
        blueprint.setCachePrimaryOffset(BlockPos.ZERO);
        blueprint.setPackName(PACK_NAME);
        blueprint.setFilePath(Path.of("initial"));
        blueprint.setFileName("road");

        final CompoundTag te = new CompoundTag();
        te.putString("id", "minecolonies:decorationcontroller");
        te.putInt("x", 1);
        te.putInt("y", 0);
        te.putInt("z", 0);
        final CompoundTag provider = new CompoundTag();
        provider.putString("pack", PACK_NAME);
        provider.putString("path", BLUEPRINT_PATH);
        provider.putString("schematicName", "road");
        provider.put("corner1", NbtUtils.writeBlockPos(new BlockPos(0, 0, 0)));
        provider.put("corner2", NbtUtils.writeBlockPos(new BlockPos(1, 0, 0)));
        te.put("blueprintDataProvider", provider);
        blueprint.getTileEntities()[0][0][1] = te;

        final CompoundTag tag = BlueprintUtil.writeBlueprintToNBT(blueprint);
        Files.createDirectories(path.getParent());
        NbtIo.writeCompressed(tag, path);
    }

    private CustomRoadPack() {
    }
}
