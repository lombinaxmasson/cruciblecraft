package com.masson.cruciblecraft.gametest;

import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrusherBlockEntity;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.blockentity.FireboxBlockEntity;
import com.masson.cruciblecraft.heat.FuelDefinition;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.worldgen.LargeVeinConfiguration;
import com.masson.cruciblecraft.worldgen.LargeVeinLayout;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Final-gate block-world coverage. Recipes are those loaded by the production
 * reload listener; no test-only RecipeMaps or processing hosts are used.
 */
@GameTestHolder(CrucibleCraft.MODID)
@PrefixGameTestTemplate(false)
public final class CrucibleCraftGameTests {
    private static final String TEMPLATE = "empty";

    private CrucibleCraftGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void concreteOreRecipesArePublishedToLiveMaps(
            GameTestHelper helper) {
        for (String materialId : List.of(
                "copper", "tin", "iron", "gold", "tungsten")) {
            assertPublishedOreChain(helper, materialId);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 240)
    public static void crusherPauseRollbackResume(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.BRONZE_CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(pos);
        ItemStack raw = material("copper", MaterialPrefixes.RAW_ORE, 1);
        crusher.inventory().setStackInSlot(0, raw);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(crusher.progress() == 0, "Crusher advanced without KU");
                    helper.assertTrue(crusher.inventory().getStackInSlot(0).getCount() == 1,
                            "Underpower consumed crusher input");
                    crusher.inventory().setStackInSlot(1, new ItemStack(Items.BEDROCK, 64));
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(crusher.progress() == 0, "Blocked crusher advanced");
                    helper.assertTrue(crusher.inventory().getStackInSlot(0).getCount() == 1,
                            "Blocked crusher consumed input");
                    crusher.inventory().setStackInSlot(1, ItemStack.EMPTY);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(crusher.duration() > 0, "Crusher did not select real recipe");
                    crusher.runtime().processor().setProgress(crusher.duration() - 1);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(crusher.inventory().getStackInSlot(0).isEmpty(),
                            "Crusher did not consume raw ore");
                    helper.assertTrue(!crusher.inventory().getStackInSlot(1).isEmpty(),
                            "Crusher did not produce crushed ore");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 220)
    public static void crusherHonorsDeclaredRecipeDuration(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.BRONZE_CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(pos);
        ItemStack raw = material("copper", MaterialPrefixes.RAW_ORE, 1);
        RecipeMap.Match liveRecipe = ModRecipeMaps.CRUSHER.findMatch(
                GTRecipeQuery.items(raw.copy())).orElseThrow();
        int declaredDuration = liveRecipe.recipe().duration();
        int[] maxObservedProgress = {0};
        boolean[] completed = {false};
        crusher.inventory().setStackInSlot(CrusherBlockEntity.INPUT_SLOT, raw);

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crusher.duration() == declaredDuration,
                            "Crusher did not expose the live recipe's declared duration");
                    helper.assertTrue(
                            crusher.progress() == 0,
                            "Unpowered crusher advanced before duration test");
                    fillKuCapability(helper, pos);
                })
                .thenExecuteFor(declaredDuration + 8, () -> {
                    if (completed[0]) {
                        return;
                    }
                    ItemStack output = crusher.inventory().getStackInSlot(
                            CrusherBlockEntity.OUTPUT_SLOT);
                    if (!output.isEmpty()) {
                        helper.assertTrue(
                                maxObservedProgress[0] == declaredDuration - 1,
                                "Crusher completed before all declared duration ticks");
                        helper.assertTrue(
                                crusher.inventory().getStackInSlot(
                                        CrusherBlockEntity.INPUT_SLOT).isEmpty(),
                                "Duration-complete crusher retained its input");
                        completed[0] = true;
                        return;
                    }
                    helper.assertTrue(
                            !crusher.inventory().getStackInSlot(
                                    CrusherBlockEntity.INPUT_SLOT).isEmpty(),
                            "Crusher consumed input before declared duration elapsed");
                    helper.assertTrue(
                            crusher.progress() < declaredDuration,
                            "Crusher reached declared duration without completing atomically");
                    maxObservedProgress[0] = Math.max(
                            maxObservedProgress[0], crusher.progress());
                    fillKuCapability(helper, pos);
                })
                .thenExecute(() -> helper.assertTrue(
                        completed[0],
                        "Crusher did not complete after its declared duration"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tungstenJsonProvidesCrusherIngotToDustRecipe(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.BRONZE_CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(pos);
        crusher.inventory().setStackInSlot(
                0, material("tungsten", MaterialPrefixes.INGOT, 1));
        ItemStack expectedDust = material("tungsten", MaterialPrefixes.DUST, 1);
        fillKuCapability(helper, pos);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, crusher);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(crusher.inventory().getStackInSlot(0).isEmpty(),
                            "Crusher did not consume tungsten ingot");
                    ItemStack output = crusher.inventory().getStackInSlot(1);
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(output, expectedDust),
                            "Crusher did not produce tungsten dust");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void waterMachinesCapabilityAndCompletion(GameTestHelper helper) {
        BlockPos sluicePos = new BlockPos(3, 2, 3);
        BlockPos bathPos = new BlockPos(8, 2, 3);
        ConfiguredProcessingMachineBlockEntity sluice =
                placeConfigured(helper, sluicePos, ModBlocks.SLUICE.get(), ModProcessingMachines.SLUICE);
        ConfiguredProcessingMachineBlockEntity bath =
                placeConfigured(helper, bathPos, ModBlocks.BATH.get(), ModProcessingMachines.BATH);
        ItemStack crushed = material("copper", MaterialPrefixes.CRUSHED_ORE, 1);
        sluice.inventory().setStackInSlot(0, crushed.copy());
        bath.inventory().setStackInSlot(0, crushed.copy());
        assertWaterPolicy(helper, sluice);
        assertWaterPolicy(helper, bath);
        fillKuCapability(helper, sluicePos);
        fillKuCapability(helper, bathPos);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, sluice);
                    forceLastTick(helper, bath);
                    fillKuCapability(helper, sluicePos);
                    fillKuCapability(helper, bathPos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(sluice.inventory().getStackInSlot(0).isEmpty(),
                            "Sluice did not consume input");
                    helper.assertTrue(bath.inventory().getStackInSlot(0).isEmpty(),
                            "Bath did not consume input");
                    helper.assertTrue(hasAnyOutput(sluice), "Sluice produced no output");
                    helper.assertTrue(hasAnyOutput(bath), "Bath produced no output");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void smelterAboveFireboxUsesRealHeat(GameTestHelper helper) {
        BlockPos fireboxPos = new BlockPos(4, 1, 4);
        BlockPos smelterPos = fireboxPos.above();
        helper.setBlock(fireboxPos, ModBlocks.FIREBOX.get());
        ConfiguredProcessingMachineBlockEntity smelter =
                placeConfigured(helper, smelterPos, ModBlocks.SMELTER.get(), ModProcessingMachines.SMELTER);
        FireboxBlockEntity firebox = helper.getBlockEntity(fireboxPos);
        helper.assertTrue(firebox.addFuel(FuelDefinition.CHARCOAL), "Could not fuel real firebox");
        smelter.inventory().setStackInSlot(0, material("copper", MaterialPrefixes.DUST, 1));
        IEnergyHandler heat = helper.getLevel().getCapability(
                ModCapabilities.ENERGY, helper.absolutePos(fireboxPos), Direction.UP);
        helper.assertTrue(heat != null && heat.handles(EnergyType.HEAT, Direction.UP),
                "Firebox UP HEAT capability missing");

        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    helper.assertTrue(smelter.progress() > 0,
                            "Smelter above firebox did not receive real HEAT");
                    forceLastTick(helper, smelter);
                })
                .thenIdle(2)
                .thenExecute(() ->
                        helper.assertTrue(hasAnyOutput(smelter), "Smelter did not complete"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void oreChainCopperAcrossPlacedMachines(GameTestHelper helper) {
        runOreChain(helper, "copper");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void oreChainTinAcrossPlacedMachines(GameTestHelper helper) {
        runOreChain(helper, "tin");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void oreChainIronAcrossPlacedMachines(GameTestHelper helper) {
        runOreChain(helper, "iron");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void oreChainGoldAcrossPlacedMachines(GameTestHelper helper) {
        runOreChain(helper, "gold");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 520)
    public static void tungstenWorldgenLootAndMachineChain(GameTestHelper helper) {
        ItemStack minedRawOre = placeTungstenVeinAndMineRawOre(helper);
        runOreChain(helper, "tungsten", minedRawOre);
    }

    private static void runOreChain(GameTestHelper helper, String materialId) {
        runOreChain(
                helper,
                materialId,
                material(materialId, MaterialPrefixes.RAW_ORE, 1));
    }

    private static void runOreChain(
            GameTestHelper helper, String materialId, ItemStack rawOreInput) {
        assertPublishedOreChain(helper, materialId);
        ItemStack expectedRawOre =
                material(materialId, MaterialPrefixes.RAW_ORE, 1);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(rawOreInput, expectedRawOre),
                materialId + " chain input is not its canonical raw ore");
        List<ProcessingMachineSpec> specs = List.of(
                ModProcessingMachines.SLUICE,
                ModProcessingMachines.CENTRIFUGE,
                ModProcessingMachines.SHREDDER,
                ModProcessingMachines.SIFTER,
                ModProcessingMachines.SMELTER);
        List<Block> blocks = List.of(
                ModBlocks.SLUICE.get(),
                ModBlocks.CENTRIFUGE.get(),
                ModBlocks.SHREDDER.get(),
                ModBlocks.SIFTER.get(),
                ModBlocks.SMELTER.get());
        List<ConfiguredProcessingMachineBlockEntity> machines = new java.util.ArrayList<>();
        for (int i = 0; i < specs.size(); i++) {
            machines.add(placeConfigured(
                    helper, new BlockPos(3 + i * 4, 2, 8), blocks.get(i), specs.get(i)));
        }
        BlockPos chainFireboxPos = new BlockPos(19, 1, 8);
        helper.setBlock(chainFireboxPos, ModBlocks.FIREBOX.get());
        FireboxBlockEntity chainFirebox = helper.getBlockEntity(chainFireboxPos);
        helper.assertTrue(chainFirebox.addFuel(FuelDefinition.CHARCOAL),
                "Could not fuel chain smelter firebox");
        CrusherBlockEntity crusher;
        BlockPos crusherPos = new BlockPos(3, 2, 13);
        helper.setBlock(crusherPos, ModBlocks.BRONZE_CRUSHER.get());
        crusher = helper.getBlockEntity(crusherPos);
        rawOreInput.setCount(1);
        crusher.inventory().setStackInSlot(0, rawOreInput);
        fillKuCapability(helper, crusherPos);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, crusher);
                    fillKuCapability(helper, crusherPos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack crushed = crusher.inventory().extractItem(1, 1, false);
                    helper.assertTrue(!crushed.isEmpty(), "Chain crusher output missing");
                    machines.get(0).inventory().setStackInSlot(0, crushed);
                    fillWater(machines.get(0));
                    fillKu(helper, machines.get(0));
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, machines.get(0));
                    fillKu(helper, machines.get(0));
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    transferPrimary(helper, machines.get(0), machines.get(1));
                    fillKu(helper, machines.get(1));
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, machines.get(1));
                    fillKu(helper, machines.get(1));
                })
                .thenIdle(2)
                .thenExecute(() -> transferPrimary(helper, machines.get(1), machines.get(2)))
                .thenExecute(() -> fillKu(helper, machines.get(2)))
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, machines.get(2));
                    fillKu(helper, machines.get(2));
                })
                .thenIdle(2)
                .thenExecute(() -> transferPrimary(helper, machines.get(2), machines.get(3)))
                .thenExecute(() -> fillKu(helper, machines.get(3)))
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, machines.get(3));
                    fillKu(helper, machines.get(3));
                })
                .thenIdle(2)
                .thenExecute(() -> transferPrimary(helper, machines.get(3), machines.get(4)))
                .thenIdle(4)
                .thenExecute(() -> forceLastTick(helper, machines.get(4)))
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack expected = material(
                            materialId, MaterialPrefixes.INGOT, 1);
                    helper.assertTrue(
                            machines.get(4).spec().items().outputs().stream()
                                    .map(slot -> machines.get(4).inventory()
                                            .getStackInSlot(slot))
                                    .anyMatch(stack -> stack.is(expected.getItem())),
                            materialId + " chain did not reach its ingot");
                })
                .thenSucceed();
    }

    private static ItemStack placeTungstenVeinAndMineRawOre(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceLocation featureId = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "large_tungsten_vein");
        Registry<ConfiguredFeature<?, ?>> registry =
                level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        ResourceKey<ConfiguredFeature<?, ?>> featureKey =
                ResourceKey.create(Registries.CONFIGURED_FEATURE, featureId);
        ConfiguredFeature<?, ?> configured = registry.get(featureKey);
        helper.assertTrue(
                configured != null,
                "Runtime configured-feature registry lacks " + featureId);
        helper.assertTrue(
                configured.config() instanceof LargeVeinConfiguration,
                featureId + " did not decode as a large vein");
        LargeVeinConfiguration config =
                (LargeVeinConfiguration) configured.config();

        ChunkPos testChunk = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        int baseRegionX = Math.floorDiv(testChunk.x, config.regionSizeChunks()) + 64;
        int baseRegionZ = Math.floorDiv(testChunk.z, config.regionSizeChunks()) + 64;
        int selectedRegionX = 0;
        int selectedRegionZ = 0;
        LargeVeinLayout.Anchor anchor = null;
        for (int index = 0; index < 256 && anchor == null; index++) {
            int regionX = baseRegionX + index % 16;
            int regionZ = baseRegionZ + index / 16;
            if (LargeVeinLayout.generationRoll(
                    level.getSeed(), regionX, regionZ, config.salt())
                    < config.generationChance()) {
                selectedRegionX = regionX;
                selectedRegionZ = regionZ;
                anchor = LargeVeinLayout.anchor(
                        level.getSeed(),
                        regionX,
                        regionZ,
                        config.regionSizeChunks(),
                        config.salt());
            }
        }
        helper.assertTrue(anchor != null, "No deterministic tungsten test region accepted");
        LargeVeinLayout.Anchor selectedAnchor = anchor;
        long veinSeed =
                LargeVeinLayout.veinSeed(level.getSeed(), selectedAnchor, config.salt());
        int centerX = selectedAnchor.x() * 16 + 8;
        int centerZ = selectedAnchor.z() * 16 + 8;
        int centerY =
                LargeVeinLayout.centerY(veinSeed, config.minY(), config.maxY());

        for (int x = centerX - config.horizontalRadius();
                x <= centerX + config.horizontalRadius();
                x++) {
            for (int z = centerZ - config.horizontalRadius();
                    z <= centerZ + config.horizontalRadius();
                    z++) {
                BlockState host = Math.floorMod(x + z, 2) == 0
                        ? Blocks.STONE.defaultBlockState()
                        : Blocks.DEEPSLATE.defaultBlockState();
                for (int y = centerY - config.verticalRadius();
                        y <= centerY + config.verticalRadius();
                        y++) {
                    level.setBlock(new BlockPos(x, y, z), host, 2);
                }
            }
        }

        boolean placed = configured.place(
                level,
                level.getChunkSource().getGenerator(),
                RandomSource.create(veinSeed),
                new BlockPos(
                        selectedAnchor.x() * 16,
                        centerY,
                        selectedAnchor.z() * 16));
        helper.assertTrue(
                placed,
                "Runtime configured feature did not place at recomputed anchor "
                        + selectedRegionX + "," + selectedRegionZ);

        Block stoneOre = ModBlocks.oreBlock("tungsten", Host.STONE).get();
        Block deepslateOre = ModBlocks.oreBlock("tungsten", Host.DEEPSLATE).get();
        int stoneCount = 0;
        int deepslateCount = 0;
        BlockPos minedPos = null;
        for (int x = centerX - config.horizontalRadius();
                x <= centerX + config.horizontalRadius();
                x++) {
            for (int z = centerZ - config.horizontalRadius();
                    z <= centerZ + config.horizontalRadius();
                    z++) {
                for (int y = centerY - config.verticalRadius();
                        y <= centerY + config.verticalRadius();
                        y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = level.getBlockState(pos).getBlock();
                    if (block != stoneOre && block != deepslateOre) {
                        continue;
                    }
                    boolean expectedStone = Math.floorMod(x + z, 2) == 0;
                    helper.assertTrue(
                            block == (expectedStone ? stoneOre : deepslateOre),
                            "Tungsten ore host adaptation disagrees with replaced host at "
                                    + pos);
                    if (block == stoneOre) {
                        stoneCount++;
                    } else {
                        deepslateCount++;
                    }
                    if (minedPos == null) {
                        minedPos = pos.immutable();
                    }
                }
            }
        }
        helper.assertTrue(
                stoneCount > 0 && deepslateCount > 0,
                "Runtime tungsten vein did not adapt both stone and deepslate hosts");
        helper.assertTrue(minedPos != null, "Runtime tungsten vein placed no ore");

        BlockState minedState = level.getBlockState(minedPos);
        List<ItemStack> drops = Block.getDrops(
                minedState,
                level,
                minedPos,
                level.getBlockEntity(minedPos),
                null,
                new ItemStack(Items.DIAMOND_PICKAXE));
        ItemStack expectedRaw =
                material("tungsten", MaterialPrefixes.RAW_ORE, 1);
        ItemStack actualRaw = drops.stream()
                .filter(stack ->
                        ItemStack.isSameItemSameComponents(stack, expectedRaw))
                .findFirst()
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(
                !actualRaw.isEmpty(),
                "Real tungsten ore loot table did not drop canonical raw ore: " + drops);
        helper.assertTrue(
                drops.stream().allMatch(stack -> stack.is(expectedRaw.getItem())),
                "Tungsten ore loot included a non-raw-ore entry: " + drops);
        level.setBlock(minedPos, Blocks.AIR.defaultBlockState(), 3);
        actualRaw.setCount(1);
        return actualRaw;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void assemblerCableAtomicRollbackAndNbt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        ConfiguredProcessingMachineBlockEntity assembler =
                placeConfigured(helper, pos, ModBlocks.ASSEMBLER.get(), ModProcessingMachines.ASSEMBLER);
        RecipeMap.Entry cable = ModRecipeMaps.ASSEMBLER.entries().stream()
                .filter(entry -> entry.id().getPath().contains(
                        "wire_and_rubber_to_cable/copper"))
                .findFirst().orElseThrow();
        loadRecipeInputs(assembler, cable.recipe());
        fillKuCapability(helper, pos);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(assembler.duration() > 0, "Cable recipe not selected");
                    CompoundTag saved = assembler.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    helper.assertTrue(saved.contains("selected_recipe_fingerprint")
                                    && saved.contains("rolled_outputs_valid"),
                            "Selected chance outputs were not persisted");
                    int persistedProgress = assembler.progress();
                    helper.assertTrue(saved.contains("inventory")
                                    && saved.getLong("energy") > 0L,
                            "Capability mutations were not present in saved state");
                    assembler.loadWithComponents(saved, helper.getLevel().registryAccess());
                    helper.assertTrue(assembler.progress() == persistedProgress
                                    && !assembler.inventory().getStackInSlot(0).isEmpty()
                                    && !assembler.inventory().getStackInSlot(1).isEmpty(),
                            "Machine NBT load did not restore progress and inputs");
                    CompoundTag update = assembler.getUpdateTag(
                            helper.getLevel().registryAccess());
                    helper.assertTrue(update.contains("tank_count")
                                    && update.contains("status")
                                    && update.contains("power_demand"),
                            "Client update tag is missing symmetric processing keys");
                    assembler.inventory().setStackInSlot(
                            assembler.spec().items().outputs().getFirst(),
                            new ItemStack(Items.BEDROCK, 64));
                    forceLastTick(helper, assembler);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(!assembler.inventory().getStackInSlot(0).isEmpty()
                                    && !assembler.inventory().getStackInSlot(1).isEmpty(),
                            "Blocked assembler partially consumed cable inputs");
                    assembler.inventory().setStackInSlot(
                            assembler.spec().items().outputs().getFirst(), ItemStack.EMPTY);
                    forceLastTick(helper, assembler);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(assembler.inventory().getStackInSlot(0).isEmpty()
                                    && assembler.inventory().getStackInSlot(1).isEmpty(),
                            "Assembler did not atomically consume cable inputs");
                    helper.assertTrue(hasAnyOutput(assembler), "Assembler produced no cable");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 360)
    public static void everyT3PlacedMachineAdvancesRealRecipe(GameTestHelper helper) {
        List<Block> blocks = List.of(
                ModBlocks.EXTRUDER.get(), ModBlocks.CUTTER.get(), ModBlocks.LATHE.get(),
                ModBlocks.ROLLINGMILL.get(), ModBlocks.ROLLBENDER.get(),
                ModBlocks.WIREMILL.get(), ModBlocks.BENDER.get(), ModBlocks.ASSEMBLER.get(),
                ModBlocks.WELDER.get(), ModBlocks.PRESS.get());
        List<ConfiguredProcessingMachineBlockEntity> machines = new java.util.ArrayList<>();
        for (int i = 0; i < ModProcessingMachines.T3_MACHINES.size(); i++) {
            BlockPos pos = new BlockPos(2 + (i % 5) * 4, 2, 2 + (i / 5) * 6);
            ProcessingMachineSpec spec = ModProcessingMachines.T3_MACHINES.get(i);
            ConfiguredProcessingMachineBlockEntity machine =
                    placeConfigured(helper, pos, blocks.get(i), spec);
            GTRecipe recipe = spec.requireRecipeMap().recipes().getFirst();
            loadRecipeInputs(machine, recipe);
            fillKuCapability(helper, pos);
            machines.add(machine);
        }
        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    for (ConfiguredProcessingMachineBlockEntity machine : machines) {
                        helper.assertTrue(machine.duration() > 0,
                                machine.spec().id() + " did not resolve its real map recipe");
                        helper.assertTrue(machine.progress() > 0,
                                machine.spec().id() + " did not advance");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void everyLiveT3RecipeTracesToGeneratedComponentJson(
            GameTestHelper helper) {
        Map<RecipeMap, Integer> expected = Map.of(
                ModRecipeMaps.EXTRUDER, 2782,
                ModRecipeMaps.CUTTER, 651,
                ModRecipeMaps.LATHE, 929,
                ModRecipeMaps.ROLLINGMILL, 336,
                ModRecipeMaps.ROLLBENDER, 438,
                ModRecipeMaps.WIREMILL, 282,
                ModRecipeMaps.BENDER, 638,
                ModRecipeMaps.ASSEMBLER, 568,
                ModRecipeMaps.WELDER, 321,
                ModRecipeMaps.PRESS, 1191);
        int total = 0;
        for (ProcessingMachineSpec spec : ModProcessingMachines.T3_MACHINES) {
            RecipeMap map = spec.requireRecipeMap();
            helper.assertTrue(
                    map.entries().size() == expected.get(map),
                    map.id() + " live count drifted from generated component manifest");
            total += map.entries().size();
            for (RecipeMap.Entry entry : map.entries()) {
                String expandedPath = entry.id().getPath();
                int materialSeparator = expandedPath.lastIndexOf('/');
                helper.assertTrue(
                        materialSeparator > 0,
                        "T3 recipe lacks expanded material suffix: " + entry.id());
                String sourcePath = "data/" + entry.id().getNamespace() + "/recipe/"
                        + expandedPath.substring(0, materialSeparator) + ".json";
                helper.assertTrue(
                        CrucibleCraftGameTests.class.getClassLoader()
                                .getResource(sourcePath) != null,
                        "Live T3 recipe has no component-rule JSON source: "
                                + entry.id() + " -> " + sourcePath);
            }
        }
        helper.assertTrue(total == 8136, "Live T3 recipe total is not 8136: " + total);
        for (RecipeMap.Entry entry : ModRecipeMaps.EXTRUDER.entries()) {
            GTRecipe recipe = entry.recipe();
            helper.assertTrue(
                    recipe.itemInputs().size() == 2
                            && recipe.itemInputCounts().equals(List.of(
                                    recipe.itemInputCounts().getFirst(), 0))
                            && recipe.itemInputCounts().getFirst() > 0,
                    "Extruder recipe is not material + exact presence-only shape: "
                            + entry.id());
            helper.assertTrue(
                    java.util.Arrays.stream(recipe.itemInputs().get(1).getItems())
                            .allMatch(ExtruderShapeCatalog::isShape),
                    "Extruder recipe retains a no-shape bypass: " + entry.id());
        }
        helper.succeed();
    }

    @GameTest(
            template = TEMPLATE,
            batch = "component_runtime_copper",
            timeoutTicks = 2400)
    public static void componentChainCopperThroughRealMachines(GameTestHelper helper) {
        runComponentChain(helper, "copper");
    }

    @GameTest(
            template = TEMPLATE,
            batch = "component_runtime_tin",
            timeoutTicks = 2400)
    public static void componentChainTinThroughRealMachines(GameTestHelper helper) {
        runComponentChain(helper, "tin");
    }

    @GameTest(
            template = TEMPLATE,
            batch = "component_runtime_iron",
            timeoutTicks = 2400)
    public static void componentChainIronThroughRealMachines(GameTestHelper helper) {
        runComponentChain(helper, "iron");
    }

    @GameTest(
            template = TEMPLATE,
            batch = "component_runtime_gold",
            timeoutTicks = 2400)
    public static void componentChainGoldThroughRealMachines(GameTestHelper helper) {
        runComponentChain(helper, "gold");
    }

    private static void runComponentChain(GameTestHelper helper, String materialId) {
        ItemStack ingot = material(materialId, MaterialPrefixes.INGOT, 64);
        ItemStack plate = material(materialId, MaterialPrefixes.PLATE, 64);
        ItemStack foil = material(materialId, MaterialPrefixes.FOIL, 64);
        ItemStack shape = new ItemStack(ModItems.extruderShape("long_rod").get());
        shape.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("component-chain-" + materialId));
        ItemStack originalShape = shape.copy();

        RecipeMap.Match rollingRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.ROLLINGMILL,
                materialId,
                "rollingmill/ingot_to_plate/",
                MaterialPrefixes.PLATE,
                GTRecipeQuery.items(ingot));
        RecipeMap.Match assemblerRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.ASSEMBLER,
                materialId,
                "assembler/plates_to_gear/",
                MaterialPrefixes.GEAR,
                GTRecipeQuery.items(plate));
        RecipeMap.Match latheRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.LATHE,
                materialId,
                "lathe/ingot_to_rods/",
                MaterialPrefixes.ROD,
                GTRecipeQuery.items(ingot));
        RecipeMap.Match extruderRecipe = requireComponentRecipeByPath(
                helper,
                ModRecipeMaps.EXTRUDER,
                materialId,
                "extruder/long_rod/" + materialId + "/",
                MaterialPrefixes.LONG_ROD);
        RecipeMap.Match wireRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.WIREMILL,
                materialId,
                "wiremill/ingot_to_wire/",
                MaterialPrefixes.WIRE,
                GTRecipeQuery.items(ingot));
        RecipeMap.Match cutterRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.CUTTER,
                materialId,
                "cutter/plate_to_foil/",
                MaterialPrefixes.FOIL,
                GTRecipeQuery.items(plate));
        RecipeMap.Match fineWireRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.WIREMILL,
                materialId,
                "wiremill/foil_to_fine_wire/",
                MaterialPrefixes.FINE_WIRE,
                GTRecipeQuery.items(foil));
        helper.assertTrue(
                extruderRecipe.recipe().itemInputs().size() == 2
                        && extruderRecipe.recipe().itemInputCounts().get(1) == 0
                        && extruderRecipe.recipe().itemInputs().get(1).test(shape),
                materialId + " extruder route does not require its exact shape presence-only");

        ConfiguredProcessingMachineBlockEntity rolling = placeConfigured(
                helper, new BlockPos(2, 2, 2),
                ModBlocks.ROLLINGMILL.get(), ModProcessingMachines.ROLLINGMILL);
        ConfiguredProcessingMachineBlockEntity assembler = placeConfigured(
                helper, new BlockPos(6, 2, 2),
                ModBlocks.ASSEMBLER.get(), ModProcessingMachines.ASSEMBLER);
        ConfiguredProcessingMachineBlockEntity lathe = placeConfigured(
                helper, new BlockPos(10, 2, 2),
                ModBlocks.LATHE.get(), ModProcessingMachines.LATHE);
        ConfiguredProcessingMachineBlockEntity extruder = placeConfigured(
                helper, new BlockPos(14, 2, 2),
                ModBlocks.EXTRUDER.get(), ModProcessingMachines.EXTRUDER);
        ConfiguredProcessingMachineBlockEntity wiremill = placeConfigured(
                helper, new BlockPos(18, 2, 2),
                ModBlocks.WIREMILL.get(), ModProcessingMachines.WIREMILL);
        ConfiguredProcessingMachineBlockEntity cutter = placeConfigured(
                helper, new BlockPos(22, 2, 2),
                ModBlocks.CUTTER.get(), ModProcessingMachines.CUTTER);

        int cutterCycles = cyclesFor(
                fineWireRecipe.recipe().itemInputCounts().getFirst(),
                cutterRecipe.recipe().itemOutputs().getFirst().getCount());
        int neededPlates = assemblerRecipe.recipe().itemInputCounts().getFirst()
                + cutterRecipe.recipe().itemInputCounts().getFirst() * cutterCycles;
        int rollingCycles = cyclesFor(
                neededPlates, rollingRecipe.recipe().itemOutputs().getFirst().getCount());
        ComponentStage rollingStage = stage(
                rolling, rollingRecipe, rollingCycles, materialId + " rollingmill");
        ComponentStage latheStage = stage(
                lathe, latheRecipe, 1, materialId + " lathe");
        ComponentStage extruderStage = stage(
                extruder, extruderRecipe, 1, materialId + " extruder");
        ComponentStage wireStage = stage(
                wiremill, wireRecipe, 1, materialId + " wiremill ingot");

        insertExact(
                helper, rolling, 0,
                ingredientSample(rollingRecipe.recipe(), 0, rollingCycles),
                rollingStage.label());
        insertExact(
                helper, lathe, 0,
                ingredientSample(latheRecipe.recipe(), 0, 1),
                latheStage.label());
        insertExact(
                helper, extruder, 0,
                ingredientSample(extruderRecipe.recipe(), 0, 1),
                extruderStage.label());
        insertExact(helper, extruder, 1, shape, extruderStage.label() + " shape");
        insertExact(
                helper, wiremill, 0,
                ingredientSample(wireRecipe.recipe(), 0, 1),
                wireStage.label());

        ComponentStage[] firstPhase = {
                rollingStage, latheStage, extruderStage, wireStage
        };
        int firstPhaseTicks = phaseTicks(firstPhase);
        final ItemStack[] producedFoil = {ItemStack.EMPTY};

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> assertDeclaredDurations(helper, firstPhase))
                .thenExecuteFor(
                        firstPhaseTicks,
                        () -> powerUntilOutput(helper, firstPhase))
                .thenExecute(() -> {
                    ItemStack producedPlates = takeStageOutput(helper, rollingStage);
                    takeStageOutput(helper, latheStage);
                    takeStageOutput(helper, extruderStage);
                    takeStageOutput(helper, wireStage);
                    assertShapeUnchanged(helper, extruder, originalShape, materialId);

                    int assemblerPlates =
                            assemblerRecipe.recipe().itemInputCounts().getFirst();
                    int cutterPlates =
                            cutterRecipe.recipe().itemInputCounts().getFirst() * cutterCycles;
                    ItemStack gearInput = producedPlates.split(assemblerPlates);
                    ItemStack cutterInput = producedPlates.split(cutterPlates);
                    helper.assertTrue(
                            gearInput.getCount() == assemblerPlates
                                    && cutterInput.getCount() == cutterPlates,
                            materialId + " rollingmill output could not feed both plate routes");
                    insertExact(
                            helper, assembler, 0, gearInput,
                            materialId + " assembler plate transfer");
                    insertExact(
                            helper, cutter, 0, cutterInput,
                            materialId + " cutter plate transfer");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    assertDeclaredDuration(helper, assembler, assemblerRecipe, materialId);
                    assertDeclaredDuration(helper, cutter, cutterRecipe, materialId);
                })
                .thenExecuteFor(
                        phaseTicks(
                                stage(assembler, assemblerRecipe, 1,
                                        materialId + " assembler"),
                                stage(cutter, cutterRecipe, cutterCycles,
                                        materialId + " cutter")),
                        new Runnable() {
                            private final ComponentStage[] phase = {
                                    stage(assembler, assemblerRecipe, 1,
                                            materialId + " assembler"),
                                    stage(cutter, cutterRecipe, cutterCycles,
                                            materialId + " cutter")
                            };
                            @Override public void run() {
                                powerUntilOutput(helper, phase);
                            }
                        })
                .thenExecute(() -> {
                    ComponentStage gearStage =
                            stage(assembler, assemblerRecipe, 1, materialId + " assembler");
                    ComponentStage foilStage =
                            stage(cutter, cutterRecipe, cutterCycles, materialId + " cutter");
                    takeStageOutput(helper, gearStage);
                    producedFoil[0] = takeStageOutput(helper, foilStage);
                    int fineWireFoils =
                            fineWireRecipe.recipe().itemInputCounts().getFirst();
                    ItemStack fineWireInput = producedFoil[0].split(fineWireFoils);
                    helper.assertTrue(
                            fineWireInput.getCount() == fineWireFoils,
                            materialId + " cutter output could not feed fine-wire route");
                    insertExact(
                            helper, wiremill, 0, fineWireInput,
                            materialId + " wiremill foil transfer");
                })
                .thenIdle(2)
                .thenExecute(() ->
                        assertDeclaredDuration(helper, wiremill, fineWireRecipe, materialId))
                .thenExecuteFor(
                        fineWireRecipe.recipe().duration() + 12,
                        () -> powerUntilOutput(
                                helper,
                                stage(
                                        wiremill,
                                        fineWireRecipe,
                                        1,
                                        materialId + " wiremill foil")))
                .thenExecute(() -> {
                    takeStageOutput(
                            helper,
                            stage(
                                    wiremill,
                                    fineWireRecipe,
                                    1,
                                    materialId + " wiremill foil"));
                    assertShapeUnchanged(helper, extruder, originalShape, materialId);
                    CrucibleCraft.LOGGER.info(
                            "component-runtime {}: rollingmill {}t -> {}; assembler {}t -> {}; "
                                    + "lathe {}t -> {}; extruder {}t -> {}; wiremill-ingot {}t -> {}; "
                                    + "cutter {}t -> {}; wiremill-foil {}t -> {}",
                            materialId,
                            rollingRecipe.recipe().duration(),
                            rollingRecipe.recipe().itemOutputs().getFirst(),
                            assemblerRecipe.recipe().duration(),
                            assemblerRecipe.recipe().itemOutputs().getFirst(),
                            latheRecipe.recipe().duration(),
                            latheRecipe.recipe().itemOutputs().getFirst(),
                            extruderRecipe.recipe().duration(),
                            extruderRecipe.recipe().itemOutputs().getFirst(),
                            wireRecipe.recipe().duration(),
                            wireRecipe.recipe().itemOutputs().getFirst(),
                            cutterRecipe.recipe().duration(),
                            cutterRecipe.recipe().itemOutputs().getFirst(),
                            fineWireRecipe.recipe().duration(),
                            fineWireRecipe.recipe().itemOutputs().getFirst());
                })
                .thenSucceed();
    }

    private static RecipeMap.Match requireComponentRecipe(
            GameTestHelper helper,
            RecipeMap map,
            String materialId,
            String expectedPathPrefix,
            com.masson.cruciblecraft.api.material.MaterialPrefix expectedOutput,
            GTRecipeQuery query) {
        RecipeMap.Match match = map.findMatch(query).orElse(null);
        helper.assertTrue(
                match != null,
                map.id() + " has no live component recipe for " + materialId);
        helper.assertTrue(
                match.id().getPath().startsWith(expectedPathPrefix),
                map.id() + " resolved unexpected recipe " + match.id());
        GTRecipe recipe = match.recipe();
        helper.assertTrue(
                recipe.itemOutputs().size() == 1
                        && recipe.outputChances().equals(List.of(GTRecipe.GUARANTEED_CHANCE))
                        && ItemStack.isSameItemSameComponents(
                                recipe.itemOutputs().getFirst(),
                                material(materialId, expectedOutput, 1)),
                match.id() + " does not guarantee the requested " + expectedOutput);
        return match;
    }

    private static RecipeMap.Match requireComponentRecipeByPath(
            GameTestHelper helper,
            RecipeMap map,
            String materialId,
            String expectedPathPrefix,
            com.masson.cruciblecraft.api.material.MaterialPrefix expectedOutput) {
        RecipeMap.Entry entry = map.entries().stream()
                .filter(candidate ->
                        candidate.id().getPath().startsWith(expectedPathPrefix))
                .findFirst()
                .orElse(null);
        helper.assertTrue(
                entry != null,
                map.id() + " has no live component recipe for " + materialId);
        GTRecipe recipe = entry.recipe();
        helper.assertTrue(
                recipe.itemOutputs().size() == 1
                        && recipe.outputChances().equals(List.of(GTRecipe.GUARANTEED_CHANCE))
                        && ItemStack.isSameItemSameComponents(
                                recipe.itemOutputs().getFirst(),
                                material(materialId, expectedOutput, 1)),
                entry.id() + " does not guarantee the requested " + expectedOutput);
        return new RecipeMap.Match(entry.id(), recipe);
    }

    private static ComponentStage stage(
            ConfiguredProcessingMachineBlockEntity machine,
            RecipeMap.Match match,
            int cycles,
            String label) {
        return new ComponentStage(machine, match, cycles, label);
    }

    private static int cyclesFor(int needed, int perCycle) {
        return Math.floorDiv(needed + perCycle - 1, perCycle);
    }

    private static int phaseTicks(ComponentStage... stages) {
        return java.util.Arrays.stream(stages)
                .mapToInt(stage -> Math.multiplyExact(
                        stage.match().recipe().duration(), stage.cycles()))
                .max()
                .orElseThrow() + 12;
    }

    private static ItemStack ingredientSample(
            GTRecipe recipe, int index, int cycles) {
        ItemStack input = recipe.itemInputs().get(index).getItems()[0].copy();
        input.setCount(Math.multiplyExact(recipe.itemInputCounts().get(index), cycles));
        return input;
    }

    private static void insertExact(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine,
            int inputSlot,
            ItemStack input,
            String label) {
        IItemHandler automation = machine.items(Direction.WEST);
        helper.assertTrue(automation != null, label + " input capability missing");
        ItemStack remainder = automation.insertItem(inputSlot, input, false);
        helper.assertTrue(remainder.isEmpty(), label + " rejected real input transfer");
    }

    private static void assertDeclaredDurations(
            GameTestHelper helper, ComponentStage... stages) {
        for (ComponentStage stage : stages) {
            assertDeclaredDuration(
                    helper, stage.machine(), stage.match(), stage.label());
        }
    }

    private static void assertDeclaredDuration(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine,
            RecipeMap.Match match,
            String label) {
        helper.assertTrue(
                machine.duration() == match.recipe().duration(),
                label + " selected duration " + machine.duration()
                        + " instead of live recipe duration " + match.recipe().duration());
    }

    private static void powerUntilOutput(
            GameTestHelper helper, ComponentStage... stages) {
        for (ComponentStage stage : stages) {
            if (!stageOutputReady(stage)) {
                fillKuCapability(helper, stage.machine());
            }
        }
    }

    private static boolean stageOutputReady(ComponentStage stage) {
        ItemStack expected = stage.match().recipe().itemOutputs().getFirst();
        ItemStack actual = stage.machine().inventory().getStackInSlot(
                stage.machine().spec().items().outputs().getFirst());
        return ItemStack.isSameItemSameComponents(actual, expected)
                && actual.getCount() >= Math.multiplyExact(
                        expected.getCount(), stage.cycles());
    }

    private static ItemStack takeStageOutput(
            GameTestHelper helper, ComponentStage stage) {
        ItemStack expected = stage.match().recipe().itemOutputs().getFirst();
        int expectedCount = Math.multiplyExact(expected.getCount(), stage.cycles());
        int outputSlot = stage.machine().spec().items().outputs().getFirst();
        ItemStack observed = stage.machine().inventory().getStackInSlot(outputSlot);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(observed, expected)
                        && observed.getCount() >= expectedCount,
                stage.label() + " did not expose its real target output; observed "
                        + observed + ", expected " + expected.copyWithCount(expectedCount));
        IItemHandler automation = stage.machine().items(Direction.NORTH);
        helper.assertTrue(automation != null, stage.label() + " output capability missing");
        ItemStack extracted = automation.extractItem(0, expectedCount, false);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(extracted, expected)
                        && extracted.getCount() == expectedCount,
                stage.label() + " output capability did not transfer the observed output");
        return extracted;
    }

    private static void assertShapeUnchanged(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity extruder,
            ItemStack originalShape,
            String materialId) {
        ItemStack retained = extruder.inventory().getStackInSlot(1);
        helper.assertTrue(
                retained.getCount() == originalShape.getCount()
                        && ItemStack.isSameItemSameComponents(retained, originalShape),
                materialId + " extruder consumed or changed the shape stack");
        int shapeCount = 0;
        for (int slot = 0; slot < extruder.inventory().getSlots(); slot++) {
            ItemStack stack = extruder.inventory().getStackInSlot(slot);
            if (ExtruderShapeCatalog.isShape(stack)) {
                shapeCount += stack.getCount();
            }
        }
        helper.assertTrue(shapeCount == 1, materialId + " extruder duplicated its shape");
    }

    private record ComponentStage(
            ConfiguredProcessingMachineBlockEntity machine,
            RecipeMap.Match match,
            int cycles,
            String label) {}

    private static ConfiguredProcessingMachineBlockEntity placeConfigured(
            GameTestHelper helper, BlockPos pos, Block block, ProcessingMachineSpec spec) {
        helper.setBlock(pos, block);
        ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(machine.spec() == spec, "Placed block resolved wrong machine spec");
        return machine;
    }

    private static void assertPublishedOreChain(
            GameTestHelper helper, String materialId) {
        assertPublished(
                helper,
                ModRecipeMaps.CRUSHER,
                "crusher",
                materialId,
                GTRecipeQuery.items(material(
                        materialId, MaterialPrefixes.RAW_ORE, 64)));
        assertPublished(
                helper,
                ModRecipeMaps.SLUICE,
                "sluice",
                materialId,
                new GTRecipeQuery(
                        List.of(material(
                                materialId,
                                MaterialPrefixes.CRUSHED_ORE,
                                64)),
                        List.of(new FluidStack(Fluids.WATER, 1_000))));
        assertPublished(
                helper,
                ModRecipeMaps.CENTRIFUGE,
                "centrifuge",
                materialId,
                GTRecipeQuery.items(material(
                        materialId,
                        MaterialPrefixes.WASHED_CRUSHED_ORE,
                        64)));
        assertPublished(
                helper,
                ModRecipeMaps.SHREDDER,
                "shredder",
                materialId,
                GTRecipeQuery.items(material(
                        materialId,
                        MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
                        64)));
        assertPublished(
                helper,
                ModRecipeMaps.SIFTER,
                "sifter",
                materialId,
                GTRecipeQuery.items(material(
                        materialId, MaterialPrefixes.PURIFIED_DUST, 64)));
        assertPublished(
                helper,
                ModRecipeMaps.SMELTER,
                "smelter",
                materialId,
                GTRecipeQuery.items(material(
                        materialId, MaterialPrefixes.DUST, 64)));
    }

    private static void assertPublished(
            GameTestHelper helper,
            RecipeMap map,
            String mapName,
            String materialId,
            GTRecipeQuery query) {
        RecipeMap.Match match = map.findMatch(query).orElse(null);
        helper.assertTrue(
                match != null,
                mapName + " did not resolve " + materialId + " from the live map");
        helper.assertTrue(
                match.id().getPath().startsWith(
                        "ore_chain/" + mapName + "/" + materialId + "/"),
                mapName + "/" + materialId
                        + " resolved a non-concrete recipe " + match.id());
    }

    private static void assertWaterPolicy(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        IFluidHandler external = machine.fluids(Direction.WEST);
        helper.assertTrue(external != null, "Water input capability missing");
        helper.assertTrue(external.fill(
                new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) == 0,
                "Machine accepted wrong fluid");
        helper.assertTrue(external.fill(
                new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "Machine rejected recipe water");
        helper.assertTrue(external.drain(
                100, IFluidHandler.FluidAction.SIMULATE).isEmpty(),
                "External input capability allowed drain");
    }

    private static void fillWater(ConfiguredProcessingMachineBlockEntity machine) {
        IFluidHandler handler = machine.fluids(Direction.WEST);
        if (handler != null) {
            handler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private static void fillKuCapability(GameTestHelper helper, BlockPos pos) {
        BlockPos worldPos = helper.getBlockEntity(pos).getBlockPos();
        fillKuCapability(helper, worldPos, pos.toString());
    }

    private static void fillKuCapability(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        fillKuCapability(helper, machine.getBlockPos(), machine.spec().id().toString());
    }

    private static void fillKuCapability(
            GameTestHelper helper, BlockPos worldPos, String label) {
        IEnergyHandler energy = helper.getLevel().getCapability(
                ModCapabilities.ENERGY, worldPos, Direction.SOUTH);
        helper.assertTrue(energy != null, "Back KU capability missing at " + label);
        long accepted = energy.insert(EnergyType.KINETIC, 256L, 16L, Direction.SOUTH, false);
        helper.assertTrue(accepted > 0L || energy.stored(EnergyType.KINETIC) > 0L,
                "KU capability accepted no energy at " + label);
    }

    private static void fillKu(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        long accepted = machine.insert(
                EnergyType.KINETIC, 256L, 16L, Direction.SOUTH, false);
        helper.assertTrue(accepted > 0L || machine.stored(EnergyType.KINETIC) > 0L,
                "Placed machine accepted no KU: " + machine.spec().id());
    }

    private static void forceLastTick(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        helper.assertTrue(machine.duration() > 0, machine.spec().id() + " has no selected recipe");
        machine.runtime().processor().setProgress(machine.duration() - 1);
    }

    private static void forceLastTick(GameTestHelper helper, CrusherBlockEntity machine) {
        helper.assertTrue(machine.duration() > 0, "Crusher has no selected recipe");
        machine.runtime().processor().setProgress(machine.duration() - 1);
    }

    private static void loadRecipeInputs(
            ConfiguredProcessingMachineBlockEntity machine, GTRecipe recipe) {
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack sample = recipe.itemInputs().get(i).getItems()[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(machine.spec().items().inputs().get(i), sample);
        }
        for (int i = 0; i < recipe.fluidInputs().size(); i++) {
            machine.tanks().get(machine.spec().fluids().inputs().get(i).index())
                    .setFluid(recipe.fluidInputs().get(i).copy());
        }
    }

    private static void transferPrimary(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity from,
            ConfiguredProcessingMachineBlockEntity to) {
        ItemStack output = ItemStack.EMPTY;
        for (int slot : from.spec().items().outputs()) {
            if (!from.inventory().getStackInSlot(slot).isEmpty()) {
                output = from.inventory().extractItem(slot, 1, false);
                break;
            }
        }
        helper.assertTrue(!output.isEmpty(), from.spec().id() + " primary output missing");
        to.inventory().setStackInSlot(to.spec().items().inputs().getFirst(), output);
    }

    private static boolean hasAnyOutput(ConfiguredProcessingMachineBlockEntity machine) {
        return machine.spec().items().outputs().stream()
                .anyMatch(slot -> !machine.inventory().getStackInSlot(slot).isEmpty());
    }

    private static ItemStack material(String id, com.masson.cruciblecraft.api.material.MaterialPrefix prefix, int count) {
        Item item = MaterialLookup.item(id, prefix).orElseThrow();
        return new ItemStack(item, count);
    }
}
