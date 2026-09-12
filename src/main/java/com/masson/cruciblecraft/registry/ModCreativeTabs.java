package com.masson.cruciblecraft.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.item.GtWoodCatalog;
import com.masson.cruciblecraft.content.item.BathMteIdentityCatalog;
import com.masson.cruciblecraft.content.item.BathIdentityCatalog;
import com.masson.cruciblecraft.content.item.SemanticObjectCatalog;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.machine.MachineDurabilityComponent;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.processing.DeviceMaterialCatalog;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrucibleCraft.MODID);
    private static final MaterialEntryPlanCache MATERIAL_ENTRY_PLAN =
            new MaterialEntryPlanCache();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cruciblecraft"))
                    .icon(() -> ModItems.FIREBRICK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.FIREBRICK.get());
                        DeviceMaterialCatalog.require(
                                        MachineMaterialRules.Device.CRUCIBLE)
                                .creativeVisible()
                                .forEach(material -> output.accept(
                                        machineVariant(
                                                ModItems.CRUCIBLE.get(),
                                                material.materialId())));
                        DeviceMaterialCatalog.require(
                                        MachineMaterialRules.Device.ANVIL)
                                .creativeVisible()
                                .forEach(material -> output.accept(
                                        machineVariant(
                                                ModItems.ANVIL.get(),
                                                material.materialId())));
                        output.accept(ModItems.COKE_OVEN.get());
                        output.accept(ModItems.MULTIBLOCK_CASING.get());
                        output.accept(ModItems.MULTIBLOCK_ITEM_FLUID_PORT.get());
                        output.accept(ModItems.MULTIBLOCK_ENERGY_INPUT_PORT.get());
                        output.accept(ModItems.LARGE_CENTRIFUGE.get());
                        output.accept(ModItems.DISTILLATION_TOWER.get());
                        output.accept(ModItems.LARGE_BOILER.get());
                        output.accept(ModItems.TANK_3X3X3.get());
                        output.accept(ModItems.LARGE_CRUCIBLE.get());
                        output.accept(ModItems.LASER_ENGRAVER.get());
                        output.accept(ModItems.LU_FIBER_CABLE.get());
                        output.accept(ModItems.FUSION_REACTOR.get());
                        output.accept(ModItems.REACTOR_CORE_1X1.get());
                        output.accept(ModItems.REACTOR_CORE_2X2.get());
                        ModItems.reactorRods().forEach(rod -> output.accept(rod.get()));
                        ModItems.technologicalParts().forEach(part -> output.accept(part.get()));
                        output.accept(ModItems.RAW_CERAMIC_CRUCIBLE.get());
                        output.accept(ModItems.RAW_CERAMIC_MOLD.get());
                        output.accept(ModItems.RAW_INGOT_MOLD.get());
                        output.accept(ModItems.RAW_PLATE_MOLD.get());
                        output.accept(ModItems.RAW_ROD_MOLD.get());
                        output.accept(ModItems.RAW_BOLT_MOLD.get());
                        output.accept(ModItems.INGOT_MOLD.get());
                        output.accept(ModItems.PLATE_MOLD.get());
                        output.accept(ModItems.ROD_MOLD.get());
                        output.accept(ModItems.BOLT_MOLD.get());
                        output.accept(ModItems.MATCH.get());
                        output.accept(ModItems.PROGRAMMED_CIRCUIT.get());
                        output.accept(ModItems.CREOSOTE_BUCKET.get());
                        output.accept(ModItems.STEAM_BUCKET.get());
                        output.accept(ModItems.PORTABLE_FLUID_TANK.get());
                        output.accept(ModItems.FLUID_CELL.get());
                        output.accept(ModItems.GAS_CELL.get());
                        ModItems.batteryCellItemsByPath().values().forEach(
                                item -> output.accept(item.get()));
                        output.accept(ModItems.PIPE_FILTER_COVER.get());
                        output.accept(ModItems.PIPE_VALVE_COVER.get());
                        output.accept(ModItems.PIPE_PUMP_COVER.get());
                        output.accept(ModItems.CONVEYOR_COVER.get());
                        output.accept(ModItems.RETRIEVER_ITEM_COVER.get());
                        output.accept(ModItems.ROBOT_ARM_COVER.get());
                        output.accept(ModItems.PRESSURE_VALVE_COVER.get());
                        output.accept(ModItems.SELECTOR_MANUAL_COVER.get());
                        ModItems.compactElectricCovers().forEach(
                                cover -> output.accept(cover.get()));
                        output.accept(ModItems.LOGISTICS_ITEM_STORAGE_COVER.get());
                        output.accept(ModItems.LOGISTICS_ITEM_IMPORT_COVER.get());
                        output.accept(ModItems.LOGISTICS_ITEM_EXPORT_COVER.get());
                        output.accept(ModItems.LOGISTICS_FLUID_STORAGE_COVER.get());
                        output.accept(ModItems.LOGISTICS_FLUID_IMPORT_COVER.get());
                        output.accept(ModItems.LOGISTICS_FLUID_EXPORT_COVER.get());
                        output.accept(ModItems.LOGISTICS_GENERIC_STORAGE_COVER.get());
                        output.accept(ModItems.LOGISTICS_GENERIC_IMPORT_COVER.get());
                        output.accept(ModItems.LOGISTICS_GENERIC_EXPORT_COVER.get());
                        output.accept(ModItems.LOGISTICS_GENERIC_DUMP_COVER.get());
                        output.accept(ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER.get());
                        output.accept(ModItems.LOGISTICS_DISPLAY_CPU_CONTROL_COVER.get());
                        output.accept(ModItems.LOGISTICS_DISPLAY_CPU_STORAGE_COVER.get());
                        output.accept(ModItems.LOGISTICS_DISPLAY_CPU_CONVERSION_COVER.get());
                        output.accept(ModItems.LOGISTICS_CORE.get());
                        output.accept(ModItems.GALVANIZED_STEEL_WALL.get());
                        output.accept(ModItems.TUNGSTENSTEEL_WALL.get());
                        output.accept(ModItems.STAINLESS_STEEL_WALL.get());
                        output.accept(ModItems.LARGE_IRIDIUM_COIL.get());
                        output.accept(ModItems.VENTILATION_UNIT.get());
                        output.accept(ModItems.VERSATILE_PROCESSOR_UNIT.get());
                        output.accept(ModItems.LOGIC_PROCESSOR_UNIT.get());
                        output.accept(ModItems.CONTROL_PROCESSOR_UNIT.get());
                        output.accept(ModItems.STORAGE_PROCESSOR_UNIT.get());
                        output.accept(ModItems.CONVERSION_PROCESSOR_UNIT.get());
                        ModItems.converterItemsById().values().forEach(
                                item -> output.accept(item.get()));
                        ModItems.batteryItemsById().values().forEach(item -> {
                            output.accept(item.get());
                            output.accept(item.get().fullStack());
                        });
                        ModItems.transformerItemsById().values().forEach(
                                item -> output.accept(item.get()));
                        ModItems.heatExchangerItemsById().values().forEach(
                                item -> output.accept(item.get()));
                        output.accept(ModItems.ROTATIONAL_AXLE.get());
                        output.accept(ModItems.ROTATIONAL_GEARBOX.get());
                        output.accept(ModItems.BRONZE_CRUSHER.get());
                        output.accept(ModItems.SLUICE.get());
                        output.accept(ModItems.BATH.get());
                        // Every registered machine variant from the tier catalog
                        // (opening 33 plus later material/EU/TU rows).
                        ModMachineVariants.ALL.forEach(variant ->
                                output.accept(ModBlocks
                                        .configuredProcessingBlock(variant)
                                        .asItem()));
                        ModBlocks.hopperBlocks().forEach(block ->
                                output.accept(block.get().asItem()));
                        ModBlocks.sensorBlocks().forEach(block ->
                                output.accept(block.get().asItem()));
                        com.masson.cruciblecraft.content.storage.StorageVariantCatalog
                                .sourceVisible()
                                .forEach(variant ->
                                        output.accept(ModBlocks.storageBlocksById()
                                                .get(variant.id())
                                                .get()
                                                .asItem()));
                        output.accept(ModItems.STEEL_DUST_FUNNEL.get());
                        output.accept(ModItems.MORTAR.get());
                        output.accept(ModItems.EXTRUDER.get());
                        output.accept(ModItems.CUTTER.get());
                        output.accept(ModItems.ROLLBENDER.get());
                        output.accept(ModItems.BENDER.get());
                        output.accept(ModItems.ASSEMBLER.get());
                        output.accept(ModItems.WELDER.get());
                        output.accept(ModItems.MIXER.get());
                        output.accept(ModItems.AUTOCLAVE.get());
                        output.accept(ModItems.COMPRESSOR.get());
                        output.accept(ModItems.GENERIFIER.get());
                        output.accept(ModItems.FLUID_DEPOSIT_EXTRACTOR.get());
                        ExtruderShapeCatalog.DEFINITIONS.forEach(shape ->
                                output.accept(ModItems.extruderShape(shape.id()).get()));
                        GtWoodCatalog.DEFINITIONS.forEach(wood -> {
                            output.accept(ModItems.gtWood(wood.id()).get());
                            output.accept(GtWoodCatalog.fireproofStack(
                                    ModItems.gtWood(wood.id()).get()));
                        });
                        GtStoneCatalog.variants().forEach(stone ->
                                output.accept(ModItems.gtStoneItemsById()
                                        .get(stone.id())
                                        .get()));
                        GtBlockObjectCatalog.variants().forEach(block ->
                                output.accept(ModItems.gtBlockObjectItemsById()
                                        .get(block.id())
                                        .get()));
                        com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog.variants().forEach(block ->
                                output.accept(ModItems.bathRemainderBlockObjectItemsById()
                                        .get(block.id())
                                        .get()));
                        ModItems.bathMteItemsById().values().forEach(item ->
                                output.accept(item.get()));
                        BathMteIdentityCatalog.newItems().forEach(identity ->
                                output.accept(ModItems.bathMteItemsById()
                                        .get(identity.id())
                                        .get()));
                        BathIdentityCatalog.identities().forEach(identity -> {
                            if (!"tool_head".equals(identity.kind())) {
                                output.accept(ModItems.bathIdentityItemsById()
                                        .get(identity.id())
                                        .get());
                            }
                        });
                        SemanticObjectCatalog.identities().forEach(identity -> {
                            if (!"tool_head".equals(identity.kind())) {
                                output.accept(ModItems.semanticIdentityItemsById()
                                        .get(identity.id())
                                        .get());
                            }
                        });
                        output.accept(ModItems.FLINT_KNIFE.get());
                        // Tool patterns and material-tool variants live in the
                        // dedicated TOOLS tab; MAIN only keeps the bootstrap knife.
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS =
            CREATIVE_MODE_TABS.register(
                    "tools",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "itemGroup.cruciblecraft.tools"))
                            .icon(() -> ModItems.MATERIAL_WRENCH.get()
                                    .variant("iron"))
                            .displayItems((parameters, output) -> {
                                ModItems.toolPatterns().forEach(pattern ->
                                        output.accept(pattern.get()));
                                com.masson.cruciblecraft.content.item
                                        .ToolDisplayPlan.routedVariantStacks()
                                        .forEach(output::accept);
                                MaterialCreativeTab.toolHeadEntryIds(
                                                MaterialCatalog.startupValues(),
                                                registeredFormsForPlan(),
                                                MaterialCatalog.runtimePreferences())
                                        .forEach(itemId ->
                                                output.accept(requirePlannedItem(itemId)));
                            })
                            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORES =
            materialTab(
                    MaterialCreativeTab.ORES,
                    () -> ModItems.BRONZE_CRUSHER.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORE_PROCESSING =
            materialTab(
                    MaterialCreativeTab.ORE_PROCESSING,
                    () -> ModItems.SLUICE.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DUSTS =
            materialTab(
                    MaterialCreativeTab.DUSTS,
                    () -> ModItems.MORTAR.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> METALS_GEMS =
            materialTab(
                    MaterialCreativeTab.METALS_GEMS,
                    () -> machineVariant(ModItems.ANVIL.get(), "iron"));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PLATES =
            materialTab(
                    MaterialCreativeTab.PLATES,
                    () -> ModItems.PLATE_MOLD.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PARTS =
            materialTab(
                    MaterialCreativeTab.PARTS,
                    () -> ModItems.LATHE.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MECHANICAL_PARTS =
            materialTab(
                    MaterialCreativeTab.MECHANICAL_PARTS,
                    () -> ModItems.ROLLBENDER.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WIRES =
            materialTab(
                    MaterialCreativeTab.WIRES,
                    () -> ModItems.WIREMILL.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CABLES =
            materialTab(
                    MaterialCreativeTab.CABLES,
                    () -> ModItems.ASSEMBLER.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FLUID_CELLS =
            CREATIVE_MODE_TABS.register(
                    "fluid_cells",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "itemGroup.cruciblecraft.fluid_cells"))
                            .icon(() -> ModItems.FLUID_CELL.get()
                                    .getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                com.masson.cruciblecraft.material
                                        .CellContentGate.sortedEntries()
                                        .forEach(entry -> {
                                                net.minecraft.world.level.material
                                                        .Fluid fluid =
                                                        net.minecraft.core.registries
                                                                .BuiltInRegistries.FLUID
                                                                .get(entry.getKey());
                                                if (fluid == net.minecraft.world.level.material
                                                        .Fluids.EMPTY) {
                                                    return;
                                                }
                                                boolean liquid =
                                                        entry.getValue()
                                                                == com.masson.cruciblecraft
                                                                        .material
                                                                        .CellContentGate.Kind
                                                                        .FLUID;
                                                ItemStack cell = new ItemStack(
                                                        (liquid
                                                                ? ModItems.FLUID_CELL
                                                                : ModItems.GAS_CELL)
                                                                .get());
                                                cell.set(
                                                        (liquid
                                                                ? ModComponents
                                                                        .FLUID_CELL_CONTENT
                                                                : ModComponents
                                                                        .GAS_CELL_CONTENT)
                                                                .get(),
                                                        net.neoforged.neoforge.fluids
                                                                .SimpleFluidContent.copyOf(
                                                                new net.neoforged.neoforge.fluids
                                                                        .FluidStack(
                                                                        fluid,
                                                                        1_000)));
                                                output.accept(cell);
                                        });
                                ModItems.batteryCellItemsByPath().values()
                                        .forEach(item -> output.accept(item.get()));
                            })
                            .build());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PIPES =
            materialTab(
                    MaterialCreativeTab.PIPES,
                    () -> ModItems.EXTRUDER.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MISC =
            materialTab(
                    MaterialCreativeTab.MISC,
                    () -> ModItems.UNKNOWN_MATERIAL.get().getDefaultInstance());

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> materialTab(
            MaterialCreativeTab tab,
            Supplier<ItemStack> icon) {
        return CREATIVE_MODE_TABS.register(
                tab.registryName(),
                () -> CreativeModeTab.builder()
                        .title(Component.translatable(tab.translationKey()))
                        .icon(icon)
                        .displayItems((parameters, output) -> {
                            materialEntryPlan().get(tab).forEach(itemId ->
                                    output.accept(requirePlannedItem(itemId)));
                            if (tab == MaterialCreativeTab.CABLES) {
                                output.accept(ModItems.LU_FIBER_CABLE.get());
                            }
                        })
                        .build());
    }

    static Map<MaterialCreativeTab, List<String>> materialEntryPlan() {
        return MATERIAL_ENTRY_PLAN.get(
                MaterialCatalog.runtimeRevision(),
                ModCreativeTabs::buildMaterialEntryPlan);
    }

    private static Map<MaterialCreativeTab, List<String>> buildMaterialEntryPlan() {
        return MaterialCreativeTab.planEntryIds(
                MaterialCatalog.startupValues(),
                registeredFormsForPlan(),
                MaterialCatalog.runtimePreferences());
    }

    private static Map<String, List<MaterialPrefix>> registeredFormsForPlan() {
        Map<String, List<MaterialPrefix>> registeredForms = new LinkedHashMap<>();
        MaterialCatalog.startupValues().forEach(material -> registeredForms.put(
                material.id(),
                MaterialCatalog.registeredForms(material)));
        return registeredForms;
    }

    static Item requirePlannedItem(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location != null) {
            var item = BuiltInRegistries.ITEM.getOptional(location);
            if (item.isPresent()) {
                return item.orElseThrow();
            }
        }
        CrucibleCraft.LOGGER.error(
                "Material creative-tab plan references missing item {}", itemId);
        throw new IllegalStateException(
                "Material creative-tab plan references missing item " + itemId);
    }

    static final class MaterialEntryPlanCache {
        private volatile Snapshot snapshot = new Snapshot(Long.MIN_VALUE, Map.of());

        Map<MaterialCreativeTab, List<String>> get(
                long revision,
                Supplier<Map<MaterialCreativeTab, List<String>>> planner) {
            Snapshot current = snapshot;
            if (current.revision() == revision) {
                return current.plan();
            }
            synchronized (this) {
                current = snapshot;
                if (current.revision() == revision) {
                    return current.plan();
                }
                Map<MaterialCreativeTab, List<String>> rebuilt = planner.get();
                snapshot = new Snapshot(revision, rebuilt);
                return rebuilt;
            }
        }

        private record Snapshot(
                long revision,
                Map<MaterialCreativeTab, List<String>> plan) {}
    }

    private static ItemStack machineVariant(net.minecraft.world.item.Item item, String materialId) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.MACHINE_MATERIAL, materialId);
        if (item == ModItems.ANVIL.get()) {
            long max = MachineMaterialRules.anvilMaxDurability(materialId);
            stack.set(ModComponents.MACHINE_DURABILITY, new MachineDurabilityComponent(max, max));
        }
        return stack;
    }

    private ModCreativeTabs() {}
}
