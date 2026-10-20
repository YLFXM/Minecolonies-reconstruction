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
import com.YLFXM.mcreconstruction.road.RoadPlacementMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Configures a road controller. If a/b are non-zero, the server builds from
 * two endpoints; otherwise it uses the length field from the controller.
 */
public record ConfigureRoadPayload(BlockPos controllerPos, int length, int width, int lampSpacing,
                                   BlockPos a, BlockPos b, boolean mirror,
                                   RoadPlacementMode mode) implements CustomPacketPayload {
    public static final Type<ConfigureRoadPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCReconstruction.MODID, "configure_road"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureRoadPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.controllerPos());
                buf.writeInt(payload.length());
                buf.writeInt(payload.width());
                buf.writeInt(payload.lampSpacing());
                buf.writeBlockPos(payload.a());
                buf.writeBlockPos(payload.b());
                buf.writeBoolean(payload.mirror());
                buf.writeByte(payload.mode().ordinal());
            },
            buf -> new ConfigureRoadPayload(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readBlockPos(), buf.readBlockPos(), buf.readBoolean(),
                    RoadPlacementMode.values()[Math.max(0,
                            Math.min(buf.readUnsignedByte(), RoadPlacementMode.values().length - 1))]));

    public boolean hasEndpoints() {
        return !a.equals(BlockPos.ZERO) && !b.equals(BlockPos.ZERO);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
