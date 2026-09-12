package com.masson.cruciblecraft.logistics.machinecover;

import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

/** GT6-backed behavior contracts for covers mounted on processing machines. */
public final class MachineCoverBehaviors {
    private static final int DETECTOR_STRONG = 1;
    private static final int DETECTOR_INVERT = 2;
    private static final int BUTTON_RESET_TICKS = 10;
    private static final int VENT_INTERVAL = 360;
    private static final int VENT_OFFSET = 30;
    private static final int VENT_AMOUNT = 256_000;
    private static final ResourceLocation NETHER_AIR =
            ResourceLocation.parse("cruciblecraft:netherair");
    private static final ResourceLocation END_AIR =
            ResourceLocation.parse("cruciblecraft:enderair");
    private static final Set<String> CONTROLLERS = Set.of(
            "controller_auto",
            "controller_auto_redstone",
            "controller_auto_timer",
            "controller_redstone");
    private static final Set<String> DETECTORS = Set.of(
            "detector_running_possible",
            "detector_running_passively",
            "detector_running_actively",
            "detector_running_successfully");
    private static final Set<String> SCALES = Set.of(
            "scale_energy",
            "scale_progress");
    private static final Set<String> SELECTORS = Set.of(
            "selector_redstone",
            "selector_tag",
            "selector_button_panel");

    private MachineCoverBehaviors() {}

    public static void validateDefinitions() {
        for (MachineCoverKinds.ItemCover item : MachineCoverKinds.ITEMS) {
            validate(item.definitionPath(), item.behaviorPath());
        }
        for (MachineCoverKinds.ExtraDefinition extra : MachineCoverKinds.EXTRA) {
            validate(extra.definitionPath(), extra.behaviorPath());
        }
    }

    private static void validate(String path, String behaviorPath) {
        ResourceLocation id = MachineCoverKinds.id(path);
        CoverDefinition definition = CoverDefinitionCatalog.require(id);
        if (!behaviorPath.equals(definition.behaviorId().getPath())) {
            throw new IllegalStateException(
                    id + " behavior drifted to " + definition.behaviorId());
        }
    }

    public static boolean canPlace(
            MachineCoverHost host,
            Direction side,
            PipeCover cover) {
        if (host == null || side == null || cover == null) {
            return false;
        }
        String path = cover.definitionId().getPath();
        String behaviorPath = cover.definition()
                .map(definition -> definition.behaviorId().getPath())
                .orElse("");
        if (!"cruciblecraft".equals(cover.definitionId().getNamespace())
                || !MachineCoverKinds.BEHAVIOR_PATHS.contains(behaviorPath)
                || MachineCoverKinds.isBlockedWireHost(cover.definitionId())) {
            return false;
        }
        if (requiresCanTick(path) && !host.canTick()) {
            return false;
        }
        if ("vent".equals(path) && !host.hasFluidTanks()) {
            return false;
        }
        if (MachineCoverKinds.requiresEnergy(cover.definitionId())
                && !host.hasEnergyBuffer()) {
            return false;
        }
        if ("controller_auto".equals(path) && !host.runningPossible()) {
            return false;
        }
        return true;
    }

    public static void tickAll(MachineCoverHost host) {
        if (host == null) {
            return;
        }
        boolean controlled = false;
        boolean hasDisplay = false;
        boolean enabled = true;
        boolean stopped = false;
        int selectedMode = 0;
        for (Direction side : Direction.values()) {
            PipeCover cover = host.covers().get(side).orElse(null);
            if (cover == null) {
                continue;
            }
            String path = cover.definitionId().getPath();
            String behaviorPath = cover.definition()
                    .map(definition -> definition.behaviorId().getPath())
                    .orElse("");
            if (CONTROLLERS.contains(behaviorPath)) {
                controlled = true;
                enabled &= controllerState(host, side, cover, behaviorPath);
            } else if ("controller_display".equals(path)) {
                hasDisplay = true;
            } else if ("controller_covers".equals(path)) {
                stopped |= controllerCoversState(host, side, cover);
            } else if ("selector_redstone".equals(path)) {
                selectedMode = host.incomingRedstone(side);
            } else if ("selector_tag".equals(path)) {
                selectedMode = cover.config().redstone();
            } else if ("selector_button_panel".equals(path)) {
                selectedMode = MachineCoverVisuals.buttonMode(
                        cover.config().visual());
            }
        }
        host.setCoverEnabled(controlled
                ? enabled
                : (hasDisplay ? host.coverEnabled() : true));
        host.setCoversStopped(stopped);
        host.setSelectorMode(selectedMode);
        for (Direction side : Direction.values()) {
            PipeCover cover = host.covers().get(side).orElse(null);
            if (cover == null) {
                continue;
            }
            tickOne(host, side, cover, cover.definitionId().getPath());
        }
    }

