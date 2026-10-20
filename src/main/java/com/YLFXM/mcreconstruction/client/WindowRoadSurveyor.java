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

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.YLFXM.mcreconstruction.MCReconstruction;
import com.YLFXM.mcreconstruction.network.ClientRoadCache;
import com.YLFXM.mcreconstruction.network.RoadBulkActionPayload;
import com.YLFXM.mcreconstruction.road.RoadNames;
import com.YLFXM.mcreconstruction.road.RoadPlan;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * BlockUI version of the Road Surveyor window, matching MineColonies styling.
 */
public class WindowRoadSurveyor extends AbstractWindowSkeleton {
    private final Set<UUID> selected = new HashSet<>();
    private final ScrollingList roadList;
    private final Text titleText;
    private final Text selectedText;
    private final Button releaseButton;
    private final Button removeButton;
    private int lastCount = -1;
    private int lastManageable = -1;
    private boolean lastBound;

    public WindowRoadSurveyor() {
        super(ResourceLocation.fromNamespaceAndPath(MCReconstruction.MODID, "gui/windowroadsurveyor.xml"));
        this.roadList = this.findPaneOfTypeByID("roads", ScrollingList.class);
        this.titleText = this.findPaneOfTypeByID("title", Text.class);
        this.selectedText = this.findPaneOfTypeByID("selected", Text.class);
        this.releaseButton = this.findPaneOfTypeByID("release", Button.class);
        this.removeButton = this.findPaneOfTypeByID("remove", Button.class);
        // "color" alone makes BlockUI reuse it for the disabled state, so set an
        // explicit grey or a disabled button looks exactly like an enabled one.
        this.releaseButton.setColors(0x000000, 0x9A9A9A, 0x000000);
        this.removeButton.setColors(0x000000, 0x9A9A9A, 0x000000);
        this.roadList.setDataProvider(new ScrollingList.DataProvider() {
            @Override
            public int getElementCount() {
                return ClientRoadCache.roads().size();
            }

            @Override
            public void updateElement(final int index, final Pane row) {
                final List<RoadPlan> roads = ClientRoadCache.roads();
                if (index < 0 || index >= roads.size()) {
                    return;
                }
                final RoadPlan plan = roads.get(index);
                final boolean manageable = ClientRoadCache.canManage(plan.id());
                final boolean checked = selected.contains(plan.id());
                final Button select = row.findPaneOfTypeByID("select", Button.class);
                // Per-row permission: roads in colonies the player cannot manage
                // stay listed for information but cannot be selected.
                select.setText(Component.literal(!manageable ? "[-]" : checked ? "[x]" : "[ ]"));
                select.setEnabled(manageable);
                select.setColors(0x000000, 0x9A9A9A, 0x000000);
                select.setHandler(button -> {
                    if (!selected.add(plan.id())) {
                        selected.remove(plan.id());
                    }
                    refreshRows();
                });
                final Text nameText = row.findPaneOfTypeByID("roadName", Text.class);
                nameText.setText(Component.literal(RoadNames.label(plan)));
                nameText.setColors(manageable ? 0x000000 : 0x8A8A8A);
                final Text detailText = row.findPaneOfTypeByID("roadDetail", Text.class);
                detailText.setText(Component.literal(
                        plan.state() + " | len " + plan.length() + " | " + plan.controllerPos().toShortString()));
                detailText.setColors(manageable ? 0x000000 : 0x8A8A8A);
            }
        });

        this.registerButton("selectAll", this::selectAll);
        this.registerButton("clear", this::clearSelection);
        this.registerButton("close", button -> this.close());
        this.registerButton("release", button -> this.send(false));
        this.registerButton("remove", button -> this.send(true));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        final List<RoadPlan> roads = ClientRoadCache.roads();
        final int manageable = (int) roads.stream()
                .filter(plan -> ClientRoadCache.canManage(plan.id())).count();
        if (roads.size() != this.lastCount || manageable != this.lastManageable
                || ClientRoadCache.bound() != this.lastBound) {
            this.refreshRows();
        }
    }

    @Override
    public void onOpened() {
        super.onOpened();
        this.refreshRows();
    }

    private void selectAll() {
        this.selected.clear();
        for (final RoadPlan plan : ClientRoadCache.roads()) {
            // Only roads this player may actually manage can be selected.
            if (ClientRoadCache.canManage(plan.id())) {
                this.selected.add(plan.id());
            }
        }
        this.refreshRows();
    }

    private void clearSelection() {
        this.selected.clear();
        this.refreshRows();
    }

    private void refreshRows() {
        final List<RoadPlan> roads = ClientRoadCache.roads();
        // Drop selections that vanished or that the player may no longer manage.
        this.selected.removeIf(id -> !ClientRoadCache.canManage(id)
                || roads.stream().noneMatch(plan -> plan.id().equals(id)));
        this.titleText.setText(Component.literal(ClientRoadCache.bound()
                ? "Road Surveyor - " + ClientRoadCache.colonyName()
                : "Road Surveyor"));
        this.selectedText.setText(Component.literal(ClientRoadCache.bound()
                ? "Selected: " + this.selected.size()
                : "Not bound to a town hall - shift + right-click one to bind"));
        // Only offer the actions when at least one selected road is manageable;
        // the server still checks each road individually.
        final boolean canAct = !this.selected.isEmpty()
                && this.selected.stream().anyMatch(ClientRoadCache::canManage);
        this.releaseButton.setEnabled(canAct);
        this.removeButton.setEnabled(canAct);
        this.roadList.refreshElementPanes();
        this.lastCount = roads.size();
        this.lastBound = ClientRoadCache.bound();
        this.lastManageable = (int) roads.stream()
                .filter(plan -> ClientRoadCache.canManage(plan.id())).count();
    }

    private void send(final boolean removeBlocks) {
        if (this.selected.isEmpty()) {
            if (this.mc.player != null) {
                this.mc.player.displayClientMessage(Component.literal("Select at least one road first."), true);
            }
            return;
        }
        PacketDistributor.sendToServer(new RoadBulkActionPayload(List.copyOf(this.selected), removeBlocks));
    }
}
