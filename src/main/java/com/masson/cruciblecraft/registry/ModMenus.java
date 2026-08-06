package com.masson.cruciblecraft.registry;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.menu.CokeOvenMenu;
import com.masson.cruciblecraft.content.menu.CrusherMenu;
import com.masson.cruciblecraft.content.menu.ConfiguredProcessingMachineMenu;
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
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> ROLLBENDER =
            processing("rollbender", ModProcessingMachines.ROLLBENDER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> WIREMILL =
            processing("wiremill", ModProcessingMachines.WIREMILL);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> BENDER =
            processing("bender", ModProcessingMachines.BENDER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> ASSEMBLER =
            processing("assembler", ModProcessingMachines.ASSEMBLER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> WELDER =
            processing("welder", ModProcessingMachines.WELDER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> PRESS =
            processing("press", ModProcessingMachines.PRESS);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            ELECTROLYZER = processing("electrolyzer", ModProcessingMachines.ELECTROLYZER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>> MIXER =
            processing("mixer", ModProcessingMachines.MIXER);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            DISTILLERY = processing("distillery", ModProcessingMachines.DISTILLERY);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            AUTOCLAVE = processing("autoclave", ModProcessingMachines.AUTOCLAVE);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            DRYING = processing("drying", ModProcessingMachines.DRYING);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            COMPRESSOR = processing("compressor", ModProcessingMachines.COMPRESSOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ConfiguredProcessingMachineMenu>>
            GENERIFIER = processing("generifier", ModProcessingMachines.GENERIFIER);

    static {
        validateProcessingMenuMapping(
                ModProcessingMachines.CONFIGURED_MACHINES,
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