    public static boolean hasSelector(MachineCoverHost host) {
        if (host == null) {
            return false;
        }
        for (Direction side : Direction.values()) {
            PipeCover cover = host.covers().get(side).orElse(null);
            if (cover != null
                    && SELECTORS.contains(cover.definitionId().getPath())) {
                return true;
            }
        }
        return false;
    }

    public static boolean collectableAir(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty()
                && state.getFluidState().isEmpty();
    }

    public static Optional<Fluid> ventAir(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        var biome = level.getBiome(pos);
        if (level.dimension() == Level.NETHER || biome.is(BiomeTags.IS_NETHER)) {
            return BuiltInRegistries.FLUID.getOptional(NETHER_AIR);
        }
        if (level.dimension() == Level.END || biome.is(BiomeTags.IS_END)) {
            return BuiltInRegistries.FLUID.getOptional(END_AIR);
        }
        return ModFluids.materialFluid("air");
    }

    private static boolean requiresCanTick(String path) {
        return "controller_auto".equals(path)
                || "vent".equals(path)
                || "display_energy".equals(path)
                || DETECTORS.contains(path)
                || SCALES.contains(path);
    }

    private static boolean controllerState(
            MachineCoverHost host,
            Direction side,
            PipeCover cover,
            String path) {
        int invert = cover.config().invert().orElse(0);
        return switch (path) {
            case "controller_auto" ->
                    host.runningPossible() || host.runningActively();
            case "controller_redstone" ->
                    bind1(host.incomingRedstone(side)) != invert;
            case "controller_auto_redstone" ->
                    (host.runningActively() && !host.runningSuccessfully())
                            || bind1(host.incomingRedstone(side)) != invert;
            case "controller_auto_timer" -> {
                int period = MachineCoverKinds.timerPeriod(
                        cover.definitionId());
                long phase = period <= 0 ? 0 : host.gameTime() % period;
                yield host.runningActively() || phase >= period - 10;
            }
            default -> true;
        };
    }

    private static boolean controllerCoversState(
            MachineCoverHost host,
            Direction side,
            PipeCover cover) {
        int invert = cover.config().invert().orElse(0);
        return bind1(host.incomingRedstone(side)) == invert;
    }

    private static int bind1(int redstone) {
        return redstone > 0 ? 1 : 0;
    }

    private static void tickOne(
            MachineCoverHost host,
            Direction side,
            PipeCover cover,
            String path) {
        switch (path) {
            case "controller_display" -> updateDisplay(host, side);
            case "display_energy" -> updateEnergyDisplay(host, side, cover);
            case "scale_energy" -> updateEnergyScale(host, side, cover);
            case "scale_progress" -> updateProgressScale(host, side, cover);
            case "detector_running_possible" ->
                    updateDetector(host, side, cover, host.runningPossible());
            case "detector_running_passively" ->
                    updateDetector(host, side, cover, host.runningPassively());
            case "detector_running_actively" ->
                    updateDetector(host, side, cover, host.runningActively());
            case "detector_running_successfully" ->
                    updateDetector(host, side, cover, host.runningSuccessfully());
            case "selector_button_panel" -> updateButtonPanel(host, side, cover);
            case "redstone_conductor_out" ->
                    updateConductorOut(host, side, cover);
            case "vent" -> tickVent(host, side);
            default -> {
                // Static and input-only covers have no per-tick work.
            }
        }
    }

    private static void updateDisplay(
            MachineCoverHost host,
            Direction side) {
        PipeCover cover = host.covers().get(side).orElse(null);
        if (cover == null) {
            return;
        }
        int visual = MachineCoverVisuals.encodeDisplay(
                host.runningPossible(),
                host.runningPassively(),
                host.runningActively(),
                host.coverEnabled(),
                MachineCoverVisuals.displayStyle(cover.config().visual()));
        replace(host, side, cover, visual, cover.config().redstone());
    }

    private static void updateEnergyDisplay(
            MachineCoverHost host,
            Direction side,
            PipeCover cover) {
        if (!host.hasEnergyBuffer()) {
            return;
        }
        replace(
                host,
                side,
                cover,
                energyVisual(host.energyStored(), host.energyCapacity()),
                cover.config().redstone());
    }

    private static void updateEnergyScale(
            MachineCoverHost host,
            Direction side,
            PipeCover cover) {
        if (!host.hasEnergyBuffer()) {
            return;
        }
        int value = fullness(host.energyStored(), host.energyCapacity());
        updateScale(host, side, cover, value);
    }

