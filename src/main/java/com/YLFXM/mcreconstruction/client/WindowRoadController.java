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
import com.YLFXM.mcreconstruction.MCReconstruction;
import com.YLFXM.mcreconstruction.network.ClientRoadCache;
import com.YLFXM.mcreconstruction.network.RoadControllerActionPayload;
import com.YLFXM.mcreconstruction.road.RoadNames;
import com.YLFXM.mcreconstruction.road.RoadPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Post-build controller panel: Upgrade / Repair / Remove only. Construction
 * options belong to the Road Wand / native build tool, not this window.
 */
public class WindowRoadController extends AbstractWindowSkeleton {
    private final BlockPos controllerPos;
    private final Text nameText;
    private final Text stateText;
    private final Button upgradeButton;
    private final Button repairButton;
    private final Button removeButton;

    public WindowRoadController(final BlockPos controllerPos) {
        super(ResourceLocation.fromNamespaceAndPath(MCReconstruction.MODID, "gui/windowroadcontroller.xml"));
        this.controllerPos = controllerPos;
        this.nameText = this.findPaneOfTypeByID("name", Text.class);
        this.stateText = this.findPaneOfTypeByID("state", Text.class);
        this.upgradeButton = this.findPaneOfTypeByID("upgrade", Button.class);
        this.repairButton = this.findPaneOfTypeByID("repair", Button.class);
        this.removeButton = this.findPaneOfTypeByID("remove", Button.class);
        // BlockUI derives the disabled colour from "color" when only that is set,
        // so disabled buttons would look identical to enabled ones. Force a grey.
        for (final Button button : new Button[]{this.upgradeButton, this.repairButton, this.removeButton}) {
            button.setColors(0x000000, 0x9A9A9A, 0x000000);
        }

        this.registerButton("upgrade", button -> this.send(RoadControllerActionPayload.Action.UPGRADE));
        this.registerButton("repair", button -> this.send(RoadControllerActionPayload.Action.REPAIR));
        this.registerButton("remove", button -> this.send(RoadControllerActionPayload.Action.REMOVE));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        final RoadPlan plan = this.plan();
        this.nameText.setText(Component.literal(plan == null ? "No plan" : RoadNames.label(plan)));
        this.stateText.setText(Component.literal(plan == null ? "" : plan.state().toString()));
        final boolean canManage = plan != null && ClientRoadCache.canManage(plan.id());
        this.upgradeButton.setEnabled(false);
        this.repairButton.setEnabled(canManage);
        this.removeButton.setEnabled(canManage);
    }

    private void send(final RoadControllerActionPayload.Action action) {
        PacketDistributor.sendToServer(new RoadControllerActionPayload(this.controllerPos, action));
        this.close();
    }

    private RoadPlan plan() {
        for (final RoadPlan plan : ClientRoadCache.roads()) {
            if (plan.controllerPos().equals(this.controllerPos)) {
                return plan;
            }
        }
        return null;
    }
}
