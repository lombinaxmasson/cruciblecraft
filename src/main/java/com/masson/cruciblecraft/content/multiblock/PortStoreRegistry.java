package com.masson.cruciblecraft.content.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;

/**
 * Runtime index of the independent stores belonging to one formed controller.
 *
 * <p>The physical block entity owns persistence. This registry is only a
 * loaded-world index used by processing hosts to aggregate inputs and route
 * outputs.</p>
 */
public final class PortStoreRegistry {
    private static final Map<
            MultiblockPortHost,
            LinkedHashMap<BlockPos, PortStoreCarrier>> STORES =
            new WeakHashMap<>();

    private PortStoreRegistry() {}

    public static synchronized void bind(
            MultiblockPortHost host,
            BlockPos position,
            PortStoreCarrier carrier) {
        STORES.computeIfAbsent(host, unused -> new LinkedHashMap<>())
                .put(position.immutable(), carrier);
    }

    public static synchronized void unbind(
            MultiblockPortHost host,
            BlockPos position,
            PortStoreCarrier carrier) {
        Map<BlockPos, PortStoreCarrier> entries = STORES.get(host);
        if (entries == null) {
            return;
        }
        PortStoreCarrier current = entries.get(position);
        if (current == carrier) {
            entries.remove(position);
        }
        if (entries.isEmpty()) {
            STORES.remove(host);
        }
    }

    public static synchronized List<PortStoreCarrier> stores(
            MultiblockPortHost host) {
        Map<BlockPos, PortStoreCarrier> entries = STORES.get(host);
        return entries == null
                ? List.of()
                : List.copyOf(entries.values());
    }

    public static synchronized void clear(MultiblockPortHost host) {
        STORES.remove(host);
    }

    public static Map<BlockPos, PortStore.Assignment> assignments(
            List<MultiblockStructureValidator.MatchedPort> ports,
            MultiblockPortHost host) {
        Map<BlockPos, MutableAssignment> mutable = new LinkedHashMap<>();
        List<BlockPos> itemInputs = ports.stream()
                .filter(port -> PortCapabilityGate.itemInsert(port.type()))
                .map(MultiblockStructureValidator.MatchedPort::position)
                .toList();
        List<BlockPos> itemOutputs = ports.stream()
                .filter(port -> PortCapabilityGate.itemExtract(port.type()))
                .map(MultiblockStructureValidator.MatchedPort::position)
                .toList();
        List<BlockPos> fluidInputs = ports.stream()
                .filter(port -> PortCapabilityGate.fluidFill(port.type()))
                .map(MultiblockStructureValidator.MatchedPort::position)
                .toList();
        List<BlockPos> fluidOutputs = ports.stream()
                .filter(port -> PortCapabilityGate.fluidDrain(port.type()))
                .map(MultiblockStructureValidator.MatchedPort::position)
                .toList();
        ports.forEach(port -> mutable.computeIfAbsent(
                port.position().immutable(),
                unused -> new MutableAssignment()));

        assignItems(host.itemInputSlots(), itemInputs, mutable, true);
        assignItems(host.itemOutputSlots(), itemOutputs, mutable, false);
        assignFluids(host.fluidInputTanks(), fluidInputs, mutable, true);
        assignFluids(host.fluidOutputTanks(), fluidOutputs, mutable, false);

        Map<BlockPos, PortStore.Assignment> result = new LinkedHashMap<>();
        mutable.forEach((position, value) -> result.put(
                position,
                new PortStore.Assignment(
                        value.itemInputs,
                        value.itemOutputs,
                        value.fluidInputs,
                        value.fluidOutputs)));
        return result;
    }

    private static void assignItems(
            List<Integer> globals,
            List<BlockPos> ports,
            Map<BlockPos, MutableAssignment> assignments,
            boolean input) {
        if (input) {
            for (BlockPos port : ports) {
                assignments.get(port).itemInputs.addAll(globals);
            }
            return;
        }
        for (int index = 0; index < globals.size() && !ports.isEmpty(); index++) {
            MutableAssignment assignment =
                    assignments.get(ports.get(index % ports.size()));
            (input ? assignment.itemInputs : assignment.itemOutputs)
                    .add(globals.get(index));
        }
    }

    private static void assignFluids(
            List<Integer> globals,
            List<BlockPos> ports,
            Map<BlockPos, MutableAssignment> assignments,
            boolean input) {
        if (input) {
            for (BlockPos port : ports) {
                assignments.get(port).fluidInputs.addAll(globals);
            }
            return;
        }
        for (int index = 0; index < globals.size() && !ports.isEmpty(); index++) {
            MutableAssignment assignment =
                    assignments.get(ports.get(index % ports.size()));
            (input ? assignment.fluidInputs : assignment.fluidOutputs)
                    .add(globals.get(index));
        }
    }

    private static final class MutableAssignment {
        private final List<Integer> itemInputs = new ArrayList<>();
        private final List<Integer> itemOutputs = new ArrayList<>();
        private final List<Integer> fluidInputs = new ArrayList<>();
        private final List<Integer> fluidOutputs = new ArrayList<>();
    }
}
