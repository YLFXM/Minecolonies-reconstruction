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

import com.ldtteam.blockui.BOScreen;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.client.gui.AbstractBlueprintManipulationWindow;
import com.ldtteam.structurize.client.gui.WindowExtendedBuildTool;
import com.ldtteam.structurize.storage.StructurePacks;
import com.YLFXM.mcreconstruction.CustomRoadPack;
import com.YLFXM.mcreconstruction.MCReconstruction;
import com.YLFXM.mcreconstruction.network.NativeBuildClosedPayload;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tracks the native build-tool window and tries to preselect the freshly
 * generated road blueprint, so the player only has to press Confirm.
 */
@EventBusSubscriber(modid = MCReconstruction.MODID, value = Dist.CLIENT)
public final class ClientNativeBuildTracker {
    private static UUID activePlan;
    private static String blueprintPath;
    private static boolean nativeWindowSeen;
    private static boolean applied;
    private static int applyAttempts;
    private static Method setBlueprintMethod;

    public static void begin(final UUID planId, final String path) {
        activePlan = planId;
        blueprintPath = path;
        nativeWindowSeen = false;
        applied = false;
        applyAttempts = 0;
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        if (activePlan == null) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof BOScreen screen)
                || !(screen.getWindow() instanceof WindowExtendedBuildTool window)) {
            if (nativeWindowSeen) {
                PacketDistributor.sendToServer(new NativeBuildClosedPayload(activePlan));
                activePlan = null;
                blueprintPath = null;
                nativeWindowSeen = false;
                applied = false;
            }
            return;
        }

        nativeWindowSeen = true;
        if (applied || applyAttempts >= 40 || minecraft.level == null) {
            return;
        }
        applyAttempts++;
        try {
            if (window.selectedBlueprint != null) {
                applied = true;
                return;
            }
            Blueprint blueprint = StructurePacks.getBlueprint(
                    CustomRoadPack.PACK_NAME, blueprintPath, minecraft.level.registryAccess());
            if (blueprint == null) {
                // Some pack APIs expect the category only; fall back to listing
                // generated and matching the filename.
                final int slash = blueprintPath.lastIndexOf('/');
                final String suffix = slash >= 0 ? blueprintPath.substring(slash + 1) : blueprintPath;
                for (final Blueprint candidate : StructurePacks.getBlueprints(
                        CustomRoadPack.PACK_NAME, "generated", minecraft.level.registryAccess())) {
                    if (candidate.getFilePath() != null
                            && candidate.getFilePath().toString().replace('\\', '/').endsWith(suffix)) {
                        blueprint = candidate;
                        break;
                    }
                }
            }
            if (blueprint == null) {
                return;
            }
            window.updateBlueprints(List.of(blueprint), "generated");
            if (setBlueprintMethod == null) {
                setBlueprintMethod = AbstractBlueprintManipulationWindow.class
                        .getDeclaredMethod("setBlueprint", Blueprint.class);
                setBlueprintMethod.setAccessible(true);
            }
            setBlueprintMethod.invoke(window, blueprint);
            window.updatePlacementOptions();
            applied = window.selectedBlueprint != null;
        } catch (final Exception ex) {
            MCReconstruction.LOGGER.warn("Could not preselect generated road blueprint", ex);
            applied = true;
        }
    }

    private ClientNativeBuildTracker() {
    }
}
