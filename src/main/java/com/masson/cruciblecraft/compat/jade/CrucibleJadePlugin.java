package com.masson.cruciblecraft.compat.jade;

import java.util.Locale;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.config.ModConfig;
import com.masson.cruciblecraft.content.block.AnvilBlock;
import com.masson.cruciblecraft.content.block.CokeOvenBlock;
import com.masson.cruciblecraft.content.block.CrucibleBlock;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.content.block.FireboxBlock;
import com.masson.cruciblecraft.content.block.BoilerBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.block.CrusherBlock;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FireboxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrusherBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class CrucibleJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CrucibleComponentProvider.INSTANCE, CrucibleBlock.class);
        registration.registerBlockComponent(AnvilComponentProvider.INSTANCE, AnvilBlock.class);
        registration.registerBlockComponent(CokeOvenComponentProvider.INSTANCE, CokeOvenBlock.class);
        registration.registerBlockComponent(CeramicMoldComponentProvider.INSTANCE, CeramicMoldBlock.class);
        registration.registerBlockComponent(FireboxComponentProvider.INSTANCE, FireboxBlock.class);
        registration.registerBlockComponent(BoilerComponentProvider.INSTANCE, BoilerBlock.class);
        registration.registerBlockComponent(SteamEngineComponentProvider.INSTANCE, SteamEngineBlock.class);
        registration.registerBlockComponent(CrusherComponentProvider.INSTANCE, CrusherBlock.class);
    }

    private enum BoilerComponentProvider implements IBlockComponentProvider {
        INSTANCE;
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "bronze_boiler");
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof BoilerBlockEntity boiler) {
                tooltip.add(Component.translatable("jade.cruciblecraft.boiler",
                        boiler.waterAmount(), BoilerBlockEntity.WATER_CAPACITY,
                        boiler.steamAmount(), BoilerBlockEntity.STEAM_CAPACITY, boiler.accumulatedHu()));
            }
        }
        @Override public ResourceLocation getUid() { return UID; }
    }

    private enum SteamEngineComponentProvider implements IBlockComponentProvider {
        INSTANCE;
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "bronze_steam_engine");
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof SteamEngineBlockEntity engine) {
                tooltip.add(Component.translatable("jade.cruciblecraft.steam_engine",
                        engine.steamAmount(), SteamEngineBlockEntity.STEAM_CAPACITY,
                        engine.stored(), SteamEngineBlockEntity.KU_CAPACITY,
                        engine.strokeSign() > 0 ? "push" : "return"));
            }
        }
        @Override public ResourceLocation getUid() { return UID; }
    }

    private enum CrusherComponentProvider implements IBlockComponentProvider {
        INSTANCE;
        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "bronze_crusher");
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof CrusherBlockEntity crusher) {
                tooltip.add(Component.translatable("jade.cruciblecraft.crusher",
                        crusher.powerDemand(), crusher.progress(), crusher.duration(), crusher.pausedReason()));
            }
        }
        @Override public ResourceLocation getUid() { return UID; }
    }

    private enum FireboxComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "firebox");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof FireboxBlockEntity firebox)) {
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.firebox_heat",
                    firebox.storedHeat(),
                    firebox.remainingSeconds()));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.firebox_output",
                    firebox.outputRate()));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum CrucibleComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "crucible");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof CrucibleBlockEntity crucible)) {
                return;
            }

            boolean fahrenheit = "F".equalsIgnoreCase(ModConfig.TEMPERATURE_UNIT.get());
            float celsius = crucible.temperatureCelsius();
            float displayed = fahrenheit ? celsius * 9.0f / 5.0f + 32.0f : celsius;
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.casing",
                    Component.translatable("material.cruciblecraft." + crucible.casingMaterialId()),
                    crucible.casingTier(),
                    crucible.processingTier()));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.max_temperature",
                    Math.round(fahrenheit
                            ? crucible.casingMaxTemperature() * 9.0f / 5.0f + 32.0f
                            : crucible.casingMaxTemperature()),
                    fahrenheit ? "°F" : "°C"));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.temperature",
                    String.format(Locale.ROOT, "%.1f", displayed),
                    fahrenheit ? "°F" : "°C"));
            if (!crucible.composition().isEmpty()) {
                String contents = crucible.composition().entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue() + " u")
                        .collect(Collectors.joining(", "));
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.contents",
                        contents,
                        crucible.totalUnits(),
                        CrucibleBlockEntity.maxUnits()));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum AnvilComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "anvil");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof AnvilBlockEntity anvil)) {
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.machine_material",
                    Component.translatable("material.cruciblecraft." + anvil.materialId()),
                    anvil.materialTier()));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.anvil_durability",
                    anvil.durability(),
                    anvil.maxDurability()));
            for (int slot = 0; slot < 2; slot++) {
                if (!anvil.workpiece(slot).isEmpty()) {
                    tooltip.add(Component.translatable(
                            "jade.cruciblecraft.anvil_slot",
                            slot + 1,
                            anvil.workpiece(slot).getCount(),
                            anvil.workpiece(slot).getHoverName()));
                }
            }
            if (anvil.strikes() > 0) {
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.anvil_progress",
                        anvil.strikes()));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum CokeOvenComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "coke_oven");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof CokeOvenBlockEntity cokeOven)) {
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.coke_oven.structure",
                    Component.translatable(cokeOven.structureValid()
                            ? "jade.cruciblecraft.coke_oven.valid"
                            : "jade.cruciblecraft.coke_oven.invalid")));
            if (cokeOven.recipeDuration() > 0) {
                tooltip.add(Component.translatable(
                        "jade.cruciblecraft.coke_oven.progress",
                        cokeOven.progress(),
                        cokeOven.recipeDuration()));
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.coke_oven.creosote",
                    cokeOven.creosoteAmount(),
                    CokeOvenBlockEntity.TANK_CAPACITY));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum CeramicMoldComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "ceramic_mold");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof CeramicMoldBlockEntity mold)) {
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.mold_shape",
                    title(mold.shape().serializedName())));
            if (!mold.isFilled()) {
                tooltip.add(Component.translatable("jade.cruciblecraft.mold_empty"));
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.mold_contents",
                    mold.outputCount(),
                    mold.materialId()));
            tooltip.add(Component.translatable(
                    "jade.cruciblecraft.mold_state",
                    Component.translatable(mold.isSolidified()
                            ? "jade.cruciblecraft.mold_solid"
                            : "jade.cruciblecraft.mold_cooling"),
                    String.format(Locale.ROOT, "%.1f", mold.temperature())));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        private static String title(String value) {
            return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
        }
    }
}
