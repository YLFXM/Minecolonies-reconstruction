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
package com.YLFXM.mcreconstruction.network;

import com.YLFXM.mcreconstruction.MCReconstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenRoadPlanPayload(BlockPos controllerPos) implements CustomPacketPayload {
    public static final Type<OpenRoadPlanPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCReconstruction.MODID, "open_road_plan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRoadPlanPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBlockPos(payload.controllerPos()),
            buf -> new OpenRoadPlanPayload(buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