    private static void updateProgressScale(
            MachineCoverHost host,
            Direction side,
            PipeCover cover) {
        updateScale(host, side, cover, fullness(host.progress(), host.duration()));
    }

    private static void updateScale(
            MachineCoverHost host,
            Direction side,
            PipeCover cover,
            int value) {
        int output = value;
        if ((cover.config().visual() & DETECTOR_INVERT) != 0) {
            output = 15 - output;
        }
        replace(host, side, cover, cover.config().visual(), output);
    }

    private static void updateDetector(
            MachineCoverHost host,
            Direction side,
            PipeCover cover,
            boolean condition) {
        int output = condition ? 15 : 0;
        if ((cover.config().visual() & DETECTOR_INVERT) != 0) {
            output = 15 - output;
        }
        replace(host, side, cover, cover.config().visual(), output);
    }

    private static void updateButtonPanel(
            MachineCoverHost host,
            Direction side,
            PipeCover cover) {
        int visual = cover.config().visual();
        int remaining = cover.config().redstone();
        int mode = host.selectorMode();
        if (cover.config().invert().orElse(0) != 0 && remaining > 1) {
            remaining -= 1;
            if (remaining == 1) {
                mode = 0;
                host.setSelectorMode(0);
            }
        }
        visual = MachineCoverVisuals.withButtonMode(visual, mode);
        replace(host, side, cover, visual, remaining);
    }

    private static void updateConductorOut(
            MachineCoverHost host,
            Direction side,
            PipeCover cover) {
        int maximum = 0;
        for (Direction candidate : Direction.values()) {
            PipeCover other = host.covers().get(candidate).orElse(null);
            if (other != null
                    && "redstone_conductor_in".equals(
                            other.definitionId().getPath())) {
                maximum = Math.max(maximum, host.incomingRedstone(candidate));
            }
        }
        replace(host, side, cover, cover.config().visual(), maximum);
    }

    private static void tickVent(MachineCoverHost host, Direction side) {
        if (!host.hasFluidTanks()
                || host.coversStopped()
                || host.level() == null
                || !collectableAir(
                        host.level(), host.hostPos().relative(side))
                || Math.floorMod(
                                host.gameTime(),
                                VENT_INTERVAL)
                        != VENT_OFFSET + 60 * side.ordinal()) {
            return;
        }
        host.fillAir(VENT_AMOUNT);
    }

    private static void replace(
            MachineCoverHost host,
            Direction side,
            PipeCover current,
            int visual,
            int redstone) {
        if (current.config().visual() == visual
                && current.config().redstone() == redstone) {
            return;
        }
        host.replaceCover(side, current.withDisplay(visual, redstone));
    }

    public static int weakRedstone(
            MachineCoverHost host,
            Direction side) {
        PipeCover cover = host == null || side == null
                ? null
                : host.covers().get(side).orElse(null);
        if (cover == null) {
            return 0;
        }
        String path = cover.definitionId().getPath();
        if (DETECTORS.contains(path)
                || SCALES.contains(path)
                || "redstone_emitter".equals(path)
                || "redstone_conductor_out".equals(path)) {
            return cover.config().redstone();
        }
        return 0;
    }

    public static int directRedstone(
            MachineCoverHost host,
            Direction side) {
        PipeCover cover = host == null || side == null
                ? null
                : host.covers().get(side).orElse(null);
        if (cover == null) {
            return 0;
        }
        String path = cover.definitionId().getPath();
        if (DETECTORS.contains(path)
                || SCALES.contains(path)
                || "redstone_emitter".equals(path)) {
            return (cover.config().visual() & DETECTOR_STRONG) != 0
                    ? cover.config().redstone()
                    : 0;
        }
        return 0;
    }

    public static boolean onRightClick(
            MachineCoverHost host,
            Direction side,
            BlockHitResult hit) {
        if (host == null || side == null || hit == null) {
            return false;
        }
        PipeCover cover = host.covers().get(side).orElse(null);
        if (cover == null) {
            return false;
        }
        String path = cover.definitionId().getPath();
        if ("controller_display".equals(path)) {
            if (hit.getDirection() != side) {
                return false;
            }
            double[] uv = faceUv(host.hostPos(), hit, side);
            if (!MachineCoverVisuals.displaySwitchHotspot(
                    MachineCoverVisuals.displayStyle(cover.config().visual()),
                    uv[0],
                    uv[1])) {
                return false;
            }
            host.setCoverEnabled(!host.coverEnabled());
            return true;
        }
        if ("redstone_emitter".equals(path)) {
            int next = Math.floorMod(
                    cover.config().redstone()
                            + (hit.getDirection() == side
                                    && hit.getLocation().y % 1.0 < 0.5
                                    ? -1 : 1),
                    16);
            host.replaceCover(side, cover.withDisplay(
                    cover.config().visual(), next));
            host.notifyRedstone();
            return true;
        }
        if ("selector_button_panel".equals(path)) {
            int mode = buttonMode(host.hostPos(), hit, side);
            int visual = MachineCoverVisuals.withButtonMode(
                    cover.config().visual(), mode);
            int redstone = cover.config().invert().orElse(0) != 0
                    ? BUTTON_RESET_TICKS
                    : cover.config().redstone();
            host.replaceCover(side, cover.withDisplay(visual, redstone));
            host.setSelectorMode(mode);
            return true;
        }
        return false;
    }

