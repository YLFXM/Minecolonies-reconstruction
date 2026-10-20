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
package com.YLFXM.mcreconstruction.road;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Persistent road plan. The controller block entity owns this data; a
 * WorkOrder only carries the controller position and a synthetic structure
 * path.
 */
public final class RoadPlan {
    private UUID id;
    private ResourceKey<Level> dimension;
    private BlockPos anchorPos;
    private BlockPos controllerPos;
    private Direction controllerFacing;
    private boolean mirror;
    private String pack;
    private String variant;
    private String displayName = "";
    private int width;
    private int walkWidth;
    private int widthSide;
    private int lampSpacing;
    private final List<RoadSegment> segments = new ArrayList<>();
    private RoadState state;
    private int workOrderId;
    /**
     * Player who planned this road. Wilderness roads are only visible to their
     * creator, so this has to survive a save/load. Older saves have no such tag
     * and load as {@code null}, which means "nobody" — only colony managers can
     * see the road then.
     */
    private UUID creator;

    public RoadPlan(final UUID id,
                    final ResourceKey<Level> dimension,
                    final BlockPos controllerPos,
                    final Direction controllerFacing,
                    final boolean mirror,
                    final String pack,
                    final String variant,
                    final int width,
                    final int walkWidth,
                    final int widthSide,
                    final int lampSpacing) {
        this(id, dimension, controllerPos, controllerPos, controllerFacing, mirror, pack, variant,
                width, walkWidth, widthSide, lampSpacing);
    }

    public RoadPlan(final UUID id,
                    final ResourceKey<Level> dimension,
                    final BlockPos anchorPos,
                    final BlockPos controllerPos,
                    final Direction controllerFacing,
                    final boolean mirror,
                    final String pack,
                    final String variant,
                    final int width,
                    final int walkWidth,
                    final int widthSide,
                    final int lampSpacing) {
        this.id = id;
        this.dimension = dimension;
        this.anchorPos = anchorPos;
        this.controllerPos = controllerPos;
        this.controllerFacing = controllerFacing;
        this.mirror = mirror;
        this.pack = pack;
        this.variant = variant;
        this.width = Math.max(1, width);
        this.walkWidth = Math.max(1, Math.min(walkWidth, this.width));
        this.widthSide = widthSide >= 0 ? 1 : -1;
        this.lampSpacing = Math.max(0, lampSpacing);
        this.state = RoadState.DRAFT;
    }

    public static RoadPlan createDraft(final ResourceKey<Level> dimension,
                                       final BlockPos controllerPos,
                                       final Direction facing,
                                       final String pack,
                                       final String variant,
                                       final int width,
                                       final int walkWidth,
                                       final int widthSide,
                                       final int lampSpacing) {
        return new RoadPlan(UUID.randomUUID(), dimension, controllerPos, facing, false, pack, variant,
                width, walkWidth, widthSide, lampSpacing);
    }

    public UUID id() {
        return id;
    }

    /** Player who planned this road, or {@code null} for pre-existing saves. */
    public UUID creator() {
        return creator;
    }

    public void setCreator(final UUID creator) {
        this.creator = creator;
    }

