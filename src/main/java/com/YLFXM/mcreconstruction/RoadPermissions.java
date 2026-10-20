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

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.constant.TranslationConstants;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The single place that answers the two permission questions this mod asks.
 *
 * <p>Both answers are MineColonies' own
 * {@code Permissions#hasPermission(Player, Action)}. Nothing here re-implements
 * rank logic, and there is deliberately no custom bypass: the original check
 * already lets through creative players whose permission level reaches
 * {@code permissioneventbypassminpermlevel} (default 2), and deliberately does
 * <em>not</em> let through an operator who is playing in survival.</p>
 */
public final class RoadPermissions {
    /**
     * Two different capabilities are needed, and they are not interchangeable:
     *
     * <ul>
     *   <li>{@link #canManage} — may <em>change</em> the colony (plan a road,
     *       generate a blueprint, repair, remove, cancel work orders).
     *       {@link Action#MANAGE_HUTS}.</li>
     *   <li>{@link #canView} — may <em>read</em> colony internals (see the road
     *       list, open a road's controller panel, pull a blueprint file).
     *       {@link Action#ACCESS_HUTS}.</li>
     * </ul>
     *
     * <p>{@code ACCESS_HUTS} is the original's own "may enter and use colony
     * buildings" bit. By default {@code loadRanks()} grants it to OWNER,
     * OFFICER and FRIEND (via the switch fallthrough) but not to NEUTRAL or
     * HOSTILE, which is exactly "the lord, the managers and their friends". It
     * is also the bit the original already uses to deny right-clicking a hut and
     * to deny opening the town hall GUI, so reusing it keeps this mod consistent
     * with vanilla instead of inventing a parallel permission system.</p>
     *
     * <p>There is intentionally no self-made {@code Action} for viewing: the
     * enum lives in MineColonies, cannot be extended from outside, and
     * {@code WindowPermissionsPage} builds its checkboxes from
     * {@code Action.values()} — a private extra bit would be invisible in the
     * permission GUI and therefore unconfigurable.</p>
     */

    /**
     * Ticks between two deny notices for the same player. 10 seconds, the same
     * cooldown {@code ColonyPermissionEventHandler#cancelEvent} uses.
     */
    private static final long DENY_NOTICE_COOLDOWN = Constants.TICKS_SECOND * 10L;

    /** Last game time a deny notice was shown, per player. */
    private static final Map<UUID, Long> LAST_DENY_NOTICE = new ConcurrentHashMap<>();

    /** Denials suppressed since the last notice, per player. */
    private static final Map<UUID, Integer> DENY_ATTEMPTS = new ConcurrentHashMap<>();

    /** Ignoring this many suppressions in a row earns the levitation. */
    private static final int DENY_ATTEMPTS_BEFORE_LEVITATION = 10;

    /** Levitation duration, 10 seconds like the original. */
    private static final int LEVITATION_TICKS = Constants.TICKS_SECOND * 10;

    /** May the player change things at {@code pos}? Wilderness has nothing to protect. */
    public static boolean canManage(final ServerLevel level, final ServerPlayer player,
                                    final BlockPos pos) {
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, pos);
        return canManage(player, colony);
    }

    public static boolean canManage(final ServerPlayer player, final IColony colony) {
        return colony == null || colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS);
    }

    /** May the player read colony internals at {@code pos}? */
    public static boolean canView(final ServerLevel level, final ServerPlayer player,
                                  final BlockPos pos) {
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(level, pos);
        return canView(player, colony);
    }

    public static boolean canView(final ServerPlayer player, final IColony colony) {
        return colony == null || colony.getPermissions().hasPermission(player, Action.ACCESS_HUTS);
    }

    /**
     * Reports a denial the way the original does: the vanilla
     * {@code com.minecolonies.coremod.permission.no} translation sent with
     * {@code MessageUtils}' default GRAY priority, throttled to one message per
     * player per 10 seconds.
     *
     * <p>The throttle and the levitation punishment are both copied from
     * {@code ColonyPermissionEventHandler#cancelEvent}: one message per ten
     * seconds, and ignoring more than ten denials inside that window floats the
     * player for ten seconds. Players recognised the behaviour from the original
     * and asked for it here too, so it is reproduced with the same numbers.</p>
     */
    public static void notifyDenied(final ServerPlayer player) {
        final long now = player.level().getGameTime();
        final Long last = LAST_DENY_NOTICE.get(player.getUUID());
        if (last == null || last + DENY_NOTICE_COOLDOWN < now) {
            LAST_DENY_NOTICE.put(player.getUUID(), now);
            DENY_ATTEMPTS.put(player.getUUID(), 0);
            MessageUtils.format(TranslationConstants.PERMISSION_DENIED).sendTo(player);
            return;
        }
        // Inside the cooldown: stay quiet but count the attempts, and make a
        // player who keeps hammering a denied GUI float for ten seconds, exactly
        // as ColonyPermissionEventHandler does.
        final int attempts = DENY_ATTEMPTS.merge(player.getUUID(), 1, Integer::sum);
        if (attempts > DENY_ATTEMPTS_BEFORE_LEVITATION) {
            DENY_ATTEMPTS.put(player.getUUID(), 0);
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITATION_TICKS));
        }
    }

    private RoadPermissions() {
    }
}
