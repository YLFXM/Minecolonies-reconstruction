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

import com.mojang.logging.LogUtils;
import com.YLFXM.mcreconstruction.network.Network;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * MineColonies: Reconstruction Plan.
 *
 * <p>Feature collection for MineColonies: custom roads/intersections, terrain
 * modification, decoration control and pathfinding integration. This mod is
 * independent from Pause Research; a future optional module may merge the
 * pause-research behaviour.</p>
 */
@Mod(MCReconstruction.MODID)
public final class MCReconstruction {
    public static final String MODID = "mcreconstruction";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MCReconstruction(final IEventBus modEventBus, final ModContainer container) {
        ModRegistry.register(modEventBus);
        modEventBus.addListener(Network::register);
        RoadAnchorSurvivalHandler.register();
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
