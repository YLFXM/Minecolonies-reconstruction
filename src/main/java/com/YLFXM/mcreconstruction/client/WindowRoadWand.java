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

import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Text;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.YLFXM.mcreconstruction.ModRegistry;
import com.YLFXM.mcreconstruction.MCReconstruction;
import com.YLFXM.mcreconstruction.network.ClientRoadCache;
import com.YLFXM.mcreconstruction.network.ConfigureRoadPayload;
import com.YLFXM.mcreconstruction.road.RoadNames;
import com.YLFXM.mcreconstruction.road.RoadPlacementMode;
import com.YLFXM.mcreconstruction.road.RoadPlan;
import com.YLFXM.mcreconstruction.road.RoadState;
import com.YLFXM.mcreconstruction.road.RoadWandBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * BlockUI Road Wand editor. Width and lamp spacing come from the bound plan /
 * building pack, not from this window.
 */
public class WindowRoadWand extends AbstractWindowSkeleton {
    private final Text summaryText;
    private final Text boundText;
    private final Text stateText;
    private final Button mirrorButton;
    private final Button planButton;
    private final Button clearButton;
    private boolean mirror;

    public WindowRoadWand() {
        super(ResourceLocation.fromNamespaceAndPath(MCReconstruction.MODID, "gui/windowroadwand.xml"));
        this.summaryText = this.findPaneOfTypeByID("summary", Text.class);
        this.boundText = this.findPaneOfTypeByID("bound", Text.class);
        this.stateText = this.findPaneOfTypeByID("state", Text.class);
        this.mirrorButton = this.findPaneOfTypeByID("mirror", Button.class);
        this.planButton = this.findPaneOfTypeByID("plan", Button.class);
        this.clearButton = this.findPaneOfTypeByID("clear", Button.class);
        this.mirror = RoadSelectionClient.mirror();
        this.mirrorButton.setText(Component.literal("Mirror: " + (this.mirror ? "on" : "off")));
        // "color" alone is reused by BlockUI for the disabled state; set an
        // explicit grey so disabled buttons read as disabled.
        for (final Button button : new Button[]{this.planButton, this.mirrorButton, this.clearButton}) {
            button.setColors(0x000000, 0x9A9A9A, 0x000000);
        }

        this.registerButton("plan", button -> this.send(RoadPlacementMode.PLAN));
        this.registerButton("mirror", button -> {
            this.mirror = !this.mirror;
            RoadSelectionClient.setMirror(this.mirror);
            this.mirrorButton.setText(Component.literal("Mirror: " + (this.mirror ? "on" : "off")));
        });
        this.registerButton("clear", button -> RoadSelectionClient.clear());
    }

    @Override
    public void onOpened() {
        super.onOpened();
        // Refresh the road index so the bound plan -- and with it the permission
        // state used for greying out buttons -- is current.
        PacketDistributor.sendToServer(new com.YLFXM.mcreconstruction.network.RequestRoadsPayload());
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.summaryText.setText(RoadSelectionClient.summary());
        final BlockPos bound = this.boundController();
        final RoadPlan plan = this.boundPlan(bound);
        this.boundText.setText(Component.literal(plan != null
                ? "Bound: " + RoadNames.label(plan)
                : bound == null ? "Unbound: right-click a road controller" : "Bound controller (no plan)"));
        final boolean locked = plan != null && plan.state() != RoadState.DRAFT
                && plan.state() != RoadState.PLANNED;
        // Grey out planning when the server says this player may not manage the
        // bound road; the server re-checks anyway.
        final boolean allowed = plan != null && ClientRoadCache.canManage(plan.id());
        this.stateText.setText(Component.literal(plan == null ? "No plan" : "Plan: " + plan.state()));
        this.planButton.setEnabled(!locked && allowed);
        this.mirrorButton.setEnabled(!locked && allowed);
        this.clearButton.setEnabled(!locked);
    }

    private void send(final RoadPlacementMode mode) {
        if (!RoadSelectionClient.hasBoth()) {
            if (this.mc.player != null) {
                this.mc.player.displayClientMessage(Component.literal(
                        "Select endpoint A (left click) and B (shift-right click) first."), true);
            }
            return;
        }
        final BlockPos controller = this.boundController();
        final RoadPlan plan = this.boundPlan(controller);
        if (controller == null || plan == null) {
            if (this.mc.player != null) {
                this.mc.player.displayClientMessage(Component.literal(
                        "Road Wand is not bound. Right-click a road controller first."), true);
            }
            return;
        }
        if (plan.state() != RoadState.DRAFT && plan.state() != RoadState.PLANNED) {
            if (this.mc.player != null) {
                this.mc.player.displayClientMessage(Component.literal(
                        "This road is already built; construction options are locked."), true);
            }
            return;
        }
        PacketDistributor.sendToServer(new ConfigureRoadPayload(controller, 1, plan.width(),
                plan.lampSpacing(), RoadSelectionClient.a(), RoadSelectionClient.b(), this.mirror, mode));
        RoadSelectionClient.clearAll();
        this.close();
    }

    private RoadPlan boundPlan(final BlockPos controller) {
        if (controller == null) {
            return null;
        }
        for (final RoadPlan plan : ClientRoadCache.roads()) {
            if (plan.controllerPos().equals(controller)) {
                return plan;
            }
        }
        return null;
    }

    private BlockPos boundController() {
        if (this.mc.player == null) {
            return null;
        }
        final ItemStack main = this.mc.player.getMainHandItem();
        if (main.is(ModRegistry.ROAD_WAND.get())) {
            final BlockPos bound = RoadWandBinding.getBoundController(main);
            if (bound != null) {
                return bound;
            }
        }
        final ItemStack offhand = this.mc.player.getOffhandItem();
        if (offhand.is(ModRegistry.ROAD_WAND.get())) {
            final BlockPos bound = RoadWandBinding.getBoundController(offhand);
            if (bound != null) {
                return bound;
            }
        }
        for (final ItemStack stack : this.mc.player.getInventory().items) {
            if (stack.is(ModRegistry.ROAD_WAND.get())) {
                final BlockPos bound = RoadWandBinding.getBoundController(stack);
                if (bound != null) {
                    return bound;
                }
            }
        }
        return null;
    }
}
