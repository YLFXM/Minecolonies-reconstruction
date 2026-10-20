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

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MCReconstruction.MODID);

    public static final DeferredItem<ItemRoadSurveyor> ROAD_SURVEYOR =
            ITEMS.register("road_surveyor",
                    () -> new ItemRoadSurveyor(new net.minecraft.world.item.Item.Properties()
                            .stacksTo(1)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredItem<ItemRoadWand> ROAD_WAND =
            ITEMS.register("road_wand",
                    () -> new ItemRoadWand(new net.minecraft.world.item.Item.Properties()
                            .stacksTo(1)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredItem<ItemWorkOrderScroll> WORK_ORDER_SCROLL =
            ITEMS.register("work_order_scroll",
                    () -> new ItemWorkOrderScroll(new net.minecraft.world.item.Item.Properties()
                            .stacksTo(1)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    /** Dedicated creative tab so all mod items are in one place. */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MCReconstruction.MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB =
            CREATIVE_TABS.register("mcreconstruction", () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.mcreconstruction"))
                    .icon(() -> new net.minecraft.world.item.ItemStack(ROAD_WAND.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ROAD_WAND.get());
                        output.accept(ROAD_SURVEYOR.get());
                        output.accept(WORK_ORDER_SCROLL.get());
                    })
                    .build());

    public static void register(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }

    private ModRegistry() {
    }
}
