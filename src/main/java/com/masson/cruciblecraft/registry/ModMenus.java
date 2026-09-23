package com.masson.cruciblecraft.registry;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.menu.CokeOvenMenu;
import com.masson.cruciblecraft.content.menu.CrusherMenu;
import com.masson.cruciblecraft.content.menu.ConfiguredProcessingMachineMenu;
import com.masson.cruciblecraft.content.menu.HopperMenu;
import com.masson.cruciblecraft.content.menu.StorageMenu;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CrucibleCraft.MODID);
    private static final IdentityHashMap<
            ProcessingMachineSpec,
            DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>>
            PROCESSING_MENUS = new IdentityHashMap<>();

    public static final DeferredHolder<MenuType<?>, MenuType<HopperMenu>> HOPPER =
            MENUS.register(
                    "hopper",
                    () -> IMenuTypeExtension.create(
                            (containerId, inventory, data) ->
                                    HopperMenu.client(
                                            containerId,
                                            inventory,
                                            data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> BOOKSHELF =
            MENUS.register(
                    "bookshelf",
                    () -> IMenuTypeExtension.create(
                            (containerId, inventory, data) ->
                                    StorageMenu.clientBookshelf(
                                            containerId,
                                            inventory,
                                            data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> BOTTLE_CRATE =
            MENUS.register(
                    "bottle_crate",
                    () -> IMenuTypeExtension.create(
                            (containerId, inventory, data) ->
                                    StorageMenu.clientCrate(
                                            containerId,
                                            inventory,
                                            data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> DRAWER =
            MENUS.register(
                    "drawer",
                    () -> IMenuTypeExtension.create(
                            (containerId, inventory, data) ->
                                    StorageMenu.clientDrawer(
                                            containerId,
                                            inventory,
                                            data.readBlockPos(),
                                            data.readVarInt())));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> MTE_STORAGE =
            MENUS.register(
                    "mte_storage",
                    () -> IMenuTypeExtension.create(
                            (containerId, inventory, data) ->
                                    StorageMenu.clientInPlace(
                                            containerId,
                                            inventory,
                                            data.readBlockPos(),
                                            data.readVarInt(),
                                            data.readVarInt())));
    public static final DeferredHolder<MenuType<?>, MenuType<CokeOvenMenu>> COKE_OVEN =
            MENUS.register(
                    "coke_oven",
                    () -> new MenuType<>(CokeOvenMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<CrusherMenu>> CRUSHER =
            MENUS.register(
                    "bronze_crusher",
                    () -> new MenuType<>(CrusherMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> SLUICE =
            processing("sluice", ModProcessingMachines.SLUICE);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> BATH =
            processing("bath", ModProcessingMachines.BATH);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> CENTRIFUGE =
            processing("centrifuge", ModProcessingMachines.CENTRIFUGE);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> SHREDDER =
            processing("shredder", ModProcessingMachines.SHREDDER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> SIFTER =
            processing("sifter", ModProcessingMachines.SIFTER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> SMELTER =
            processing("smelter", ModProcessingMachines.SMELTER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> MELTER =
            processing("melter", ModProcessingMachines.MELTER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> MORTAR =
            processing("mortar", ModProcessingMachines.MORTAR);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> EXTRUDER =
            processing("extruder", ModProcessingMachines.EXTRUDER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> CUTTER =
            processing("cutter", ModProcessingMachines.CUTTER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> LATHE =
            processing("lathe", ModProcessingMachines.LATHE);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> ROLLINGMILL =
            processing("rollingmill", ModProcessingMachines.ROLLINGMILL);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> ROLLFORMER =
            processing("rollformer", ModProcessingMachines.ROLLFORMER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> SANDING =
            processing("sanding", ModProcessingMachines.SANDING);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> OVEN =
            processing("oven", ModProcessingMachines.OVEN);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> CLUSTERMILL =
            processing("clustermill", ModProcessingMachines.CLUSTERMILL);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> SLICER =
            processing("slicer", ModProcessingMachines.SLICER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> LAMINATOR =
            processing("laminator", ModProcessingMachines.LAMINATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> PRESSUREWASHER =
            processing("pressurewasher", ModProcessingMachines.PRESSUREWASHER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> LOOM =
            processing("loom", ModProcessingMachines.LOOM);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> ELECTRICLOOM =
            processing("electricloom", ModProcessingMachines.ELECTRICLOOM);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> INJECTOR =
            processing("injector", ModProcessingMachines.INJECTOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> NANOFAB =
            processing("nanofab", ModProcessingMachines.NANOFAB);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> ROLLBENDER =
            processing("rollbender", ModProcessingMachines.ROLLBENDER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> WIREMILL =
            processing("wiremill", ModProcessingMachines.WIREMILL);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> BENDER =
            processing("bender", ModProcessingMachines.BENDER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> ASSEMBLER =
            processing("assembler", ModProcessingMachines.ASSEMBLER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> PRESS =
            processing("press", ModProcessingMachines.PRESS);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            ELECTROLYZER = processing("electrolyzer", ModProcessingMachines.ELECTROLYZER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> MIXER =
            processing("mixer", ModProcessingMachines.MIXER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            DISTILLERY = processing("distillery", ModProcessingMachines.DISTILLERY);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            DISTILLATION_TOWER = processing(
                    "distillation_tower",
                    ModProcessingMachines.DISTILLATION_TOWER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            CRYO_DISTILLATION_TOWER = processing(
                    "cryo_distillation_tower",
                    ModProcessingMachines.CRYO_DISTILLATION_TOWER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            AUTOCLAVE = processing("autoclave", ModProcessingMachines.AUTOCLAVE);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            DRYING = processing("drying", ModProcessingMachines.DRYING);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            COMPRESSOR = processing("compressor", ModProcessingMachines.COMPRESSOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            GENERIFIER = processing("generifier", ModProcessingMachines.GENERIFIER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            ROASTER = processing("roaster", ModProcessingMachines.ROASTER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            COAGULATOR = processing("coagulator", ModProcessingMachines.COAGULATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            CANNER = processing("canner", ModProcessingMachines.CANNER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            SQUEEZER = processing("squeezer", ModProcessingMachines.SQUEEZER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            LASER_ENGRAVER = processing("laser_engraver", ModProcessingMachines.LASER_ENGRAVER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            LASER_WELDER = processing("laser_welder", ModProcessingMachines.LASER_WELDER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            PRINTER = processing("printer", ModProcessingMachines.PRINTER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            SCANNER = processing("scanner", ModProcessingMachines.SCANNER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            AUTOCRAFTER = processing("autocrafter", ModProcessingMachines.AUTOCRAFTER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            ELECTRIC_MIXER = processing("electric_mixer", ModProcessingMachines.ELECTRIC_MIXER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            BOXINATOR = processing("boxinator", ModProcessingMachines.BOXINATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            LIGHTNING = processing("lightning", ModProcessingMachines.LIGHTNING);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            PLANTALYZER = processing("plantalyzer", ModProcessingMachines.PLANTALYZER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            BUMBLELYZER = processing("bumblelyzer", ModProcessingMachines.BUMBLELYZER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            MASSFAB = processing("massfab", ModProcessingMachines.MASSFAB);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            REPLICATOR = processing("replicator", ModProcessingMachines.REPLICATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            FREEZER = processing("freezer", ModProcessingMachines.FREEZER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            CRYO_MIXER = processing("cryo_mixer", ModProcessingMachines.CRYO_MIXER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            POLARIZER = processing("polarizer", ModProcessingMachines.POLARIZER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            MAGNETIC_SEPARATOR = processing(
                    "magnetic_separator", ModProcessingMachines.MAGNETIC_SEPARATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            FERMENTER = processing("fermenter", ModProcessingMachines.FERMENTER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            LARGE_OVEN = processing("large_oven", ModProcessingMachines.LARGE_OVEN);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            LARGE_CRUSHER = processing(
                    "large_crusher", ModProcessingMachines.LARGE_CRUSHER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            LARGE_SHREDDER = processing(
                    "large_shredder", ModProcessingMachines.LARGE_SHREDDER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            IMPLOSION_COMPRESSOR = processing(
                    "implosion_compressor",
                    ModProcessingMachines.IMPLOSION_COMPRESSOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            LARGE_MATTER_FABRICATOR = processing(
                    "large_matter_fabricator",
                    ModProcessingMachines.LARGE_MATTER_FABRICATOR);

    static {
        validateProcessingMenuMapping(
                menuHostSpecs(),
                PROCESSING_MENUS);
    }

    private static DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> processing(
            String id,
            ProcessingMachineSpec spec) {
        DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> holder =
                MENUS.register(id, () -> IMenuTypeExtension.create(
                        (containerId, inventory, data) ->
                                new ConfiguredProcessingMachineMenu(
                                        containerId,
                                        inventory,
                                        spec,
                                        data.readBlockPos())));
        DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> previous =
                PROCESSING_MENUS.put(spec, holder);
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate processing menu mapping for " + spec.id());
        }
        return holder;
    }

    public static DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> forMachine(
            ProcessingMachineSpec spec) {
        DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> holder =
                PROCESSING_MENUS.get(spec);
        if (holder == null) {
            throw new IllegalArgumentException(
                    "No processing menu registered for machine " + spec.id());
        }
        return holder;
    }

    public static List<DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>>
            processingMenus() {
        return menuHostSpecs().stream()
                .map(ModMenus::forMachine)
                .toList();
    }

    static List<ProcessingMachineSpec> menuHostSpecs() {
        return java.util.stream.Stream.concat(
                        ModProcessingMachines.CONFIGURED_MACHINES.stream(),
                        ModProcessingMachines.MULTIBLOCK_MENU_HOSTS.stream())
                .toList();
    }

    static int processingMenuCount() {
        return PROCESSING_MENUS.size();
    }

    static void validateProcessingMenuMapping(
            Collection<ProcessingMachineSpec> configuredMachines,
            Map<ProcessingMachineSpec, ?> menuMappings) {
        Set<ProcessingMachineSpec> configured =
                java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        configured.addAll(configuredMachines);
        java.util.List<String> missing = configuredMachines.stream()
                .filter(spec -> !menuMappings.containsKey(spec))
                .map(spec -> spec.id().toString())
                .toList();
        java.util.List<String> extra = menuMappings.keySet().stream()
                .filter(spec -> !configured.contains(spec))
                .map(spec -> spec.id().toString())
                .toList();
        if (!missing.isEmpty() || !extra.isEmpty()) {
            throw new IllegalStateException(
                    "Processing menu mapping mismatch: missing=" + missing
                            + ", extra=" + extra);
        }
    }

    private ModMenus() {}
}
