package com.masson.cruciblecraft.content.fluidbarrel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.content.blockentity.TankFluidSafety;
import com.masson.cruciblecraft.material.ChemicalFluidRegistrationGate;
import com.masson.cruciblecraft.material.HotFluidRegistrationGate;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Wood-barrel simple-fluid names extracted from GT6 {@code FluidsGT.SIMPLE},
 * plus barrel gas classification that keeps explicit liquids out of the gas path.
 */
public final class FluidBarrelFluids {
    private static final String RESOURCE =
            "/data/cruciblecraft/fluid_barrel_simple_fluids.json";
    private static final Gson GSON = new Gson();
    private static final Set<String> SIMPLE_NAMES = load();

    private FluidBarrelFluids() {}

    public static Set<String> simpleNames() {
        return SIMPLE_NAMES;
    }

    public static boolean isSimple(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        if (fluid == Fluids.WATER
                || fluid == Fluids.FLOWING_WATER
                || fluid == Fluids.LAVA
                || fluid == Fluids.FLOWING_LAVA) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return SIMPLE_NAMES.contains(path)
                || SIMPLE_NAMES.contains(path.replace('_', '.'))
                || SIMPLE_NAMES.contains(path.replace('.', '_'));
    }

    /**
     * Explicit liquid state is not gas, even when density is negative.
     * Otherwise the existing chemical, hot, and density classification applies.
     */
    public static boolean isGas(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            ChemicalFluidRegistrationGate.State chemical =
                    ModFluids.chemicalState(stack.getFluid()).orElse(null);
            if (chemical == ChemicalFluidRegistrationGate.State.LIQUID) {
                return false;
            }
            if (chemical == ChemicalFluidRegistrationGate.State.GAS) {
                return true;
            }
        } catch (IllegalStateException ignored) {
            // Registry lookup may not be finalized in isolated tests.
        }
        try {
            if (ModFluids.hotId(stack.getFluid()).isPresent()) {
                HotFluidRegistrationGate.State hot = ModFluids.hot(
                                ModFluids.hotId(stack.getFluid()).orElseThrow())
                        .map(entry -> entry.state())
                        .orElse(null);
                if (hot == HotFluidRegistrationGate.State.LIQUID) {
                    return false;
                }
                if (hot == HotFluidRegistrationGate.State.GAS) {
                    return true;
                }
            }
        } catch (IllegalStateException ignored) {
            // Registry lookup may not be finalized in isolated tests.
        }
        return TankFluidSafety.isGas(stack);
    }

    public static boolean allow(FluidBarrelProfile profile, FluidStack stack) {
        if (profile == null || stack == null || stack.isEmpty()) {
            return false;
        }
        return FluidBarrelLogic.allow(
                TankFluidSafety.conductsPower(stack),
                TankFluidSafety.temperature(stack),
                profile.meltingPoint(),
                profile.onlySimple(),
                isSimple(stack.getFluid()));
    }

    /** Item-form fill rejects sealed contents separately. */
    public static boolean acceptedByItem(
            FluidBarrelProfile profile, FluidStack stack) {
        if (!allow(profile, stack)) {
            return false;
        }
        if (!profile.gasProof() && isGas(stack)) {
            return false;
        }
        if (!profile.acidProof() && TankFluidSafety.isAcid(stack)) {
            return false;
        }
        if (!profile.magicProof() && TankFluidSafety.isMagic(stack)) {
            return false;
        }
        return profile.plasmaProof() || !TankFluidSafety.isPlasma(stack);
    }

    public static boolean vanillaLava(Fluid fluid) {
        return fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA;
    }

    public static Fluid fluid(String id) {
        if (id == null || id.isEmpty()) {
            return Fluids.EMPTY;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null || !BuiltInRegistries.FLUID.containsKey(parsed)) {
            return Fluids.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(parsed);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    public static String fluidId(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return "";
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        return key == null ? "" : key.toString();
    }

    private static Set<String> load() {
        try (InputStream stream =
                FluidBarrelFluids.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            try (InputStreamReader reader =
                    new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Document document = GSON.fromJson(reader, Document.class);
                if (document == null
                        || document.schemaVersion != 1
                        || !FluidBarrelCatalog.SOURCE_REVISION.equals(
                                document.sourceRevision)
                        || document.names == null
                        || !document.names.contains("water")
                        || !document.names.contains("lava")) {
                    throw new IllegalStateException(
                            "Simple-fluid list failed source checks");
                }
                return Collections.unmodifiableSet(new HashSet<>(document.names));
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load simple-fluid list", exception);
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        List<String> names;
    }
}
