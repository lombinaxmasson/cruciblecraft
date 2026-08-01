package com.masson.cruciblecraft.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AnvilBlock;
import com.masson.cruciblecraft.content.block.BellowsBlock;
import com.masson.cruciblecraft.content.block.BoilerBlock;
import com.masson.cruciblecraft.content.block.CokeOvenBlock;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.content.block.CrucibleBlock;
import com.masson.cruciblecraft.content.block.CrusherBlock;
import com.masson.cruciblecraft.content.block.FireboxBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CrucibleCraft.MODID);
    private static final Map<OreBlockKey, DeferredBlock<DropExperienceBlock>>
            MATERIAL_ORE_BLOCKS = new LinkedHashMap<>();

    /** M0 placeholder block — later reused as firebox cladding. */
    public static final DeferredBlock<Block> FIREBRICK = BLOCKS.registerSimpleBlock(
            "firebrick",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE));

    public static final DeferredBlock<FireboxBlock> FIREBOX = BLOCKS.register(
            "firebox",
            () -> new FireboxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 8.0F)
                    .lightLevel(state -> state.getValue(FireboxBlock.LIT) ? 13 : 0)
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<CrucibleBlock> CRUCIBLE = BLOCKS.register(
            "crucible",
            () -> new CrucibleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F, 8.0F)
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<AnvilBlock> ANVIL = BLOCKS.register(
            "anvil",
            () -> new AnvilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 1_200.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.ANVIL)));

    public static final DeferredBlock<CokeOvenBlock> COKE_OVEN = BLOCKS.register(
            "coke_oven",
            () -> new CokeOvenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 8.0F)
                    .lightLevel(state -> state.getValue(CokeOvenBlock.LIT) ? 8 : 0)
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<BellowsBlock> BELLOWS = BLOCKS.register(
            "bellows",
            () -> new BellowsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<CeramicMoldBlock> CERAMIC_MOLD = BLOCKS.register(
            "ceramic_mold",
            () -> new CeramicMoldBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5F, 4.0F)
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<LiquidBlock> CREOSOTE = BLOCKS.register(
            "creosote",
            () -> new LiquidBlock(
                    ModFluids.CREOSOTE_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()));

    public static final DeferredBlock<LiquidBlock> STEAM = BLOCKS.register(
            "steam",
            () -> new LiquidBlock(
                    ModFluids.STEAM_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.NONE)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()));

    public static final DeferredBlock<BoilerBlock> BRONZE_BOILER = BLOCKS.register(
            "bronze_boiler",
            () -> new BoilerBlock(machineProperties()));
    public static final DeferredBlock<SteamEngineBlock> BRONZE_STEAM_ENGINE = BLOCKS.register(
            "bronze_steam_engine",
            () -> new SteamEngineBlock(machineProperties()));
    public static final DeferredBlock<CrusherBlock> BRONZE_CRUSHER = BLOCKS.register(
            "bronze_crusher",
            () -> new CrusherBlock(machineProperties()));
    public static final DeferredBlock<ProcessingMachineBlock> SLUICE =
            processing("sluice", ModProcessingMachines.SLUICE);
    public static final DeferredBlock<ProcessingMachineBlock> BATH =
            processing("bath", ModProcessingMachines.BATH);
    public static final DeferredBlock<ProcessingMachineBlock> CENTRIFUGE =
            processing("centrifuge", ModProcessingMachines.CENTRIFUGE);
    public static final DeferredBlock<ProcessingMachineBlock> SHREDDER =
            processing("shredder", ModProcessingMachines.SHREDDER);
    public static final DeferredBlock<ProcessingMachineBlock> SIFTER =
            processing("sifter", ModProcessingMachines.SIFTER);
    public static final DeferredBlock<ProcessingMachineBlock> SMELTER =
            processing("smelter", ModProcessingMachines.SMELTER);
    public static final DeferredBlock<ProcessingMachineBlock> MORTAR =
            processing("mortar", ModProcessingMachines.MORTAR);
    public static final DeferredBlock<ProcessingMachineBlock> EXTRUDER =
            processing("extruder", ModProcessingMachines.EXTRUDER);
    public static final DeferredBlock<ProcessingMachineBlock> CUTTER =
            processing("cutter", ModProcessingMachines.CUTTER);
    public static final DeferredBlock<ProcessingMachineBlock> LATHE =
            processing("lathe", ModProcessingMachines.LATHE);
    public static final DeferredBlock<ProcessingMachineBlock> ROLLINGMILL =
            processing("rollingmill", ModProcessingMachines.ROLLINGMILL);
    public static final DeferredBlock<ProcessingMachineBlock> ROLLBENDER =
            processing("rollbender", ModProcessingMachines.ROLLBENDER);
    public static final DeferredBlock<ProcessingMachineBlock> WIREMILL =
            processing("wiremill", ModProcessingMachines.WIREMILL);
    public static final DeferredBlock<ProcessingMachineBlock> BENDER =
            processing("bender", ModProcessingMachines.BENDER);
    public static final DeferredBlock<ProcessingMachineBlock> ASSEMBLER =
            processing("assembler", ModProcessingMachines.ASSEMBLER);
    public static final DeferredBlock<ProcessingMachineBlock> WELDER =
            processing("welder", ModProcessingMachines.WELDER);
    public static final DeferredBlock<ProcessingMachineBlock> PRESS =
            processing("press", ModProcessingMachines.PRESS);

    public static void registerMaterials(Collection<MaterialDefinition> definitions) {
        if (!MATERIAL_ORE_BLOCKS.isEmpty()) {
            throw new IllegalStateException("Material ore blocks already registered");
        }
        for (MaterialDefinition material : definitions) {
            if (!MaterialCatalog.registeredForms(material).contains(MaterialPrefixes.ORE)) {
                continue;
            }
            for (Host host : Host.values()) {
                OreBlockKey key = new OreBlockKey(material.id(), host);
                MATERIAL_ORE_BLOCKS.put(
                        key,
                        ore(
                                oreRegistryName(material.id(), host),
                                host == Host.DEEPSLATE ? MapColor.DEEPSLATE : MapColor.STONE));
            }
        }
    }

    public static DeferredBlock<DropExperienceBlock> oreBlock(
            String materialId, Host host) {
        DeferredBlock<DropExperienceBlock> block =
                MATERIAL_ORE_BLOCKS.get(new OreBlockKey(materialId, host));
        if (block == null) {
            throw new IllegalArgumentException(
                    "No " + host.name().toLowerCase(java.util.Locale.ROOT)
                            + " ore block for material " + materialId);
        }
        return block;
    }

    public static boolean hasOreBlock(String materialId, Host host) {
        return MATERIAL_ORE_BLOCKS.containsKey(new OreBlockKey(materialId, host));
    }

    public static Collection<DeferredBlock<DropExperienceBlock>> oreBlocks() {
        return Collections.unmodifiableCollection(MATERIAL_ORE_BLOCKS.values());
    }

    /** Immutable material/host-to-path view used by worldgen host adaptation. */
    public static Map<OreBlockKey, String> oreBlockPaths() {
        LinkedHashMap<OreBlockKey, String> paths = new LinkedHashMap<>();
        MATERIAL_ORE_BLOCKS.keySet().forEach(key ->
                paths.put(key, oreRegistryName(key.materialId(), key.host())));
        return Collections.unmodifiableMap(paths);
    }

    public static String oreRegistryName(String materialId, Host host) {
        return host == Host.DEEPSLATE
                ? "deepslate_" + materialId + "_ore"
                : materialId + "_ore";
    }

    private static DeferredBlock<DropExperienceBlock> ore(String id, MapColor color) {
        return BLOCKS.register(
                id,
                () -> new DropExperienceBlock(
                        net.minecraft.util.valueproviders.UniformInt.of(0, 2),
                        BlockBehaviour.Properties.of()
                                .mapColor(color)
                                .strength(3.0F, 3.0F)
                                .requiresCorrectToolForDrops()
                                .sound(SoundType.STONE)));
    }

    private static DeferredBlock<ProcessingMachineBlock> processing(
            String id,
            com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec spec) {
        return BLOCKS.register(id, () -> new ProcessingMachineBlock(spec, machineProperties()));
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(3.5F, 8.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    public record OreBlockKey(String materialId, Host host) {
        public OreBlockKey {
            if (materialId == null || !materialId.matches("[a-z0-9_]+") || host == null) {
                throw new IllegalArgumentException("Invalid material ore block key");
            }
        }
    }

    private ModBlocks() {}
}
