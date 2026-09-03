package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Six persisted BE-side cover slots; never encoded in block state. */
public final class PipeCoverSet {
    public static final int MAX_COVERS = Direction.values().length;
    public static final int MAX_SUMMARY_LENGTH = 768;
    private static final ResourceLocation INVALID_DEFINITION =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "invalid_cover");
    private final EnumMap<Direction, PipeCover> covers =
            new EnumMap<>(Direction.class);

    public Optional<PipeCover> get(Direction side) {
        return Optional.ofNullable(covers.get(side));
    }

    public Map<Direction, PipeCover> snapshot() {
        return Map.copyOf(covers);
    }

    public List<ItemStack> removeAllAsItems() {
        java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
        for (Direction side : Direction.values()) {
            PipeCover cover = covers.remove(side);
            if (cover == null) {
                continue;
            }
            ItemStack stack = PipeCoverItems.stackFor(cover);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return List.copyOf(stacks);
    }

    public Optional<PipeCover> take(Direction side) {
        java.util.Objects.requireNonNull(side, "side");
        PipeCover previous = covers.remove(side);
        return Optional.ofNullable(previous);
    }

    public boolean set(Direction side, PipeCover cover) {
        java.util.Objects.requireNonNull(side, "side");
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
        return allowsIncoming(
                side, CoverDefinition.Medium.ITEM, 0, 0);
    }

    public boolean allowsIncoming(
            Direction side,
            CoverDefinition.Medium medium,
            int storedAmount,
            int capacity) {
        PipeCover cover = covers.get(side);
        if (cover == null) {
            return true;
        }
        CoverDefinition definition = cover.definition().orElse(null);
        if (definition == null || !definition.medium().supports(medium)) {
            return false;
        }
        return cover.behavior().allowsIncoming(
                cover,
                definition,
                new CoverBehavior.Access(
                        medium, storedAmount, capacity));
    }

    public boolean allowsOutgoing(
            Direction side,
            CoverDefinition.Medium medium,
            int storedAmount,
            int capacity) {
        PipeCover cover = covers.get(side);
        if (cover == null) {
            return true;
        }
        CoverDefinition definition = cover.definition().orElse(null);
        if (definition == null || !definition.medium().supports(medium)) {
            return false;
        }
        return cover.behavior().allowsOutgoing(
                cover,
                definition,
                new CoverBehavior.Access(
                        medium, storedAmount, capacity));
    }

    public int limitIncoming(
            Direction side,
            CoverDefinition.Medium medium,
            int storedAmount,
            int capacity,
            int requested) {
        PipeCover cover = covers.get(side);
        if (cover == null) {
            return Math.max(0, requested);
        }
        CoverDefinition definition = cover.definition().orElse(null);
        if (definition == null || !definition.medium().supports(medium)) {
            return 0;
        }
        return cover.behavior().limitIncoming(
                cover,
                definition,
                new CoverBehavior.Access(
                        medium, storedAmount, capacity),
                requested);
    }

    public boolean hasPump(Direction side) {
        return get(side)
                .flatMap(PipeCover::definition)
                .map(CoverDefinition::behaviorId)
                .map(ResourceLocation::getPath)
                .filter("pump_adapter"::equals)
                .isPresent();
    }

    public boolean matches(Direction side, ItemStack stack) {
        PipeCover cover = covers.get(side);
        if (cover == null) {
            return true;
        }
        CoverDefinition definition = cover.definition().orElse(null);
        return definition != null
                && definition.medium().supports(
                        CoverDefinition.Medium.ITEM)
                && cover.behavior().matchesItem(
                        cover, definition, stack);
    }

    public boolean matches(Direction side, FluidStack stack) {
        PipeCover cover = covers.get(side);
        if (cover == null) {
            return true;
        }
        CoverDefinition definition = cover.definition().orElse(null);
        return definition != null
                && definition.medium().supports(
                        CoverDefinition.Medium.FLUID)
                && cover.behavior().matchesFluid(
                        cover, definition, stack);
    }

    public void tick(
            Direction side, CoverBehavior.TransferContext context) {
        PipeCover cover = covers.get(side);
        if (cover == null) {
            return;
        }
        CoverDefinition definition = cover.definition().orElse(null);
        if (definition == null
                || !definition.medium().supports(context.medium())) {
            return;
        }
        cover.behavior().tick(cover, definition, context);
    }

    public boolean configure(
            Direction side,
            CoverDefinition.ConfigField field,
            int value) {
        PipeCover current = covers.get(side);
        if (current == null || current.definition().isEmpty()) {
            return false;
        }
        PipeCover changed = current.configure(field, value);
        return set(side, changed);
    }

    public String boundedSummary() {
        StringJoiner summary = new StringJoiner(", ");
        for (Direction side : Direction.values()) {
            PipeCover cover = covers.get(side);
            if (cover == null) {
                continue;
            }
            CoverDefinition definition = cover.definition().orElse(null);
            String value = side.getName() + "="
                    + (definition == null
                            ? "blocked"
                            : definition.id().getPath()
                                    + definition.resolve(cover.config()));
            if (summary.length() + value.length() + 2
                    > MAX_SUMMARY_LENGTH) {
                break;
            }
            summary.add(value);
        }
        return summary.toString();
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
            row.putString("definition", cover.definitionId().toString());
            cover.config().matchId().ifPresent(
                    id -> row.putString("match", id));
            cover.config().rate().ifPresent(
                    value -> row.putInt("rate", value));
            cover.config().pressureThreshold().ifPresent(
                    value -> row.putInt("pressure_threshold", value));
            cover.config().exactCount().ifPresent(
                    value -> row.putInt("exact_count", value));
            cover.config().mode().ifPresent(
                    value -> row.putString(
                            "mode",
                            value.name().toLowerCase(
                                    java.util.Locale.ROOT)));
            cover.config().selector().ifPresent(
                    value -> row.putInt("selector", value));
            cover.config().networkId().ifPresent(
                    value -> row.putInt("network_id", value));
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
            if (recovered.containsKey(side)) {
                recovered.put(side, invalid());
                rejectedRows++;
                continue;
            }
            try {
                boolean hasDefinition = row.contains(
                        "definition", Tag.TAG_STRING);
                boolean hasLegacyType = row.contains(
                        "type", Tag.TAG_STRING);
                if (hasDefinition == hasLegacyType) {
                    throw new IllegalArgumentException(
                            "Cover row must have exactly one identity field");
                }
                ResourceLocation definitionId;
                if (hasDefinition) {
                    definitionId = ResourceLocation.tryParse(
                            row.getString("definition"));
                    if (definitionId == null) {
                        throw new IllegalArgumentException(
                                "Invalid cover definition id");
                    }
                } else {
                    definitionId = PipeCoverType.decode(
                            row.getString("type")).definitionId();
                }
                PipeCoverConfig config = readConfig(row);
                recovered.put(side, new PipeCover(definitionId, config));
                if (CoverDefinitionCatalog.find(definitionId).isEmpty()) {
                    rejectedRows++;
                }
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                recovered.put(side, invalid());
                rejectedRows++;
            }
        }
        covers.clear();
        covers.putAll(recovered);
        return rejectedRows;
    }

    private static PipeCoverConfig readConfig(CompoundTag row) {
        Optional<String> match = row.contains("match", Tag.TAG_STRING)
                ? Optional.of(row.getString("match"))
                : Optional.empty();
        Optional<Integer> rate = row.contains("rate", Tag.TAG_INT)
                ? Optional.of(row.getInt("rate"))
                : Optional.empty();
        Optional<Integer> pressure = row.contains(
                "pressure_threshold", Tag.TAG_INT)
                ? Optional.of(row.getInt("pressure_threshold"))
                : Optional.empty();
        Optional<Integer> exact = row.contains(
                "exact_count", Tag.TAG_INT)
                ? Optional.of(row.getInt("exact_count"))
                : Optional.empty();
        Optional<CoverDefinition.TransferMode> mode = Optional.empty();
        if (row.contains("mode", Tag.TAG_STRING)) {
            mode = Optional.of(CoverDefinition.TransferMode.valueOf(
                    row.getString("mode").toUpperCase(
                            java.util.Locale.ROOT)));
        }
        Optional<Integer> selector = row.contains("selector", Tag.TAG_INT)
                ? Optional.of(row.getInt("selector"))
                : Optional.empty();
        Optional<Integer> networkId = row.contains("network_id", Tag.TAG_INT)
                ? Optional.of(row.getInt("network_id"))
                : Optional.empty();
        return new PipeCoverConfig(
                match, rate, pressure, exact, mode, selector, networkId);
    }

    private static PipeCover invalid() {
        return new PipeCover(INVALID_DEFINITION, PipeCoverConfig.EMPTY);
    }
}
