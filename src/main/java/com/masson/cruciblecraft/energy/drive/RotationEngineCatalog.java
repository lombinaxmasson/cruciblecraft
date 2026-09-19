package com.masson.cruciblecraft.energy.drive;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.masson.cruciblecraft.content.mte.MteInPlaceCatalog;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.energy.cable.GT6VoltageTiers;

import net.minecraft.resources.ResourceLocation;

/**
 * GT6 {@code MultiTileEntityEngineRotation} rates from
 * {@code Loader_MultiTileEntities.kinetic}: {@code NBT_INPUT = V[tier]},
 * {@code NBT_OUTPUT = V[tier]/2}, RU in / KU out, {@code NBT_WASTE_ENERGY}.
 */
public final class RotationEngineCatalog {
    private static final Map<Integer, Integer> META_VOLTAGE_INDEX = voltageByMeta();
    private static final Catalog CATALOG = load();

    private RotationEngineCatalog() {}

    public static List<Profile> profiles() {
        return CATALOG.byId.values().stream().toList();
    }

    public static Optional<Profile> find(ResourceLocation id) {
        return Optional.ofNullable(CATALOG.byId.get(id));
    }

    public static Profile require(ResourceLocation id) {
        Profile profile = CATALOG.byId.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown rotation engine " + id);
        }
        return profile;
    }

    public record Profile(
            ResourceLocation id,
            int sourceMeta,
            int voltageIndex,
            long inputRec,
            long outputRec) {
        public Profile {
            Objects.requireNonNull(id, "id");
            if (sourceMeta < 0 || voltageIndex < 0 || inputRec <= 0L || outputRec <= 0L) {
                throw new IllegalArgumentException("Invalid rotation engine " + id);
            }
        }

        public long inputMin() {
            return inputRec <= 16L ? 1L : inputRec / 2L;
        }

        public long inputMax() {
            return Math.multiplyExact(inputRec, 2L);
        }

        public long outputMin() {
            return outputRec / 2L;
        }

        public long outputMax() {
            return Math.multiplyExact(outputRec, 2L);
        }

        public long capacity() {
            return inputMax();
        }
    }

    private static Catalog load() {
        LinkedHashMap<ResourceLocation, Profile> byId = new LinkedHashMap<>();
        int engines = 0;
        for (MteInPlaceSpec spec : MteInPlaceCatalog.specs()) {
            if (spec.kind() != MteInPlaceKind.ROTATION_ENGINE) {
                continue;
            }
            Integer voltageIndex = META_VOLTAGE_INDEX.get(spec.meta());
            if (voltageIndex == null) {
                throw new IllegalStateException(
                        "Rotation engine meta missing GT6 V[] index: "
                                + spec.meta()
                                + " "
                                + spec.id());
            }
            long input = GT6VoltageTiers.VOLTAGES[voltageIndex];
            Profile profile = new Profile(
                    spec.id(),
                    spec.meta(),
                    voltageIndex,
                    input,
                    input / 2L);
            if (byId.put(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate rotation engine " + profile.id());
            }
            engines++;
        }
        if (engines != META_VOLTAGE_INDEX.size()) {
            throw new IllegalStateException(
                    "Rotation engine catalog expected "
                            + META_VOLTAGE_INDEX.size()
                            + " live engines, found "
                            + engines);
        }
        return new Catalog(Map.copyOf(byId));
    }

    /**
     * GT6 metas from {@code Loader_MultiTileEntities.kinetic}. Wooden 24807 is
     * not a live CC identity.
     */
    private static Map<Integer, Integer> voltageByMeta() {
        LinkedHashMap<Integer, Integer> byMeta = new LinkedHashMap<>();
        byMeta.put(24777, 1); // brass
        byMeta.put(24787, 1); // arsenic copper
        byMeta.put(24797, 1); // arsenic bronze
        byMeta.put(24817, 1); // bronze
        byMeta.put(24827, 2); // steel
        byMeta.put(24837, 3); // titanium
        byMeta.put(24847, 4); // tungstensteel
        byMeta.put(24857, 5); // iridium
        byMeta.put(24897, 9); // adamantium
        return Map.copyOf(byMeta);
    }

    private record Catalog(Map<ResourceLocation, Profile> byId) {}
}
