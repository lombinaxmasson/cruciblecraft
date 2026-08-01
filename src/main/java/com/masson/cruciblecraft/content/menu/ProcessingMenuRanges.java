package com.masson.cruciblecraft.content.menu;

import java.util.List;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

/** Pure transfer ranges shared by configured menu construction and tests. */
public record ProcessingMenuRanges(
        int machineStart,
        int machineEnd,
        int playerStart,
        int playerEnd,
        List<Integer> inputSlots) {
    public ProcessingMenuRanges {
        inputSlots = List.copyOf(inputSlots);
    }

    public static ProcessingMenuRanges forSpec(ProcessingMachineSpec spec) {
        int machineEnd = spec.items().slotCount();
        return new ProcessingMenuRanges(
                0, machineEnd, machineEnd, machineEnd + 36, spec.items().inputs());
    }
}
