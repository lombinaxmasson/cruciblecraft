package com.masson.cruciblecraft.machine.processing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * GT6 {@code Loader_MultiTileEntities} side bits for every live processing
 * host. Left/right follow {@link MachineRelativeFace}.
 */
public final class Gt6SidedIo {
    private static final int D = MachineRelativeFace.BOTTOM.bit();
    private static final int U = MachineRelativeFace.TOP.bit();
    private static final int L = MachineRelativeFace.LEFT.bit();
    private static final int F = MachineRelativeFace.FRONT.bit();
    private static final int R = MachineRelativeFace.RIGHT.bit();
    private static final int B = MachineRelativeFace.BACK.bit();
    private static final int ALL = MachineRelativeFace.ALL_FACES_MASK;
    private static final int ANY = MachineRelativeFace.ANY_MASK;
    private static final int BOTTOM = MachineRelativeFace.BOTTOM.ordinal();
    private static final int TOP = MachineRelativeFace.TOP.ordinal();
    private static final int LEFT = MachineRelativeFace.LEFT.ordinal();
    private static final int RIGHT = MachineRelativeFace.RIGHT.ordinal();
    private static final int BACK = MachineRelativeFace.BACK.ordinal();
    private static final int NO_AUTO = -1;

    private static final Map<String, Profile> PROFILES = new HashMap<>();

