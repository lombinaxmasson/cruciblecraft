package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mold.MoldRecipes;
import com.masson.cruciblecraft.content.mold.MoldShape;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 crucible / mold interaction. Run with
 * {@code -PwaveRecipes=content/gt6-crucible-mold-interaction}.
 */
@GameTestHolder(CrucibleMoldInteractionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CrucibleMoldInteractionGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_crucible_mold_interaction";
    private static final String TEMPLATE = "empty";
    private static final float MELT_TEMPERATURE = 1600.0F;

    private CrucibleMoldInteractionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void moldChiselORsBitAndCastsNuggets(GameTestHelper helper) {
        BlockPos cruciblePos = new BlockPos(2, 2, 2);
        BlockPos moldPos = new BlockPos(3, 2, 2);
        placeMolten(helper, cruciblePos, "copper", 1);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        mold.setPattern(0);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        clickItem(
                helper,
                moldPos,
                ModItems.MATERIAL_CHISEL.get().variant("iron"),
                player,
                0.20,
                1.0,
                0.20,
                Direction.UP);
        helper.assertTrue(
                (mold.pattern() & 1) != 0,
                "chisel did not OR an inner 5x5 bit");
        helper.assertTrue(
                MoldRecipes.recipe(mold.pattern()).orElse(null) == MaterialPrefixes.NUGGET,
                "unknown chiseled mask was not a nugget recipe");
        helper.assertTrue(
                crucibleAt(helper, cruciblePos).fillMoldAtSide(
                        mold, Direction.EAST, Direction.WEST),
                "nugget mold refused molten copper");
        helper.assertTrue(
                mold.isFilled()
                        && "copper".equals(mold.materialId())
                        && mold.contentsStack().is(
                                MaterialLookup.item("copper", MaterialPrefixes.NUGGET)
                                        .orElseThrow()),
                "chiseled mold did not solidify a copper nugget");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void moldTopEdgePoursOnlyThatNeighbor(GameTestHelper helper) {
        BlockPos ironPos = new BlockPos(2, 2, 2);
        BlockPos moldPos = new BlockPos(3, 2, 2);
        BlockPos copperPos = new BlockPos(4, 2, 2);
        placeMolten(helper, ironPos, "iron", 1);
        placeMolten(helper, copperPos, "copper", 1);
        placeIngotMold(helper, moldPos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        clickEmpty(helper, moldPos, player, 0.10, 1.0, 0.50, Direction.UP);
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        helper.assertTrue(
                mold.isFilled() && "iron".equals(mold.materialId()),
                "top-west edge did not pour the west crucible");
        helper.assertTrue(
                crucibleAt(helper, copperPos).totalUnits()
                        == MaterialPrefixes.INGOT.units(),
                "top-west edge also drained the east crucible");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fillMoldConsumesOneIngot(GameTestHelper helper) {
        BlockPos cruciblePos = new BlockPos(2, 2, 2);
        BlockPos moldPos = new BlockPos(3, 2, 2);
        placeMolten(helper, cruciblePos, "iron", 2);
        CeramicMoldBlockEntity mold = placeIngotMold(helper, moldPos);
        CrucibleBlockEntity crucible = crucibleAt(helper, cruciblePos);
        int before = crucible.totalUnits();
        helper.assertTrue(
                crucible.fillMoldAtSide(mold, Direction.EAST, Direction.WEST),
                "ingot mold refused molten iron");
        helper.assertTrue(
                mold.isFilled()
                        && mold.outputCount() == 1
                        && "iron".equals(mold.materialId()),
                "fillMold did not leave one iron ingot in the mold");
        helper.assertTrue(
                crucible.totalUnits() == before - MaterialPrefixes.INGOT.units(),
                "fillMold did not consume exactly one ingot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pincersPickupSkipsHeatDamage(GameTestHelper helper) {
        BlockPos moldPos = new BlockPos(2, 2, 2);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        mold.fill(hotSolidCopper());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        float health = player.getHealth();
        clickItem(
                helper,
                moldPos,
                ModItems.MATERIAL_PINCERS.get().variant("iron"),
                player,
                0.5,
                1.0,
                0.5,
                Direction.UP);
        helper.assertTrue(
                player.getHealth() == health,
                "pincers pickup applied temperature damage");
        helper.assertTrue(
                player.getInventory().countItem(
                        MaterialLookup.item("copper", MaterialPrefixes.INGOT).orElseThrow())
                        > 0,
                "pincers did not pick up the solidified ingot");
        helper.assertTrue(!mold.isFilled(), "pincers left the mold filled");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emptyHandPickupCanBurn(GameTestHelper helper) {
        BlockPos moldPos = new BlockPos(2, 2, 2);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        mold.fill(hotSolidCopper());
        Pig pig = helper.spawn(EntityType.PIG, moldPos);
        float pigHealth = pig.getHealth();
        helper.assertTrue(
                TemperatureDamage.apply(pig, mold.temperature(), 1.0F, 5.0F),
                "hot solidified copper was below the GT6 heat threshold");
        helper.assertTrue(
                pig.getHealth() < pigHealth,
                "heat damage helper did not hurt the pig");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        clickEmpty(helper, moldPos, player, 0.5, 1.0, 0.5, Direction.UP);
        helper.assertTrue(
                player.getInventory().countItem(
                        MaterialLookup.item("copper", MaterialPrefixes.INGOT).orElseThrow())
                        > 0,
                "empty-hand click did not pick up the solidified ingot");
        helper.assertTrue(!mold.isFilled(), "empty-hand pickup left the mold filled");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void moldContactBurns(GameTestHelper helper) {
        BlockPos moldPos = new BlockPos(2, 2, 2);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        moldAt(helper, moldPos).fill(new CrucibleBlockEntity.CastTransfer(
                MaterialCatalog.require("copper"),
                MaterialPrefixes.INGOT,
                1,
                MELT_TEMPERATURE));
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(2.5, 2.2, 2.5));
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        pig.getHealth() < pig.getMaxHealth(),
                        "hot mold contact did not burn the pig"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void crucibleMeltdownBecomesLava(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.steelSmeltingCrucible().get());
        CrucibleBlockEntity crucible = crucibleAt(helper, pos);
        helper.assertTrue(
                insert(crucible, "iron", 1, MELT_TEMPERATURE) == InsertResult.SUCCESS,
                "meltdown setup rejected iron");
        crucible.process().thermal().restore(
                100_000.0F,
                0L,
                0L,
                CrucibleThermalModel.HOT_BUFFER_TICKS,
                false);
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(
                        helper.getBlockState(pos).is(Blocks.LAVA),
                        "overheated crucible did not become lava"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wrenchRotatesIngotRecipe(GameTestHelper helper) {
        BlockPos moldPos = new BlockPos(2, 2, 2);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        mold.setPattern(MoldShape.INGOT.mask());
        int original = mold.pattern();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wrench = ModItems.MATERIAL_WRENCH.get().variant("iron");
        for (int step = 0; step < 4; step++) {
            clickItem(helper, moldPos, wrench, player, 0.5, 1.0, 0.5, Direction.UP);
        }
        helper.assertTrue(
                mold.pattern() == original,
                "four wrench rotations did not return the ingot mask");
        helper.assertTrue(
                MoldRecipes.recipe(mold.pattern()).orElse(null) == MaterialPrefixes.INGOT,
                "rotated ingot mask lost the ingot recipe");
        helper.assertTrue(
                MoldShape.fromMask(mold.pattern()).orElse(null) == MoldShape.INGOT,
                "four rotations drifted the placed ingot shape");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void faucetPoursCeramicCrucibleIntoMold(GameTestHelper helper) {
        BlockPos cruciblePos = new BlockPos(2, 2, 2);
        BlockPos faucetPos = new BlockPos(2, 2, 3);
        BlockPos moldPos = new BlockPos(2, 1, 3);
        // Stone faucet max is stone melting * 1.25 in Kelvin (~1102 °C). Iron at
        // 1600 °C lavas the faucet; copper just above melt stays under that cap.
        placeMolten(helper, cruciblePos, "copper", 1, 1090.0F);
        placeIngotMold(helper, moldPos);
        helper.setBlock(
                faucetPos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id(
                                "fluid_attachment/crucible_faucet_stone"))
                        .get()
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, Direction.NORTH));
        MteInPlaceBlockEntity faucet = helper.getBlockEntity(faucetPos);
        faucet.transferOnce();
        helper.assertFalse(
                helper.getBlockState(faucetPos).is(Blocks.LAVA),
                "stone faucet melted instead of pouring (pour temperature above stone max)");
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        helper.assertTrue(
                mold.isFilled() && "copper".equals(mold.materialId()),
                "stone faucet facing a ceramic crucible did not fill the mold below");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void moldCuCoolsTemperature(GameTestHelper helper) {
        BlockPos moldPos = new BlockPos(2, 2, 2);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        float before = mold.temperature();
        helper.assertTrue(
                mold.insert(EnergyType.CU, 1L, 40L, Direction.UP, false) == 40L,
                "ceramic mold rejected CU");
        helper.assertTrue(
                mold.temperature() < before,
                "CU did not lower the mold temperature");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void blankMoldRefusesFill(GameTestHelper helper) {
        BlockPos cruciblePos = new BlockPos(2, 2, 2);
        BlockPos moldPos = new BlockPos(3, 2, 2);
        placeMolten(helper, cruciblePos, "iron", 1);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = moldAt(helper, moldPos);
        helper.assertTrue(
                mold.pattern() == 0,
                "fresh ceramic mold was not unshaped");
        helper.assertTrue(
                !crucibleAt(helper, cruciblePos).fillMoldAtSide(
                        mold, Direction.EAST, Direction.WEST),
                "unshaped mold accepted molten iron");
        helper.assertTrue(!mold.isFilled(), "unshaped mold stored a casting");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void customMoldDropKeepsPattern(GameTestHelper helper) {
        BlockPos moldPos = new BlockPos(2, 2, 2);
        helper.setBlock(moldPos, ModBlocks.CERAMIC_MOLD.get());
        moldAt(helper, moldPos).setPattern(1);
        helper.getLevel().destroyBlock(helper.absolutePos(moldPos), true);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemEntity dropped = helper.getLevel()
                            .getEntitiesOfClass(
                                    ItemEntity.class,
                                    new AABB(helper.absolutePos(moldPos))
                                            .inflate(1.25))
                            .stream()
                            .filter(entity -> entity.getItem().is(
                                    ModItems.CERAMIC_MOLD.get()))
                            .findFirst()
                            .orElse(null);
                    helper.assertTrue(
                            dropped != null
                                    && Integer.valueOf(1).equals(
                                            dropped.getItem().get(
                                                    ModComponents.MOLD_PATTERN)),
                            "custom chiseled mold did not drop the blank mold with its pattern");
                })
                .thenSucceed();
    }

    private static CeramicMoldBlockEntity placeIngotMold(
            GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = moldAt(helper, pos);
        mold.setPattern(MoldShape.INGOT.mask());
        return mold;
    }

    private static void placeMolten(
            GameTestHelper helper, BlockPos pos, String materialId, int ingots) {
        placeMolten(helper, pos, materialId, ingots, MELT_TEMPERATURE);
    }

    private static void placeMolten(
            GameTestHelper helper,
            BlockPos pos,
            String materialId,
            int ingots,
            float temperature) {
        helper.setBlock(pos, ModBlocks.steelSmeltingCrucible().get());
        CrucibleBlockEntity crucible = crucibleAt(helper, pos);
        helper.assertTrue(
                insert(crucible, materialId, ingots, temperature) == InsertResult.SUCCESS,
                "molten setup rejected " + materialId);
        crucible.process().thermal().restore(
                temperature,
                0L,
                0L,
                CrucibleThermalModel.HOT_BUFFER_TICKS,
                false);
        helper.assertTrue(crucible.isMolten(), "crucible was not molten after heat restore");
    }

    private static InsertResult insert(
            CrucibleBlockEntity crucible,
            String materialId,
            int count,
            float temperature) {
        InsertResult last = InsertResult.SUCCESS;
        var material = MaterialCatalog.require(materialId);
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

    private static CrucibleBlockEntity.CastTransfer hotSolidCopper() {
        return new CrucibleBlockEntity.CastTransfer(
                MaterialCatalog.require("copper"),
                MaterialPrefixes.INGOT,
                1,
                200.0F);
    }

    private static void clickItem(
            GameTestHelper helper,
            BlockPos pos,
            ItemStack stack,
            Player player,
            double fx,
            double fy,
            double fz,
            Direction face) {
        helper.getBlockState(pos).useItemOn(
                stack,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                hit(helper, pos, fx, fy, fz, face));
    }

    private static void clickEmpty(
            GameTestHelper helper,
            BlockPos pos,
            Player player,
            double fx,
            double fy,
            double fz,
            Direction face) {
        helper.getBlockState(pos).useWithoutItem(
                helper.getLevel(),
                player,
                hit(helper, pos, fx, fy, fz, face));
    }

    private static BlockHitResult hit(
            GameTestHelper helper,
            BlockPos pos,
            double fx,
            double fy,
            double fz,
            Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(
                Vec3.atLowerCornerOf(absolute).add(fx, fy, fz),
                face,
                absolute,
                false);
    }

    private static CrucibleBlockEntity crucibleAt(GameTestHelper helper, BlockPos pos) {
        if (!(helper.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) {
            helper.fail("missing smelting crucible at " + pos);
            throw new IllegalStateException("unreachable");
        }
        return crucible;
    }

    private static CeramicMoldBlockEntity moldAt(GameTestHelper helper, BlockPos pos) {
        if (!(helper.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold)) {
            helper.fail("missing ceramic mold at " + pos);
            throw new IllegalStateException("unreachable");
        }
        return mold;
    }
}