    public boolean isCreatedBy(final UUID player) {
        return creator != null && creator.equals(player);
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public void setControllerPos(final BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    /** Visible marker block that carries the road anchor. */
    public BlockPos anchorPos() {
        return anchorPos;
    }

    public void setAnchorPos(final BlockPos anchorPos) {
        this.anchorPos = anchorPos;
    }

    public BlockPos controllerPos() {
        return controllerPos;
    }

    public Direction controllerFacing() {
        return controllerFacing;
    }

    public boolean mirror() {
        return mirror;
    }

    public void setMirror(final boolean mirror) {
        this.mirror = mirror;
        this.widthSide = mirror ? -Math.abs(widthSide) : Math.abs(widthSide);
    }

    public String pack() {
        return pack;
    }

    public void setPack(final String pack) {
        this.pack = pack;
    }

    public String variant() {
        return variant;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName == null ? "" : displayName;
    }

    public void setVariant(final String variant) {
        this.variant = variant;
    }

    public int width() {
        return width;
    }

    public int walkWidth() {
        return walkWidth;
    }

    public int widthSide() {
        return widthSide;
    }

    public int lampSpacing() {
        return lampSpacing;
    }

    public void setLampSpacing(final int lampSpacing) {
        this.lampSpacing = Math.max(0, lampSpacing);
    }

    public List<RoadSegment> segments() {
        return Collections.unmodifiableList(segments);
    }

    public void addSegment(final RoadSegment segment) {
        if (!segments.isEmpty()) {
            final RoadSegment last = segments.get(segments.size() - 1);
            if (last.axis() != segment.axis()) {
                throw new IllegalArgumentException("road segments must share one axis");
            }
        }
        segments.add(segment);
    }

    public RoadState state() {
        return state;
    }

    public void setState(final RoadState state) {
        this.state = state;
    }

    public int workOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(final int workOrderId) {
        this.workOrderId = workOrderId;
    }

    public int length() {
        int length = 0;
        for (final RoadSegment segment : segments) {
            length += segment.length();
        }
        return length;
    }

    public Direction.Axis axis() {
        return segments.isEmpty() ? Direction.Axis.X : segments.get(0).axis();
    }

    public RoadBounds bounds(final int lampReserveHeight) {
        return RoadBounds.of(this, lampReserveHeight);
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("dimension", dimension.location().toString());
        tag.put("anchor", NbtUtils.writeBlockPos(anchorPos));
        tag.put("controller", NbtUtils.writeBlockPos(controllerPos));
        tag.putInt("facing", controllerFacing.get3DDataValue());
        tag.putBoolean("mirror", mirror);
        tag.putString("pack", pack);
        tag.putString("variant", variant);
        tag.putString("displayName", displayName);
        tag.putInt("width", width);
        tag.putInt("walkWidth", walkWidth);
        tag.putInt("widthSide", widthSide);
        tag.putInt("lampSpacing", lampSpacing);
        tag.putString("state", state.name());
        tag.putInt("workOrderId", workOrderId);
        if (creator != null) {
            tag.putUUID("creator", creator);
        }

        final ListTag list = new ListTag();
        for (final RoadSegment segment : segments) {
            list.add(segment.save());
        }
        tag.put("segments", list);
        return tag;
    }

    public static RoadPlan load(final CompoundTag tag) {
        final UUID id = tag.hasUUID("id") ? tag.getUUID("id") : UUID.randomUUID();
        final ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("dimension")));
        final BlockPos controllerPos = NbtUtils.readBlockPos(tag, "controller").orElse(BlockPos.ZERO);
        final BlockPos anchorPos = NbtUtils.readBlockPos(tag, "anchor").orElse(controllerPos);
        final Direction facing = Direction.from3DDataValue(tag.getInt("facing"));
        final RoadPlan plan = new RoadPlan(id, dimension, anchorPos, controllerPos, facing,
                tag.getBoolean("mirror"),
                tag.getString("pack"),
                tag.getString("variant"),
                Math.max(1, tag.getInt("width")),
                Math.max(1, tag.getInt("walkWidth")),
                tag.getInt("widthSide"),
                tag.getInt("lampSpacing"));
        try {
            plan.setState(RoadState.valueOf(tag.getString("state")));
        } catch (final IllegalArgumentException ex) {
            plan.setState(RoadState.DRAFT);
        }
        plan.setWorkOrderId(tag.getInt("workOrderId"));
        plan.setDisplayName(tag.getString("displayName"));
        if (tag.hasUUID("creator")) {
            plan.setCreator(tag.getUUID("creator"));
        }

        final ListTag list = tag.getList("segments", 10);
        for (int i = 0; i < list.size(); i++) {
            plan.addSegment(RoadSegment.load(list.getCompound(i)));
        }
        return plan;
    }
}