    static {
        put("crusher", p(U, TOP, D, BOTTOM, 0, NO_AUTO, 0, NO_AUTO, B));
        put("shredder", p(U, TOP, D, BOTTOM, 0, NO_AUTO, 0, NO_AUTO, L | R));
        put("sifter", p(U, TOP, D, BOTTOM, 0, NO_AUTO, 0, NO_AUTO, B));
        put("mortar", p(ANY, NO_AUTO, ANY, NO_AUTO, ANY, NO_AUTO, ANY, NO_AUTO, ANY));
        put("sluice", p(L | U, LEFT, R | D, RIGHT, L | U, TOP, R | D, BOTTOM, B));
        put("bath", p(U | L, LEFT, D | R, RIGHT, U | L, TOP, D | R, BOTTOM, ALL));
        put("centrifuge", p(U, TOP, R, RIGHT, U, TOP, L, LEFT, D));
        put("smelter", p(U, TOP, L, LEFT, U, TOP, R, RIGHT, D));
        put("melter", p(U, TOP, L, LEFT, U, TOP, R, RIGHT, D));
        put("oven", p(L, LEFT, R, RIGHT, ALL, NO_AUTO, ALL, NO_AUTO, D));
        put("compressor", p(U, TOP, D, BOTTOM, 0, NO_AUTO, 0, NO_AUTO, L));
        put("lathe", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, D));
        put("rollingmill", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, B));
        put("rollformer", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, B));
        put("rollbender", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, B));
        put("bender", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, B));
        put("clustermill", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, B));
        put("wiremill", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, B));
        put("extruder", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, D));
        put("press", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, U));
        put("sanding", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, U));
        put("polarizer", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, U | D));
        put("laser_engraver", p(L, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, U));
        put("squeezer", p(L, LEFT, R, RIGHT, 0, NO_AUTO, D, BOTTOM, U));
        put("laser_welder", p(L, LEFT, R, RIGHT, D | L, BOTTOM, 0, NO_AUTO, U));
        put("cutter", p(L, LEFT, R, RIGHT, U | D, BOTTOM, 0, NO_AUTO, B));
        put("slicer", p(L | U, LEFT, R | D, RIGHT, 0, NO_AUTO, 0, NO_AUTO, B));
        put("laminator", p(L | U, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, D));
        put("loom", p(U, TOP, D, BOTTOM, 0, NO_AUTO, 0, NO_AUTO, L | R));
        put("electricloom", p(U, TOP, D, BOTTOM, 0, NO_AUTO, 0, NO_AUTO, L | R));
        put("pressurewasher", p(L, LEFT, R, RIGHT, U | D, TOP, 0, NO_AUTO, B));
        put("mixer", p(L | U, LEFT, R | B, RIGHT, L | U, TOP, R | B, BACK, D));
        put("electric_mixer", p(L | U, LEFT, R | B, RIGHT, L | U, TOP, R | B, BACK, D));
        put("cryo_mixer", p(L | U, LEFT, R | B, RIGHT, L | U, TOP, R | B, BACK, D));
        put("electrolyzer", p(U | F | B, TOP, R | L, RIGHT, U | F | B, TOP, R | L, LEFT, D));
        put("distillery", p(U | L, LEFT, R, RIGHT, U | L, TOP, B, BACK, D));
        put("drying", p(B | L, LEFT, R, RIGHT, B | L, BACK, U, TOP, D));
        put("roaster", p(B | L, LEFT, R, RIGHT, B | L, BACK, U, TOP, D));
        put("autoclave", p(U | L, LEFT, B | R, RIGHT, D | L, BOTTOM, B | R, BACK, ALL));
        put("coagulator", p(ANY, NO_AUTO, D | R, BOTTOM, U | L, TOP, 0, NO_AUTO, ALL));
        put("injector", p(U | L, LEFT, R | D, RIGHT, U | L, TOP, R | D, BOTTOM, B));
        put("printer", p(U | L, LEFT, R | D, RIGHT, U | L, TOP, 0, NO_AUTO, B));
        put("scanner", p(U | L, LEFT, R | D, RIGHT, 0, NO_AUTO, 0, NO_AUTO, B));
        put("autocrafter", p(U | L, LEFT, R | D, RIGHT, 0, NO_AUTO, 0, NO_AUTO, U | D));
        put("assembler", p(U | L, LEFT, R | D, RIGHT, U | L, TOP, 0, NO_AUTO, U | D));
        put("boxinator", p(L | U, LEFT, R, RIGHT, 0, NO_AUTO, 0, NO_AUTO, D));
        put("canner", p(U | L, LEFT, R | D, RIGHT, U | L, TOP, R | D, BOTTOM, B));
        put("freezer", p(U | L, LEFT, R | D, RIGHT, U | L, TOP, R | D, BOTTOM, B));
        put("lightning", p(U | L, LEFT, R | D, RIGHT, U | L, TOP, R | D, BOTTOM, B));
        put("plantalyzer", p(U | L, LEFT, R | D, RIGHT, U | D, TOP, 0, NO_AUTO, B));
        put("bumblelyzer", p(U | L, LEFT, R | D, RIGHT, U | D, TOP, 0, NO_AUTO, B));
        put("nanofab", p(L | U, LEFT, D | R, RIGHT, L | U, TOP, D | R, BOTTOM, B));
        put("massfab", p(L | U, LEFT, D | R, RIGHT, L | U, TOP, D | R, BOTTOM, B));
        put("replicator", p(L | U, LEFT, D | R, RIGHT, L | U, TOP, D | R, BOTTOM, B));
        put("generifier", p(U | L, LEFT, D | R, RIGHT, U | L, TOP, D | R, BOTTOM, ALL));
        put("magnetic_separator", p(L, LEFT, R | D, RIGHT, L, LEFT, R | D, BOTTOM, U));
    }

    private Gt6SidedIo() {}

    public static String profileKey(ProcessingMachineSpec spec) {
        Objects.requireNonNull(spec, "spec");
        String path = spec.id().getPath();
        if (PROFILES.containsKey(path)) {
            return path;
        }
        path = spec.recipeMapId().getPath();
        if (PROFILES.containsKey(path)) {
            return path;
        }
        throw new IllegalArgumentException(
                "No GT6 I/O profile for " + spec.id() + " / " + spec.recipeMapId());
    }

    public static ProcessingMachineSpec.SidedIoPolicy policy(ProcessingMachineSpec spec) {
        return policy(profileKey(spec));
    }

    public static ProcessingMachineSpec.SidedIoPolicy policy(String path) {
        Profile profile = PROFILES.get(Objects.requireNonNull(path, "path"));
        if (profile == null) {
            throw new IllegalArgumentException("No GT6 I/O profile for " + path);
        }
        return new ProcessingMachineSpec.SidedIoPolicy(
                IoChannel.items(
                        profile.itemIn,
                        profile.itemAutoIn,
                        profile.itemOut,
                        profile.itemAutoOut),
                IoChannel.fluids(
                        profile.fluidIn,
                        profile.fluidAutoIn,
                        profile.fluidOut,
                        profile.fluidAutoOut),
                IoChannel.energy(profile.energy));
    }

    public static boolean known(String path) {
        return PROFILES.containsKey(path);
    }

    private static void put(String path, Profile profile) {
        if (PROFILES.put(path, profile) != null) {
            throw new IllegalStateException("Duplicate GT6 I/O profile " + path);
        }
    }

    private static Profile p(
            int itemIn,
            int itemAutoIn,
            int itemOut,
            int itemAutoOut,
            int fluidIn,
            int fluidAutoIn,
            int fluidOut,
            int fluidAutoOut,
            int energy) {
        return new Profile(
                itemIn,
                itemAutoIn,
                itemOut,
                itemAutoOut,
                fluidIn,
                fluidAutoIn,
                fluidOut,
                fluidAutoOut,
                energy);
    }

    private record Profile(
            int itemIn,
            int itemAutoIn,
            int itemOut,
            int itemAutoOut,
            int fluidIn,
            int fluidAutoIn,
            int fluidOut,
            int fluidAutoOut,
            int energy) {}
}
