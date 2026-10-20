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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.YLFXM.mcreconstruction.network.ClientRoadCache;
import com.YLFXM.mcreconstruction.road.RoadBounds;
import com.YLFXM.mcreconstruction.road.RoadPlan;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Blue AABB overlay for roads. When the server road index has been synced it
 * shows all known roads; otherwise it falls back to loaded controllers.
 */
@EventBusSubscriber(modid = MCReconstruction.MODID, value = Dist.CLIENT)
public final class ClientRoadRenderer {
    @SubscribeEvent
    public static void onRenderLevelStage(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!minecraft.player.getMainHandItem().is(ModRegistry.ROAD_SURVEYOR.get())
                && !minecraft.player.getOffhandItem().is(ModRegistry.ROAD_SURVEYOR.get())
                && !minecraft.player.getMainHandItem().is(ModRegistry.ROAD_WAND.get())
                && !minecraft.player.getOffhandItem().is(ModRegistry.ROAD_WAND.get())) {
            return;
        }

        final List<RoadPlan> plans = new ArrayList<>(ClientRoadCache.roads());
        final RoadPlan preview = com.YLFXM.mcreconstruction.client.ClientRoadPreview.plan();
        if (preview != null) {
            plans.add(preview);
        }

        final PoseStack pose = event.getPoseStack();
        final Vec3 camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (final RoadPlan plan : plans) {
            // Hide the cached planning box for the same controller, but never
            // skip the preview plan itself (it is also in this list).
            if (preview != null && plan != preview
                    && plan.controllerPos().equals(preview.controllerPos())) {
                continue;
            }
            if (!plan.dimension().equals(minecraft.level.dimension())) {
                continue;
            }
            final RoadBounds bounds = RoadBounds.ofRoadOnly(plan, Config.defaultLampReserveHeight());
            final AABB box = new AABB(bounds.minX(), bounds.minY(), bounds.minZ(),
                    bounds.maxX() + 1.0, bounds.maxY() + 1.0, bounds.maxZ() + 1.0);
            if (box.getCenter().distanceToSqr(camera) > 256.0 * 256.0) {
                continue;
            }
            if (plan.state() == com.YLFXM.mcreconstruction.road.RoadState.PLANNED
                    || plan.state() == com.YLFXM.mcreconstruction.road.RoadState.DRAFT) {
                LevelRenderer.renderLineBox(pose, lines, box, 1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                LevelRenderer.renderLineBox(pose, lines, box, 0.2F, 0.6F, 1.0F, 1.0F);
            }
        }
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    private ClientRoadRenderer() {
    }
}
