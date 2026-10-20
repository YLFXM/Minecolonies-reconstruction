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

public record WorkOrderListPayload(int colonyId, String colonyName, List<Entry> entries,
                                   boolean bound, boolean canManage) implements CustomPacketPayload {
    public record Entry(int id, String type, String name, String location, String status,
                        int priority, int currentLevel, int targetLevel) {
    }

    public static final Type<WorkOrderListPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCReconstruction.MODID, "work_order_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkOrderListPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.bound());
                buf.writeBoolean(payload.canManage());
                buf.writeVarInt(payload.colonyId());
                buf.writeUtf(payload.colonyName());
                buf.writeVarInt(payload.entries().size());
                for (final Entry entry : payload.entries()) {
                    buf.writeVarInt(entry.id());
                    buf.writeUtf(entry.type());
                    buf.writeUtf(entry.name());
                    buf.writeUtf(entry.location());
                    buf.writeUtf(entry.status());
                    buf.writeVarInt(entry.priority());
                    buf.writeVarInt(entry.currentLevel());
                    buf.writeVarInt(entry.targetLevel());
                }
            },
            buf -> {
                final boolean bound = buf.readBoolean();
                final boolean canManage = buf.readBoolean();
                final int colonyId = buf.readVarInt();
                final String colonyName = buf.readUtf();
                final int size = buf.readVarInt();
                final List<Entry> entries = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    entries.add(new Entry(buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                            buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
                }
                return new WorkOrderListPayload(colonyId, colonyName, entries, bound, canManage);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
