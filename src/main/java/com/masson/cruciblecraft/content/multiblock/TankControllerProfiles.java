package com.masson.cruciblecraft.content.multiblock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;

import net.minecraft.resources.ResourceLocation;

/**
 * Source-backed 3x3x3 Tank Main Valve profiles.
 *
 * <p>The values mirror the NBT passed by GT6's
 * {@code Loader_MultiTileEntities} at the pinned source revision.  The
 * registry identities themselves remain owned by {@code mte_inplace_catalog}.</p>
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
                false, false, false, false, true);
        add(result, 17002, 18002, "stainless_steel", 1_728_000,
                true, true, false, false, false);
        add(result, 17007, 18007, "invar", 1_728_000,
                true, false, false, false, false);
        add(result, 17006, 18006, "titanium", 3_456_000,
                true, false, false, false, false);
        add(result, 17003, 18003, "tungstensteel", 6_912_000,
                true, false, false, true, false);
        add(result, 17004, 18004, "tungsten", 6_912_000,
                true, true, false, true, false);
        add(result, 17005, 18005, "adamantium", 110_592_000,
                true, true, true, true, false);
        add(result, 17022, 18022, "stainless_steel", 6_912_000,
                true, true, false, false, false);
        add(result, 17027, 18027, "invar", 6_912_000,
                true, false, false, false, false);
        add(result, 17026, 18026, "titanium", 13_824_000,
                true, false, false, false, false);
        add(result, 17023, 18023, "tungstensteel", 27_648_000,
                true, false, false, true, false);
        add(result, 17024, 18024, "tungsten", 27_648_000,
                true, true, false, true, false);
        add(result, 17025, 18025, "adamantium", 442_368_000,
                true, true, true, true, false);
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
            boolean onlySimple) {
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
                        onlySimple));
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
            boolean onlySimple) {
        public Profile {
            if (controllerMeta <= 0
                    || wallMeta <= 0
                    || materialId == null
                    || materialId.isBlank()
                    || capacityMb <= 0) {
                throw new IllegalArgumentException("Invalid tank profile");
            }
        }

        public ResourceLocation controllerId() {
            String path = onlySimple
                    ? "wood/tank_main_valve"
                    : "multiblock/small_"
                            + (controllerMeta >= 17022 ? "dense_" : "")
                            + materialId
                            + "_tank_main_valve";
            return ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, path);
        }

        public ResourceLocation wallId() {
            String path = onlySimple
                    ? "wood/wall"
                    : materialId + "/wall";
            if (controllerMeta >= 17022) {
                path = "multiblock/dense_" + materialId + "_wall";
            }
            return ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, path);
        }
    }
}
