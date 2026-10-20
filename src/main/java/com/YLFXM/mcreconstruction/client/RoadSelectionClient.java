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

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Client-only endpoint selection for the Road Wand.
 */
public final class RoadSelectionClient {
    private static BlockPos a = BlockPos.ZERO;
    private static BlockPos b = BlockPos.ZERO;
    private static BlockPos controllerPos = BlockPos.ZERO;
    private static boolean mirror;

    public static void setA(final BlockPos pos) {
        a = pos.immutable();
    }

    public static void setB(final BlockPos pos) {
        b = pos.immutable();
    }

    public static void setControllerPos(final BlockPos pos) {
        controllerPos = pos.immutable();
    }

    public static BlockPos controllerPos() {
        return controllerPos;
    }

    public static void setMirror(final boolean mirrored) {
        mirror = mirrored;
    }

    public static boolean mirror() {
        return mirror;
    }

    public static BlockPos a() {
        return a;
    }

    public static BlockPos b() {
        return b;
    }

    public static boolean hasBoth() {
        return !a.equals(BlockPos.ZERO) && !b.equals(BlockPos.ZERO);
    }

    public static void clear() {
        a = BlockPos.ZERO;
        b = BlockPos.ZERO;
    }

    public static void clearAll() {
        clear();
        controllerPos = BlockPos.ZERO;
        mirror = false;
    }

    public static Component summary() {
        return Component.literal("A=" + shortPos(a) + "  B=" + shortPos(b));
    }

    private static String shortPos(final BlockPos pos) {
        return pos.equals(BlockPos.ZERO) ? "-" : pos.getX() + "," + pos.getZ();
    }

    private RoadSelectionClient() {
    }
}
