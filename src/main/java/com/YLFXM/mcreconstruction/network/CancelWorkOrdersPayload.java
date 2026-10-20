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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Cancel (delete) a selection of work orders from the colony shown by the Work
 * Order Scroll. The server re-sends the work order list afterwards, so the open
 * window refreshes from authoritative data instead of trusting the client.
 */
public record CancelWorkOrdersPayload(int colonyId, List<Integer> orderIds)
        implements CustomPacketPayload {
    private static final int MAX_ORDERS = 512;

    public static final Type<CancelWorkOrdersPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCReconstruction.MODID, "cancel_work_orders"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CancelWorkOrdersPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.colonyId());
                final List<Integer> ids = payload.orderIds();
                final int size = Math.min(ids.size(), MAX_ORDERS);
                buf.writeVarInt(size);
                for (int i = 0; i < size; i++) {
                    buf.writeVarInt(ids.get(i));
                }
            },
            buf -> {
                final int colonyId = buf.readVarInt();
                final int size = Math.min(buf.readVarInt(), MAX_ORDERS);
                final List<Integer> ids = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ids.add(buf.readVarInt());
                }
                return new CancelWorkOrdersPayload(colonyId, ids);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
