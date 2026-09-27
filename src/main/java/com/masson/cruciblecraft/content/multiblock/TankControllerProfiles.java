package com.masson.cruciblecraft.content.multiblock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;

import net.minecraft.resources.ResourceLocation;

/**
 * Source-backed Tank Main Valve profiles for the two GT6 hollow cubes.
 *
 * <p>3×3×3 is one air cell inside a shell. 5×5×5 is a 3×3×3 air core
 * inside a one-block shell. There is no other span. The values mirror the
 * NBT passed by GT6's {@code Loader_MultiTileEntities} at the pinned source
 * revision. Registry identities remain owned by {@code mte_inplace_catalog}.</p>
 */
public final class TankControllerProfiles {
    public static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";

    private static final Map<Integer, Profile> BY_CONTROLLER_META =
            profiles();

    private TankControllerProfiles() {}

    public static Optional<Profile> find(MteInPlaceSpec spec) {
        return spec == null
                ? Optional.empty()
                : Optional.ofNullable(BY_CONTROLLER_META.get(spec.meta()));
    }

    public static Optional<Profile> findControllerMeta(int meta) {
        return Optional.ofNullable(BY_CONTROLLER_META.get(meta));
    }

    public static List<Profile> values() {
        return List.copyOf(BY_CONTROLLER_META.values());
    }

    public static boolean isController(MteInPlaceSpec spec) {
        return find(spec).isPresent();
    }

    public static boolean matchesWall(Profile profile, MteInPlaceSpec wall) {
        return profile != null
                && wall != null
                && profile.wallMeta() == wall.meta();
    }

    private static Map<Integer, Profile> profiles() {
        LinkedHashMap<Integer, Profile> result = new LinkedHashMap<>();
        add(result, 17001, 18001, "wood", 432_000,
                false, false, false, false, true, 3, false);
        add(result, 17002, 18002, "stainless_steel", 1_728_000,
                true, true, false, false, false, 3, false);
        add(result, 17007, 18007, "invar", 1_728_000,
                true, false, false, false, false, 3, false);
        add(result, 17006, 18006, "titanium", 3_456_000,
                true, false, false, false, false, 3, false);
        add(result, 17003, 18003, "tungstensteel", 6_912_000,
                true, false, false, true, false, 3, false);
        add(result, 17004, 18004, "tungsten", 6_912_000,
                true, true, false, true, false, 3, false);
        add(result, 17005, 18005, "adamantium", 110_592_000,
                true, true, true, true, false, 3, false);
        add(result, 17022, 18022, "stainless_steel", 6_912_000,
                true, true, false, false, false, 3, true);
        add(result, 17027, 18027, "invar", 6_912_000,
                true, false, false, false, false, 3, true);
        add(result, 17026, 18026, "titanium", 13_824_000,
                true, false, false, false, false, 3, true);
        add(result, 17023, 18023, "tungstensteel", 27_648_000,
                true, false, false, true, false, 3, true);
        add(result, 17024, 18024, "tungsten", 27_648_000,
                true, true, false, true, false, 3, true);
        add(result, 17025, 18025, "adamantium", 442_368_000,
                true, true, true, true, false, 3, true);
        add(result, 17042, 18002, "stainless_steel", 8_000_000,
                true, true, false, false, false, 5, false);
        add(result, 17047, 18007, "invar", 8_000_000,
                true, false, false, false, false, 5, false);
        add(result, 17046, 18006, "titanium", 16_000_000,
                true, false, false, false, false, 5, false);
        add(result, 17043, 18003, "tungstensteel", 32_000_000,
                true, false, false, true, false, 5, false);
        add(result, 17044, 18004, "tungsten", 32_000_000,
                true, true, false, true, false, 5, false);
        add(result, 17045, 18005, "adamantium", 512_000_000,
                true, true, true, true, false, 5, false);
        add(result, 17062, 18022, "stainless_steel", 32_000_000,
                true, true, false, false, false, 5, true);
        add(result, 17067, 18027, "invar", 32_000_000,
                true, false, false, false, false, 5, true);
        add(result, 17066, 18026, "titanium", 64_000_000,
                true, false, false, false, false, 5, true);
        add(result, 17063, 18023, "tungstensteel", 128_000_000,
                true, false, false, true, false, 5, true);
        add(result, 17064, 18024, "tungsten", 128_000_000,
                true, true, false, true, false, 5, true);
        add(result, 17065, 18025, "adamantium", 2_048_000_000,
                true, true, true, true, false, 5, true);
        return Map.copyOf(result);
    }

    private static void add(
            Map<Integer, Profile> profiles,
            int controllerMeta,
            int wallMeta,
            String materialId,
            int capacityMb,
            boolean gasProof,
            boolean acidProof,
            boolean plasmaProof,
            boolean magicProof,
            boolean onlySimple,
            int span,
            boolean dense) {
        Profile previous = profiles.put(
                controllerMeta,
                new Profile(
                        controllerMeta,
                        wallMeta,
                        materialId,
                        capacityMb,
                        gasProof,
                        acidProof,
                        plasmaProof,
                        magicProof,
                        onlySimple,
                        span,
                        dense));
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate tank controller meta " + controllerMeta);
        }
    }

    public record Profile(
            int controllerMeta,
            int wallMeta,
            String materialId,
            int capacityMb,
            boolean gasProof,
            boolean acidProof,
            boolean plasmaProof,
            boolean magicProof,
            boolean onlySimple,
            int span,
            boolean dense) {
        public Profile {
            if (controllerMeta <= 0
                    || wallMeta <= 0
                    || materialId == null
                    || materialId.isBlank()
                    || capacityMb <= 0
                    || (span != 3 && span != 5)
                    || (span == 5 && onlySimple)) {
                throw new IllegalArgumentException("Invalid tank profile");
            }
        }

        /** Shell radius. 3×3×3 is 1, 5×5×5 is 2. */
        public int radius() {
            return span / 2;
        }

        /** Wall cells, excluding the valve and the air core. */
        public int wallPorts() {
            return span == 3 ? 25 : 97;
        }

        public ResourceLocation structureId() {
            return ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID,
                    span == 3 ? "tank_3x3x3" : "tank_5x5x5");
        }

        public ResourceLocation controllerId() {
            if (onlySimple) {
                return id("wood/tank_main_valve");
            }
            String size = span == 5 ? "large_" : "small_";
            String density = dense ? "dense_" : "";
            return id("multiblock/"
                    + size
                    + density
                    + materialId
                    + "_tank_main_valve");
        }

        public ResourceLocation wallId() {
            if (onlySimple) {
                return id("wood/wall");
            }
            if (dense) {
                return id("multiblock/dense_" + materialId + "_wall");
            }
            return id(materialId + "/wall");
        }

        private static ResourceLocation id(String path) {
            return ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, path);
        }
    }
}
