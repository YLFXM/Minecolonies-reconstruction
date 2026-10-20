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
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Opens the native build tool on a generated road blueprint.
 *
 * <p>Structurize syncs structure packs from the server only when a client joins,
 * so a blueprint generated later in the session does not exist on the client and
 * the build tool cannot load it. The blueprint NBT therefore travels with this
 * payload and the client writes it into its local copy of the pack first. The
 * pack version is sent along so the client's pack.json matches the server's and
 * the pack is not discarded by the next sync.</p>
 */
public record OpenBuildToolPayload(UUID planId, String blueprintPath,
                                   CompoundTag blueprint, double packVersion)
        implements CustomPacketPayload {
    public static final Type<OpenBuildToolPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCReconstruction.MODID, "open_build_tool"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBuildToolPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.planId());
                buf.writeUtf(payload.blueprintPath());
                buf.writeDouble(payload.packVersion());
                buf.writeNbt(payload.blueprint());
            },
            buf -> {
                final UUID planId = buf.readUUID();
                final String path = buf.readUtf();
                final double version = buf.readDouble();
                return new OpenBuildToolPayload(planId, path, buf.readNbt(), version);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
