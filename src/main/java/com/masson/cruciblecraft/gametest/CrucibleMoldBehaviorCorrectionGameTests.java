package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.HopperBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteFoundryTanks;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated ceramic crucible / mold behavior correction. Run with
 * {@code -PwaveRecipes=content/gt6-crucible-mold-behavior-correction}.
 */
@GameTestHolder(CrucibleMoldBehaviorCorrectionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CrucibleMoldBehaviorCorrectionGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_crucible_mold_behavior_correction";
    private static final String TEMPLATE = "empty";
    private static final float MELT_TEMPERATURE = 1600.0F;

    private CrucibleMoldBehaviorCorrectionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void crucibleSucksDroppedIngot(GameTestHelper helper) {
        BlockPos cruciblePos = new BlockPos(2, 2, 2);
        helper.setBlock(cruciblePos, ModBlocks.CRUCIBLE.get());
        BlockPos absolute = helper.absolutePos(cruciblePos);
        ItemEntity entity = new ItemEntity(
                helper.getLevel(),
                absolute.getX() + 0.5,
                absolute.getY() + 0.4,
                absolute.getZ() + 0.5,
                new ItemStack(ironIngot()));
        entity.setDeltaMovement(0.0, 0.0, 0.0);
        entity.setPickUpDelay(0);
        helper.getLevel().addFreshEntity(entity);
        helper.startSequence()
                .thenIdle(12)
                .thenExecute(() -> {
                    CrucibleBlockEntity crucible = crucibleAt(helper, cruciblePos);
                    helper.assertTrue(
                            crucible.composition().getOrDefault("iron", 0)
                                    >= MaterialPrefixes.INGOT.units(),
                            "dropped ingot was not sucked into the crucible");
                    helper.assertTrue(
                            helper.getLevel()
                                    .getEntitiesOfClass(
                                            ItemEntity.class,
                                            new AABB(helper.absolutePos(cruciblePos))
                                                    .inflate(1.0))
                                    .isEmpty(),
                            "dropped ingot entity was left on the crucible");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void crucibleAcceptsSixteenIngots(GameTestHelper helper) {
        BlockPos cruciblePos = new BlockPos(2, 2, 2);
        helper.setBlock(cruciblePos, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = crucibleAt(helper, cruciblePos);
        helper.assertTrue(
                CrucibleBlockEntity.MAX_INGOTS == 16,
                "single-block crucible capacity drifted from 16 ingots");
        helper.assertTrue(
                insertIngots(crucible, 16) == InsertResult.SUCCESS,
                "ceramic crucible rejected a 16-ingot charge");
        helper.assertTrue(
                insertIngots(crucible, 1) == InsertResult.FULL,
                "ceramic crucible accepted a 17th ingot");
        helper.assertTrue(
                crucible.totalUnits() == MaterialPrefixes.INGOT.units() * 16,
                "ceramic crucible fill did not stop at 16 ingots");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void crucibleHopperInsertsFromTop(GameTestHelper helper) {
        BlockPos cruciblePos = new BlockPos(2, 2, 2);
        BlockPos hopperPos = cruciblePos.above();
        helper.setBlock(cruciblePos, ModBlocks.CRUCIBLE.get());
        helper.setBlock(
                hopperPos,
                hopperBlock("lead_hopper").defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
        hopper.inventory().setStackInSlot(0, new ItemStack(ironIngot(), 1));
        helper.startSequence()
                .thenIdle(16)
                .thenExecute(() -> {
                    CrucibleBlockEntity crucible = crucibleAt(helper, cruciblePos);
                    helper.assertTrue(
                            crucible.composition().getOrDefault("iron", 0)
                                    >= MaterialPrefixes.INGOT.units(),
                            "top hopper did not insert into the crucible");
                    helper.assertTrue(
                            hopper.inventory().getStackInSlot(0).isEmpty(),
                            "top hopper kept the ingot");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void moldDoesNotAutoPullByDefault(GameTestHelper helper) {
        BlockPos cruciblePos = new BlockPos(2, 2, 2);
        BlockPos moldPos = new BlockPos(3, 2, 2);
        placeMoltenCrucible(helper, cruciblePos);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
                    helper.assertTrue(
                            !mold.isFilled() && !mold.autoPulls(Direction.WEST),
                            "empty mold auto-pulled without a configured side");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void moldAutoPullsWhenSideEnabled(GameTestHelper helper) {
        BlockPos cruciblePos = new BlockPos(2, 2, 2);
        BlockPos moldPos = new BlockPos(3, 2, 2);
        placeMoltenCrucible(helper, cruciblePos);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        helper.assertTrue(
                mold.toggleAutoPull(Direction.WEST),
                "west auto-pull did not enable");
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            mold.isFilled() && "iron".equals(mold.materialId()),
                            "enabled mold side did not pull molten iron");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void moldRedstoneGatesAutoPull(GameTestHelper helper) {
        BlockPos cruciblePos = new BlockPos(2, 2, 2);
        BlockPos moldPos = new BlockPos(3, 2, 2);
        placeMoltenCrucible(helper, cruciblePos);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        helper.assertTrue(
                mold.toggleAutoPull(Direction.WEST),
                "west auto-pull did not enable");
        helper.assertTrue(
                mold.toggleRedstoneMode(),
                "redstone auto-input did not enable");
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            !mold.isFilled(),
                            "redstone-gated mold pulled without a signal");
                    helper.setBlock(moldPos.north(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            mold.isFilled() && "iron".equals(mold.materialId()),
                            "redstone-gated mold did not pull after a neighbor signal");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void moldHopperExtractsWhenCool(GameTestHelper helper) {
        BlockPos hopperPos = new BlockPos(2, 2, 2);
        BlockPos moldPos = hopperPos.above();
        helper.setBlock(
                hopperPos,
                hopperBlock("lead_hopper").defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        mold.fill(new CrucibleBlockEntity.CastTransfer(
                MaterialCatalog.require("copper"),
                MaterialPrefixes.INGOT,
                1,
                ItemHeat.AMBIENT_TEMPERATURE));
        helper.startSequence()
                .thenIdle(16)
                .thenExecute(() -> {
                    HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
                    ItemStack extracted = hopper.inventory().getStackInSlot(0);
                    helper.assertTrue(
                            extracted.is(copperIngot()) && extracted.getCount() == 1,
                            "cool mold did not extract into the hopper below");
                    helper.assertTrue(
                            !mold.isFilled(),
                            "cool mold kept its contents after hopper extract");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void foundryTankCapacitiesMatchGt6Units(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "foundry/smelting_crucible_invar", MteInPlaceKind.CRUCIBLE_FOUNDRY);
        assertFoundryCapacity(
                helper, "foundry/smelting_crucible_invar", MteFoundryTanks.SMELTERY_MB);
        assertFoundryCapacity(helper, "foundry/mold_stone", MteFoundryTanks.MOLD_MB);
        assertFoundryCapacity(helper, "foundry/basin_stone", MteFoundryTanks.BASIN_MB);
        assertFoundryCapacity(
                helper, "foundry/crucible_crossing_stone", MteFoundryTanks.CROSSING_MB);
        helper.succeed();
    }

    private static void assertFoundryCapacity(
            GameTestHelper helper, String path, int expectedMb) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(
                pos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id(path))
                        .get()
                        .defaultBlockState());
        MteInPlaceBlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(
                be.tank().getCapacity() == expectedMb,
                path + " tank capacity is " + be.tank().getCapacity()
                        + ", expected " + expectedMb);
    }

    private static void placeMoltenCrucible(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = crucibleAt(helper, pos);
        helper.assertTrue(
                insertIngots(crucible, 1, MELT_TEMPERATURE) == InsertResult.SUCCESS,
                "molten setup rejected iron");
        crucible.process().thermal().restore(
                MELT_TEMPERATURE,
                0L,
                0L,
                CrucibleThermalModel.HOT_BUFFER_TICKS,
                false);
        helper.assertTrue(
                crucible.isMolten(),
                "crucible was not molten after heat restore");
    }

    private static InsertResult insertIngots(CrucibleBlockEntity crucible, int count) {
        return insertIngots(crucible, count, ItemHeat.AMBIENT_TEMPERATURE);
    }

    private static InsertResult insertIngots(
            CrucibleBlockEntity crucible, int count, float temperature) {
        InsertResult last = InsertResult.SUCCESS;
        var material = MaterialCatalog.require("iron");
        for (int index = 0; index < count; index++) {
            last = crucible.insert(
                    new MaterialUnits.Entry(
                            material.id(),
                            MaterialPrefixes.INGOT,
                            MaterialPrefixes.INGOT.units()),
                    temperature);
            if (last != InsertResult.SUCCESS) {
                return last;
            }
        }
        return last;
    }

    private static CrucibleBlockEntity crucibleAt(
            GameTestHelper helper, BlockPos pos) {
        if (!(helper.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) {
            helper.fail("missing ceramic crucible at " + pos);
            throw new IllegalStateException("unreachable");
        }
        return crucible;
    }

    private static CeramicMoldBlockEntity moldAt(
            GameTestHelper helper, BlockPos pos) {
        if (!(helper.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold)) {
            helper.fail("missing ceramic mold at " + pos);
            throw new IllegalStateException("unreachable");
        }
        return mold;
    }

    private static HopperBlock hopperBlock(String path) {
        return ModBlocks.hopperBlocksById()
                .get(ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path))
                .get();
    }

    private static Item ironIngot() {
        return MaterialLookup.item("iron", MaterialPrefixes.INGOT).orElseThrow();
    }

    private static Item copperIngot() {
        return MaterialLookup.item("copper", MaterialPrefixes.INGOT).orElseThrow();
    }
}
