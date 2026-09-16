package com.masson.cruciblecraft.machine.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** GT6 {@code addToolTipsSided} lines for processing-machine items. */
public final class ProcessingMachineIoTooltips {
    private ProcessingMachineIoTooltips() {}

    public static List<Component> lines(ProcessingMachineSpec spec) {
        Objects.requireNonNull(spec, "spec");
        List<Component> lines = new ArrayList<>();
        appendEnergy(lines, spec);
        if (!spec.items().inputs().isEmpty()) {
            appendChannel(
                    lines,
                    spec.sidedIo().itemsChannel(),
                    true,
                    "tooltip.cruciblecraft.machine.items_in",
                    ChatFormatting.GREEN);
        }
        if (!spec.items().outputs().isEmpty()) {
            appendChannel(
                    lines,
                    spec.sidedIo().itemsChannel(),
                    false,
                    "tooltip.cruciblecraft.machine.items_out",
                    ChatFormatting.RED);
        }
        if (!spec.fluids().inputs().isEmpty()) {
            appendChannel(
                    lines,
                    spec.sidedIo().fluidsChannel(),
                    true,
                    "tooltip.cruciblecraft.machine.fluids_in",
                    ChatFormatting.GREEN);
        }
        if (!spec.fluids().outputs().isEmpty()) {
            appendChannel(
                    lines,
                    spec.sidedIo().fluidsChannel(),
                    false,
                    "tooltip.cruciblecraft.machine.fluids_out",
                    ChatFormatting.RED);
        }
        lines.add(gray("tooltip.cruciblecraft.machine.screwdriver"));
        IoChannel items = spec.sidedIo().itemsChannel();
        IoChannel fluids = spec.sidedIo().fluidsChannel();
        if (items.hasAutoInput() || fluids.hasAutoInput()) {
            lines.add(gray("tooltip.cruciblecraft.machine.monkey_wrench.auto_in"));
        }
        if (items.hasAutoOutput() || fluids.hasAutoOutput()) {
            lines.add(gray("tooltip.cruciblecraft.machine.monkey_wrench.auto_out"));
        }
        return List.copyOf(lines);
    }

    private static void appendEnergy(List<Component> lines, ProcessingMachineSpec spec) {
        if (spec.energy().type() == EnergyType.TIME) {
            return;
        }
        IoChannel energy = spec.sidedIo().energyChannel();
        if (energy.energyTooltipUsesAnyForm() || energy.listedEnergy().isEmpty()) {
            return;
        }
        MutableComponent sides = joinFaces(energy.listedEnergy(), Optional.empty());
        lines.add(labeled(
                "tooltip.cruciblecraft.machine.energy_in",
                ChatFormatting.YELLOW,
                sides));
    }

    private static void appendChannel(
            List<Component> lines,
            IoChannel channel,
            boolean input,
            String labelKey,
            ChatFormatting color) {
        Optional<MachineRelativeFace> auto = input ? channel.autoInput() : channel.autoOutput();
        if (channel.tooltipUsesAnyForm(input)) {
            MutableComponent value;
            if (auto.isPresent()) {
                value = face(auto.orElseThrow())
                        .append(Component.literal(" "))
                        .append(Component.translatable(
                                "tooltip.cruciblecraft.machine.auto_otherwise_any"));
            } else {
                value = Component.translatable("tooltip.cruciblecraft.machine.face.any")
                        .append(Component.literal(" "))
                        .append(Component.translatable(
                                "tooltip.cruciblecraft.machine.no_auto"));
            }
            lines.add(labeled(labelKey, color, value));
            return;
        }
        List<MachineRelativeFace> faces = input ? channel.listedInputs() : channel.listedOutputs();
        if (faces.isEmpty()) {
            return;
        }
        lines.add(labeled(labelKey, color, joinFaces(faces, auto)));
    }

    private static MutableComponent joinFaces(
            List<MachineRelativeFace> faces,
            Optional<MachineRelativeFace> auto) {
        MutableComponent joined = Component.empty();
        for (int index = 0; index < faces.size(); index++) {
            if (index > 0) {
                joined.append(Component.literal(", "));
            }
            MachineRelativeFace face = faces.get(index);
            joined.append(face(face));
            if (auto.isPresent() && auto.orElseThrow() == face) {
                joined.append(Component.literal(" "))
                        .append(Component.translatable(
                                "tooltip.cruciblecraft.machine.auto"));
            }
        }
        return joined;
    }

    private static MutableComponent labeled(
            String labelKey,
            ChatFormatting color,
            MutableComponent value) {
        return Component.translatable(labelKey)
                .withStyle(color)
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(value.withStyle(ChatFormatting.WHITE));
    }

    private static MutableComponent face(MachineRelativeFace face) {
        return Component.translatable(
                "tooltip.cruciblecraft.machine.face." + face.name().toLowerCase());
    }

    private static Component gray(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY);
    }
}
