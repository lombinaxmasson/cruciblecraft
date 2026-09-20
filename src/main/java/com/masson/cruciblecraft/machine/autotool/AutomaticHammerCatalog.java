package com.masson.cruciblecraft.machine.autotool;

import java.util.List;
import java.util.Objects;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.energy.cable.GT6VoltageTiers;

import net.minecraft.resources.ResourceLocation;

/** GT6 AutoToolHammer 15001-15004. Not a processing-machine recipe map. */
public final class AutomaticHammerCatalog {
    public record Profile(
            ResourceLocation id,
            String materialId,
            int sourceId,
            long input,
            int quality,
            float hardness,
            float resistance) {
        public Profile {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(materialId, "materialId");
            if (sourceId <= 0 || input <= 0L || quality <= 0 || hardness <= 0.0F) {
                throw new IllegalArgumentException("Invalid automatic hammer " + id);
            }
        }

        public long minPacket() {
            return Math.max(1L, input / 8L);
        }

        /** GT6 {@code getEnergySizeInputMax} default is recommended × 2. */
        public long maxPacket() {
            return input * 2L;
        }

        public long capacity() {
            return Math.max(512L, input * 64L);
        }
    }

    /** GT6 {@code explode(UT.Code.tierMax(aSize))}: strength is the voltage tier. */
    public static float overchargeExplosionStrength(long packetSize) {
        return GT6VoltageTiers.tierMax(packetSize);
    }

    public static final List<Profile> ALL = List.of(
            profile("automatic_hammer", "bronze", 15001, 8L, 1, 7.0F),
            profile("steel_automatic_hammer", "steel", 15002, 32L, 2, 6.0F),
            profile("titanium_automatic_hammer", "titanium", 15003, 128L, 3, 9.0F),
            profile(
                    "tungstensteel_automatic_hammer",
                    "tungstensteel",
                    15004,
                    512L,
                    4,
                    12.5F));

    private AutomaticHammerCatalog() {}

    public static Profile require(ResourceLocation id) {
        return ALL.stream()
                .filter(profile -> profile.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown automatic hammer " + id));
    }

    private static Profile profile(
            String path,
            String material,
            int sourceId,
            long input,
            int quality,
            float hardness) {
        return new Profile(
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path),
                material,
                sourceId,
                input,
                quality,
                hardness,
                hardness);
    }
}