    private static int buttonMode(
            net.minecraft.core.BlockPos pos,
            BlockHitResult hit,
            Direction side) {
        double[] uv = faceUv(pos, hit, side);
        int column = Math.min(3, Math.max(0, (int) (uv[0] * 4)));
        int row = Math.min(3, Math.max(0, (int) (uv[1] * 4)));
        return column + row * 4;
    }

    static double[] faceUv(
            net.minecraft.core.BlockPos pos,
            BlockHitResult hit,
            Direction side) {
        double x = hit.getLocation().x - pos.getX();
        double y = hit.getLocation().y - pos.getY();
        double z = hit.getLocation().z - pos.getZ();
        double horizontal;
        double vertical;
        switch (side) {
            case NORTH -> {
                horizontal = 1.0 - x;
                vertical = y;
            }
            case SOUTH -> {
                horizontal = x;
                vertical = y;
            }
            case WEST -> {
                horizontal = z;
                vertical = y;
            }
            case EAST -> {
                horizontal = 1.0 - z;
                vertical = y;
            }
            case UP -> {
                horizontal = x;
                vertical = 1.0 - z;
            }
            case DOWN -> {
                horizontal = x;
                vertical = z;
            }
            default -> {
                horizontal = x;
                vertical = y;
            }
        }
        return new double[] {horizontal, vertical};
    }

    public static boolean onTool(
            MachineCoverHost host,
            Direction side,
            ToolAction action) {
        if (host == null || side == null || action == null) {
            return false;
        }
        PipeCover cover = host.covers().get(side).orElse(null);
        if (cover == null) {
            return false;
        }
        String path = cover.definitionId().getPath();
        int visual = cover.config().visual();
        if (action == ToolAction.WIRE_CUTTER
                && (DETECTORS.contains(path)
                        || SCALES.contains(path)
                        || "redstone_emitter".equals(path))) {
            host.replaceCover(
                    side,
                    cover.withDisplay(
                            visual ^ DETECTOR_STRONG,
                            cover.config().redstone()));
            host.notifyRedstone();
            return true;
        }
        if (action == ToolAction.SCREWDRIVER
                && (DETECTORS.contains(path) || SCALES.contains(path))) {
            host.replaceCover(
                    side,
                    cover.withDisplay(
                            visual ^ DETECTOR_INVERT,
                            cover.config().redstone()));
            host.notifyRedstone();
            return true;
        }
        if (action == ToolAction.SCREWDRIVER
                && (Set.of(
                        "controller_redstone",
                        "controller_auto_redstone",
                        "controller_covers",
                        "selector_button_panel")
                        .contains(path))) {
            int next = cover.config().invert().orElse(0) == 0 ? 1 : 0;
            host.replaceCover(side, cover.withConfig(
                    cover.config().withInvert(next)));
            return true;
        }
        if (action == ToolAction.CHISEL && "controller_display".equals(path)) {
            host.replaceCover(
                    side,
                    cover.withDisplay(
                            MachineCoverVisuals.cycleDisplayStyle(visual),
                            cover.config().redstone()));
            return true;
        }
        if (action == ToolAction.CHISEL
                && "selector_button_panel".equals(path)) {
            host.replaceCover(
                    side,
                    cover.withDisplay(
                            MachineCoverVisuals.cycleButtonStyle(visual),
                            cover.config().redstone()));
            return true;
        }
        return false;
    }

    private static int energyVisual(long stored, long capacity) {
        if (stored <= 0L || capacity <= 0L) {
            return 0;
        }
        if (stored >= capacity) {
            return 10;
        }
        return 9 - (int) Math.max(
                0L,
                Math.min(8L, ((capacity - stored) * 9L) / capacity));
    }

    private static int fullness(long value, long maximum) {
        if (value <= 0L || maximum <= 0L) {
            return 0;
        }
        if (value >= maximum) {
            return 15;
        }
        return 14 - (int) Math.max(
                0L,
                Math.min(13L, ((maximum - value) * 14L) / maximum));
    }
}
