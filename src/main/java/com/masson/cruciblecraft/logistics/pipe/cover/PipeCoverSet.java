package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Six persisted BE-side cover slots; never encoded in block state. */
public final class PipeCoverSet {
    private final EnumMap<Direction, PipeCover> covers =
            new EnumMap<>(Direction.class);

    public Optional<PipeCover> get(Direction side) {
        return Optional.ofNullable(covers.get(side));
    }

    public Map<Direction, PipeCover> snapshot() {
        return Map.copyOf(covers);
    }

    public boolean set(Direction side, PipeCover cover) {
        PipeCover previous;
        if (cover == null) {
            previous = covers.remove(side);
        } else {
            previous = covers.put(side, cover);
        }
        if (!java.util.Objects.equals(previous, cover)) {
            return true;
        }
        return false;
    }

    /** A valve permits pipe-to-face output and rejects face-to-pipe input. */
    public boolean allowsIncoming(Direction side) {
        return get(side)
                .map(PipeCover::type)
                .orElse(null) != PipeCoverType.ONE_WAY_VALVE;
    }

    public boolean hasPump(Direction side) {
        return get(side)
                .map(PipeCover::type)
                .orElse(null) == PipeCoverType.OUTPUT_PUMP;
    }

    public boolean matches(Direction side, ItemStack stack) {
        Optional<PipeCover> cover = get(side);
        if (cover.isEmpty()
                || cover.orElseThrow().type() != PipeCoverType.FILTER) {
            return true;
        }
        Optional<String> expected = cover.orElseThrow().matchId();
        return expected.isEmpty()
                || expected.orElseThrow().equals(
                        BuiltInRegistries.ITEM.getKey(stack.getItem())
                                .toString());
    }

    public boolean matches(Direction side, FluidStack stack) {
        Optional<PipeCover> cover = get(side);
        if (cover.isEmpty()
                || cover.orElseThrow().type() != PipeCoverType.FILTER) {
            return true;
        }
        Optional<String> expected = cover.orElseThrow().matchId();
        return expected.isEmpty()
                || expected.orElseThrow().equals(
                        BuiltInRegistries.FLUID.getKey(stack.getFluid())
                                .toString());
    }

    public void save(
            CompoundTag target, HolderLookup.Provider registries) {
        ListTag rows = new ListTag();
        for (Direction side : Direction.values()) {
            PipeCover cover = covers.get(side);
            if (cover == null) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putString("side", side.getName());
            row.putString("type", cover.type().id());
            cover.matchId().ifPresent(id -> row.putString("match", id));
            rows.add(row);
        }
        target.put("covers", rows);
    }

    public int load(
            CompoundTag source, HolderLookup.Provider registries) {
        EnumMap<Direction, PipeCover> recovered =
                new EnumMap<>(Direction.class);
        int rejectedRows = 0;
        ListTag rows = source.getList("covers", Tag.TAG_COMPOUND);
        for (int index = 0; index < rows.size(); index++) {
            CompoundTag row = rows.getCompound(index);
            Direction side = Direction.byName(row.getString("side"));
            if (side == null) {
                rejectedRows++;
                continue;
            }
            try {
                PipeCoverType type = PipeCoverType.decode(
                        row.getString("type"));
                Optional<String> match = row.contains("match")
                        ? Optional.of(row.getString("match"))
                        : Optional.empty();
                recovered.put(side, new PipeCover(type, match));
            } catch (IllegalArgumentException ignored) {
                rejectedRows++;
            }
        }
        covers.clear();
        covers.putAll(recovered);
        return rejectedRows;
    }
}
