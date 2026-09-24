package com.masson.cruciblecraft.compat.jade.observation;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.ITooltip;

/**
 * Small presentation adapter for the reusable GT6 IWaila rows.
 *
 * <p>The source inventory describes rows, not a Java API that can be called
 * from NeoForge. Keeping the row shapes here prevents individual providers
 * from silently inventing different state, energy, or tank formatting.
 */
public final class SourceWailaRows {
    private SourceWailaRows() {}

    public static void state(ITooltip tooltip, String state) {
        String key = normalizedState(state);
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.state",
                styled(
                        Component.translatable(
                                "jade.cruciblecraft.source.state." + key),
                        stateColor(key))));
    }

    public static void energyIoRange(
            ITooltip tooltip,
            long inputMin,
            long inputMax,
            String inputType,
            long outputMin,
            long outputMax,
            String outputType) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.energy_io_range",
                white(range(inputMin, inputMax)),
                cyan(inputType),
                white(range(outputMin, outputMax)),
                cyan(outputType)));
    }

    public static void energyIoRecommended(
            ITooltip tooltip,
            long input,
            String inputType,
            long output,
            String outputType) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.energy_io_recommended",
                white(input),
                cyan(inputType),
                white(output),
                cyan(outputType)));
    }

    public static void energyInputRange(
            ITooltip tooltip,
            long minimum,
            long maximum,
            String energyType) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.energy_input_range",
                white(range(minimum, maximum)),
                cyan(energyType)));
    }

    public static void energyAmount(
            ITooltip tooltip,
            String label,
            long amount,
            String energyType) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.energy_amount",
                label,
                white(amount),
                cyan(energyType)));
    }

    public static void energyOutput(
            ITooltip tooltip,
            long amount,
            String energyType) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.energy_output",
                white(amount),
                cyan(energyType)));
    }

    public static void tank(
            ITooltip tooltip,
            String label,
            FluidStack fluid,
            int capacity) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.tank",
                label,
                white(fluid == null ? 0 : fluid.getAmount()),
                white(capacity),
                cyan("mB"),
                fluidName(fluid)));
    }

    public static void longTank(
            ITooltip tooltip,
            String label,
            long amount,
            long capacity,
            Component fluidName) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.tank",
                label,
                white(amount),
                white(capacity),
                cyan("mB"),
                styled(fluidName, ChatFormatting.WHITE)));
    }

    public static void fluidOutput(
            ITooltip tooltip,
            String label,
            FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.fluid_output",
                label,
                white(fluid.getAmount()),
                cyan("mB"),
                fluidName(fluid)));
    }

    public static FluidStack fluid(String id, int amount) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.FLUID.containsKey(key)) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(
                BuiltInRegistries.FLUID.get(key),
                Math.max(0, amount));
    }

    public static void rod(
            ITooltip tooltip,
            String name,
            long remaining,
            int neutrons,
            boolean moderated) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.rod",
                white(neutrons),
                cyan("N"),
                name.isBlank() ? unavailable() : white(name),
                white(remaining),
                cyan("NU"),
                moderated ? gold(" M") : Component.empty()));
    }

    public static void efficiency(ITooltip tooltip, long basisPoints) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.efficiency",
                white(String.format(
                        Locale.ROOT,
                        "%.2f",
                        basisPoints / 100.0)),
                cyan("%")));
    }

    public static void contents(
            ITooltip tooltip,
            String material,
            String amount) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.contents",
                white(amount),
                white(material)));
    }

    public static void temperature(
            ITooltip tooltip,
            Component value,
            Component maximum) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.temperature",
                styled(value, ChatFormatting.WHITE),
                cyan("K"),
                styled(maximum, ChatFormatting.WHITE),
                cyan("K")));
    }

    public static void weight(ITooltip tooltip, Component value) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.weight",
                styled(value, ChatFormatting.WHITE),
                cyan("kg")));
    }

    public static void producing(ITooltip tooltip, Component value) {
        tooltip.add(Component.translatable(
                "jade.cruciblecraft.source.producing",
                styled(value, ChatFormatting.WHITE)));
    }

    public static Component unavailable() {
        return Component.translatable("jade.cruciblecraft.unavailable")
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static Component fluidName(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            return unavailable();
        }
        return styled(fluid.getHoverName(), ChatFormatting.WHITE);
    }

    public static Component fluidName(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.FLUID.containsKey(key)) {
            return unavailable();
        }
        return styled(
                new FluidStack(BuiltInRegistries.FLUID.get(key), 1)
                        .getHoverName(),
                ChatFormatting.WHITE);
    }

    private static Component white(Object value) {
        return Component.literal(String.valueOf(value))
                .withStyle(ChatFormatting.WHITE);
    }

    private static Component cyan(Object value) {
        return Component.literal(String.valueOf(value))
                .withStyle(ChatFormatting.AQUA);
    }

    private static Component gold(Object value) {
        return Component.literal(String.valueOf(value))
                .withStyle(ChatFormatting.GOLD);
    }

    private static Component styled(
            Component value, ChatFormatting formatting) {
        return value.copy().withStyle(formatting);
    }

    private static ChatFormatting stateColor(String state) {
        return switch (state) {
            case "stopped_force" -> ChatFormatting.YELLOW;
            case "active", "ready" -> ChatFormatting.GREEN;
            case "passive" -> ChatFormatting.BLUE;
            case "power_saving" -> ChatFormatting.AQUA;
            case "stopped" -> ChatFormatting.RED;
            default -> ChatFormatting.DARK_GRAY;
        };
    }

    private static String range(long minimum, long maximum) {
        return maximum == Long.MAX_VALUE
                ? minimum + "-∞"
                : minimum + "-" + maximum;
    }

    private static String normalizedState(String state) {
        if (state == null || state.isBlank()) {
            return "unavailable";
        }
        return switch (state.toLowerCase(Locale.ROOT)) {
            case "active", "running", "burning", "working" -> "active";
            case "passive" -> "passive";
            case "ready", "idle" -> "ready";
            case "power_saving" -> "power_saving";
            case "forced_stop", "force_stop" -> "stopped_force";
            case "stopped", "no_water", "no_heat", "no_steam",
                    "steam_full", "no_eu", "identity_quarantined" -> "stopped";
            default -> "unavailable";
        };
    }
}
