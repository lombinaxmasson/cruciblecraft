package com.masson.cruciblecraft.machine.processing;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Immutable registry-safe machine shape and policy. */
public record ProcessingMachineSpec(
        ResourceLocation id,
        ResourceLocation recipeMapId,
        Supplier<RecipeMap> recipeMap,
        SlotLayout items,
        TankLayout fluids,
        EnergySpec energy,
        SidedIoPolicy sidedIo,
        ExecutionValidator validator,
        BufferPolicy buffering,
        UiLayout ui) {

    public ProcessingMachineSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(recipeMapId, "recipeMapId");
        Objects.requireNonNull(recipeMap, "recipeMap");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(fluids, "fluids");
        Objects.requireNonNull(energy, "energy");
        Objects.requireNonNull(sidedIo, "sidedIo");
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(buffering, "buffering");
        Objects.requireNonNull(ui, "ui");
        if (ui.machineSlots().size() != items.slotCount()) {
            throw new IllegalArgumentException("UI must position every machine slot");
        }
    }

    public RecipeMap requireRecipeMap() {
        RecipeMap map = Objects.requireNonNull(recipeMap.get(), "recipeMap supplier");
        if (!recipeMapId.equals(map.id())) {
            throw new IllegalStateException(
                    "Spec " + id + " expected map " + recipeMapId + " but received " + map.id());
        }
        return map;
    }

    public record SlotLayout(int slotCount, List<Integer> inputs, List<Integer> outputs) {
        public SlotLayout {
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
            if (slotCount < 0) {
                throw new IllegalArgumentException("Slot count must not be negative");
            }
            validateIndexes("input slot", slotCount, inputs);
            validateIndexes("output slot", slotCount, outputs);
            Set<Integer> overlap = new HashSet<>(inputs);
            overlap.retainAll(outputs);
            if (!overlap.isEmpty()) {
                throw new IllegalArgumentException("Input/output slots overlap: " + overlap);
            }
        }
    }

    public record TankLayout(List<TankSpec> inputs, List<TankSpec> outputs) {
        public TankLayout {
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
            Set<Integer> indexes = new HashSet<>();
            for (TankSpec tank : inputs) {
                if (!indexes.add(tank.index())) {
                    throw new IllegalArgumentException("Duplicate fluid tank " + tank.index());
                }
            }
            for (TankSpec tank : outputs) {
                if (!indexes.add(tank.index())) {
                    throw new IllegalArgumentException("Input/output tanks overlap at " + tank.index());
                }
            }
            int expected = 0;
            for (int index : indexes.stream().sorted().toList()) {
                if (index != expected++) {
                    throw new IllegalArgumentException("Fluid tank indexes must be contiguous from zero");
                }
            }
        }

        public int tankCount() {
            return inputs.size() + outputs.size();
        }

        public List<TankSpec> all() {
            return java.util.stream.Stream.concat(inputs.stream(), outputs.stream()).toList();
        }
    }

    public record TankSpec(int index, int capacity) {
        public TankSpec {
            if (index < 0 || capacity <= 0) {
                throw new IllegalArgumentException("Tank index/capacity must be positive");
            }
        }
    }

    public enum EnergyMode {
        BUFFERED,
        ADJACENT
    }

    public record EnergySpec(
            EnergyType type,
            EnergyMode mode,
            long capacity,
            long maxPacket) {
        public EnergySpec {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(mode, "mode");
            if (capacity < 0L || maxPacket <= 0L) {
                throw new IllegalArgumentException("Energy capacity/max packet are invalid");
            }
            if (mode == EnergyMode.BUFFERED && capacity == 0L) {
                throw new IllegalArgumentException("Buffered energy requires positive capacity");
            }
        }
    }

    @FunctionalInterface
    public interface SideRule {
        CapabilityAccess resolve(Direction front, Direction side);
    }

    public enum CapabilityAccess {
        NONE,
        INPUT,
        OUTPUT
    }

    public record SidedIoPolicy(
            SideRule items,
            SideRule fluids,
            SideRule energy) {
        public SidedIoPolicy {
            Objects.requireNonNull(items, "items");
            Objects.requireNonNull(fluids, "fluids");
            Objects.requireNonNull(energy, "energy");
        }
    }

    @FunctionalInterface
    public interface ExecutionValidator {
        Optional<String> validate(GTRecipe recipe);
    }

    public enum BufferPolicy {
        PAUSE,
        RESET_ON_BLOCK
    }

    public record UiLayout(
            List<SlotPosition> machineSlots,
            ProgressBar progress,
            List<TankPosition> tanks,
            List<String> statuses) {
        public UiLayout {
            machineSlots = List.copyOf(machineSlots);
            tanks = List.copyOf(tanks);
            statuses = List.copyOf(statuses);
            Objects.requireNonNull(progress, "progress");
        }
    }

    public record SlotPosition(int x, int y) {}
    public record TankPosition(int tank, int x, int y, int width, int height) {
        public TankPosition {
            if (tank < 0 || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Invalid tank UI position");
            }
        }
    }
    public record ProgressBar(int x, int y, int width, int height) {
        public ProgressBar {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Invalid progress bar");
            }
        }
    }

    private static void validateIndexes(String name, int size, List<Integer> indexes) {
        Set<Integer> unique = new HashSet<>();
        for (int index : indexes) {
            if (index < 0 || index >= size) {
                throw new IllegalArgumentException(name + " out of range: " + index);
            }
            if (!unique.add(index)) {
                throw new IllegalArgumentException("Duplicate " + name + ": " + index);
            }
        }
    }
}
