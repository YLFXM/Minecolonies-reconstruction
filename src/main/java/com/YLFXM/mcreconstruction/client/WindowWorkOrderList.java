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

import com.YLFXM.mcreconstruction.MCReconstruction;
import com.YLFXM.mcreconstruction.network.CancelWorkOrdersPayload;
import com.YLFXM.mcreconstruction.network.WorkOrderListPayload;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Colony work-order list opened by the Work Order Scroll. Read-only rows plus
 * multi-select and cancel, mirroring the Road Surveyor window.
 */
public class WindowWorkOrderList extends AbstractWindowSkeleton {
    /** The single live instance, so a refreshed list updates it in place. */
    private static WindowWorkOrderList current;

    private final Set<Integer> selected = new HashSet<>();
    private final ScrollingList orderList;
    private final Text subtitle;
    private final Button cancelButton;
    private int colonyId;
    private String colonyName;
    private boolean bound;
    private boolean canManage;
    private List<WorkOrderListPayload.Entry> entries;

    /**
     * Opens the window, or refreshes the open one. The server re-sends the list
     * after a cancel, so this is both the "open" and "update" entry point.
     */
    public static void show(final int colonyId, final String colonyName,
                            final List<WorkOrderListPayload.Entry> entries,
                            final boolean bound, final boolean canManage) {
        if (current != null) {
            current.apply(colonyId, colonyName, entries, bound, canManage);
            return;
        }
        final WindowWorkOrderList window =
                new WindowWorkOrderList(colonyId, colonyName, entries, bound, canManage);
        current = window;
        window.open();
    }

    public WindowWorkOrderList(final int colonyId, final String colonyName,
                               final List<WorkOrderListPayload.Entry> entries,
                               final boolean bound, final boolean canManage) {
        super(ResourceLocation.fromNamespaceAndPath(MCReconstruction.MODID, "gui/windowworkorderlist.xml"));
        // Fields must be populated before the data provider is installed:
        // BlockUI queries getElementCount() as soon as the provider is set.
        this.colonyId = colonyId;
        this.colonyName = colonyName;
        this.bound = bound;
        this.canManage = canManage;
        this.entries = entries == null ? List.of() : entries;

        this.subtitle = this.findPaneOfTypeByID("subtitle", Text.class);
        this.orderList = this.findPaneOfTypeByID("orders", ScrollingList.class);
        this.cancelButton = this.findPaneOfTypeByID("cancelOrders", Button.class);
        // "color" alone is reused by BlockUI for the disabled state, so a
        // disabled button would look enabled; give it an explicit grey.
        this.cancelButton.setColors(0x000000, 0x9A9A9A, 0x000000);
        this.updateTitle();

        this.orderList.setDataProvider(new ScrollingList.DataProvider() {
            @Override
            public int getElementCount() {
                final List<WorkOrderListPayload.Entry> list = WindowWorkOrderList.this.entries;
                return list == null ? 0 : list.size();
            }

            @Override
            public void updateElement(final int index, final Pane row) {
                final List<WorkOrderListPayload.Entry> list = WindowWorkOrderList.this.entries;
                if (list == null || index < 0 || index >= list.size()) {
                    return;
                }
                final WorkOrderListPayload.Entry entry = list.get(index);
                final boolean checked = WindowWorkOrderList.this.selected.contains(entry.id());
                final Button select = row.findPaneOfTypeByID("select", Button.class);
                select.setText(Component.literal(checked ? "[x]" : "[ ]"));
                select.setHandler(button -> {
                    if (!WindowWorkOrderList.this.selected.add(entry.id())) {
                        WindowWorkOrderList.this.selected.remove(entry.id());
                    }
                    WindowWorkOrderList.this.refresh();
                });
                row.findPaneOfTypeByID("orderName", Text.class).setText(Component.literal(
                        "#" + entry.id() + "  " + entry.name()));
                row.findPaneOfTypeByID("orderDetail", Text.class).setText(Component.literal(
                        entry.type() + " | priority " + entry.priority() + " | " + entry.status()
                                + " | L" + entry.currentLevel() + "->" + entry.targetLevel()));
                row.findPaneOfTypeByID("orderLocation", Text.class).setText(Component.literal(
                        entry.location()));
            }
        });

        this.registerButton("selectAll", this::selectAll);
        this.registerButton("clear", this::clearSelection);
        this.registerButton("close", button -> this.close());
        this.registerButton("cancelOrders", button -> this.cancelSelected());
    }

    @Override
    public void onOpened() {
        super.onOpened();
        this.refresh();
    }

    @Override
    public void onClosed() {
        if (current == this) {
            current = null;
        }
        super.onClosed();
    }

    private void apply(final int colonyId, final String colonyName,
                       final List<WorkOrderListPayload.Entry> entries,
                       final boolean bound, final boolean canManage) {
        this.colonyId = colonyId;
        this.colonyName = colonyName;
        this.bound = bound;
        this.canManage = canManage;
        this.entries = entries == null ? List.of() : entries;
        this.updateTitle();
        this.refresh();
    }

    private void updateTitle() {
        this.findPaneOfTypeByID("title", Text.class)
                .setText(Component.literal(this.bound
                        ? "Work Orders - " + this.colonyName
                        : "Work Order Scroll"));
    }

    /** Refreshes counts and rows; only safe once the window is open. */
    private void refresh() {
        if (this.orderList == null || this.entries == null) {
            return;
        }
        this.selected.removeIf(id -> this.entries.stream().noneMatch(entry -> entry.id() == id));
        if (this.subtitle != null) {
            this.subtitle.setText(Component.literal(this.bound
                    ? this.entries.size() + " order(s) | selected " + this.selected.size()
                    : "Not bound to a town hall - shift + right-click one to bind"));
        }
        if (this.cancelButton != null) {
            // Greyed out instead of a chat prompt: the colony permission system
            // already tells the player when they may not manage this colony.
            this.cancelButton.setEnabled(this.bound && this.canManage);
        }
        this.orderList.refreshElementPanes();
    }

    private void selectAll() {
        this.selected.clear();
        for (final WorkOrderListPayload.Entry entry : this.entries) {
            this.selected.add(entry.id());
        }
        this.refresh();
    }

    private void clearSelection() {
        this.selected.clear();
        this.refresh();
    }

    private void cancelSelected() {
        if (this.selected.isEmpty()) {
            if (this.mc.player != null) {
                this.mc.player.displayClientMessage(Component.literal("Select at least one order first."), true);
            }
            return;
        }
        PacketDistributor.sendToServer(new CancelWorkOrdersPayload(this.colonyId, List.copyOf(this.selected)));
    }
}
