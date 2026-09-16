package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.blockentity.LaserEngraverBlockEntity;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.battery.BatteryBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runtime gates for the LU cable and first-consumer prerequisite. */
@GameTestHolder(LuPrerequisiteGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LuPrerequisiteGameTests {
    public static final String NAMESPACE = "cruciblecraft_lu_prerequisite";
    private static final String TEMPLATE = "empty";

    private LuPrerequisiteGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void laserEngraverAcceptsLuThroughFiberCable(
            GameTestHelper helper) {
        BlockPos laserPos = new BlockPos(2, 1, 2);
        BlockPos cablePos = laserPos.above();
        BlockPos sourcePos = cablePos.above();
        helper.setBlock(
                laserPos,
                ModBlocks.LASER_ENGRAVER.get().defaultBlockState());
        helper.setBlock(
                cablePos,
                connectedFiber(Direction.DOWN, Direction.UP));
        helper.setBlock(
                sourcePos,
                ModBlocks.batteryBlocksById()
                        .get(id("red_energium_crystal_lv"))
                        .get()
                        .defaultBlockState());

        BatteryBlockEntity source = helper.getBlockEntity(sourcePos);
        LaserEngraverBlockEntity laser = helper.getBlockEntity(laserPos);
        helper.assertTrue(
                laser.handles(EnergyType.LU, Direction.UP)
                        && !laser.handles(EnergyType.ELECTRIC, Direction.UP),
                "Laser engraver energy identity is not LU-only");
        helper.assertTrue(
                source.insert(
                                EnergyType.LU,
                                32L,
                                1L,
                                Direction.DOWN,
                                false)
                        == 1L,
                "LV LU source rejected its nominal packet");
        long delivered = EnergyEmitter.emit(
                helper.getLevel(),
                helper.absolutePos(sourcePos),
                source,
                EnergyType.LU,
                Direction.DOWN);
        helper.assertTrue(
                delivered == 1L
                        && source.stored(EnergyType.LU) == 0L
                        && laser.stored(EnergyType.LU) == 32L,
                "LU packet did not reach the laser engraver");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fiberIsLosslessAcrossTwoSegments(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 2);
        BlockPos first = sourcePos.east();
        BlockPos second = first.east();
        BlockPos targetPos = second.east();
        BatteryBlockEntity source = placeCrystal(
                helper, sourcePos, "red_energium_crystal_lv");
        BatteryBlockEntity target = placeCrystal(
                helper, targetPos, "cyan_energium_crystal_lv");
        helper.setBlock(
                first,
                connectedFiber(Direction.WEST, Direction.EAST));
        helper.setBlock(
                second,
                connectedFiber(Direction.WEST, Direction.EAST));
        helper.assertTrue(
                source.insert(EnergyType.LU, 32L, 1L, Direction.EAST, false)
                        == 1L,
                "LU source rejected its nominal packet");
        long delivered = EnergyEmitter.emit(
                helper.getLevel(),
                helper.absolutePos(sourcePos),
                source,
                EnergyType.LU,
                Direction.EAST);
        helper.assertTrue(
                delivered == 1L
                        && source.stored(EnergyType.LU) == 0L
                        && target.stored(EnergyType.LU) == 32L,
                "LU fiber lost packets across two lossless segments");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void euCableDoesNotCarryLu(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 2);
        BlockPos cablePos = sourcePos.east();
        BlockPos targetPos = cablePos.east();
        BatteryBlockEntity source = placeCrystal(
                helper, sourcePos, "red_energium_crystal_ulv");
        BatteryBlockEntity target = placeCrystal(
                helper, targetPos, "cyan_energium_crystal_ulv");
        CableBlock copper = ModBlocks.electricalConductorBlock(
                "copper", MaterialPrefixes.CABLE).get();
        helper.assertTrue(
                !copper.supports(EnergyType.LU)
                        && copper.supports(EnergyType.ELECTRIC),
                "Copper EU cable must not advertise LU transport");
        helper.setBlock(
                cablePos,
                copper.defaultBlockState()
                        .setValue(
                                CableBlock.PROPERTY_BY_DIRECTION.get(
                                        Direction.WEST),
                                true)
                        .setValue(
                                CableBlock.PROPERTY_BY_DIRECTION.get(
                                        Direction.EAST),
                                true));
        helper.assertTrue(
                source.insert(EnergyType.LU, 8L, 1L, Direction.EAST, false)
                        == 1L,
                "LU source rejected its nominal packet");
        long delivered = EnergyEmitter.emit(
                helper.getLevel(),
                helper.absolutePos(sourcePos),
                source,
                EnergyType.LU,
                Direction.EAST);
        helper.assertTrue(
                delivered == 0L
                        && source.stored(EnergyType.LU) == 8L
                        && target.stored(EnergyType.LU) == 0L,
                "LU packet crossed an EU cable");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fiberRejectsEuAndDoesNotConnectToEuCable(
            GameTestHelper helper) {
        CableBlock fiber = ModBlocks.LU_FIBER_CABLE.get();
        CableBlock copper = ModBlocks.electricalConductorBlock(
                "copper", MaterialPrefixes.CABLE).get();
        helper.assertTrue(
                fiber.supports(EnergyType.LU)
                        && !fiber.supports(EnergyType.ELECTRIC),
                "LU fiber must be LU-only");
        helper.assertTrue(
                !copper.supports(EnergyType.LU),
                "EU cable still advertises LU");
        BlockPos fiberPos = new BlockPos(2, 1, 2);
        BlockPos copperPos = fiberPos.east();
        helper.setBlock(
                fiberPos,
                connectedFiber(Direction.EAST, Direction.WEST));
        helper.setBlock(
                copperPos,
                copper.defaultBlockState()
                        .setValue(
                                CableBlock.PROPERTY_BY_DIRECTION.get(
                                        Direction.WEST),
                                true));
        helper.assertTrue(
                !com.masson.cruciblecraft.content.block.Gt6StyleConnections
                        .canConnect(
                                helper.getLevel(),
                                helper.absolutePos(fiberPos),
                                Direction.EAST),
                "LU fiber connected to a copper EU cable");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sourceBackedFiberRecipeExistsAndEngraverStaysBlocked(
            GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("machines/lu_fiber_cable"))
                        .isPresent(),
                "Missing exact LU fiber recipe");
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("machines/laser_engraver"))
                        .isPresent(),
                "Missing source-backed laser engraver recipe");
        var silver = MaterialLookup.item("silver", MaterialPrefixes.PLATE)
                .orElseThrow();
        List<ItemStack> slots = List.of(
                new ItemStack(silver),
                new ItemStack(Items.GLASS),
                new ItemStack(Items.REDSTONE),
                new ItemStack(Items.DIAMOND),
                new ItemStack(ModItems.MATERIAL_WIRE_CUTTER.get()),
                new ItemStack(Items.DIAMOND),
                new ItemStack(Items.REDSTONE),
                new ItemStack(Items.GLASS),
                new ItemStack(silver));
        ItemStack assembled = craft(helper, slots);
        helper.assertTrue(
                assembled.is(ModItems.LU_FIBER_CABLE.get())
                        && assembled.getCount() == 1,
                "LU fiber 3x3 recipe did not assemble");
        helper.succeed();
    }

    private static net.minecraft.world.level.block.state.BlockState
            connectedFiber(Direction first, Direction second) {
        return ModBlocks.LU_FIBER_CABLE.get()
                .defaultBlockState()
                .setValue(CableBlock.PROPERTY_BY_DIRECTION.get(first), true)
                .setValue(CableBlock.PROPERTY_BY_DIRECTION.get(second), true);
    }

    private static BatteryBlockEntity placeCrystal(
            GameTestHelper helper, BlockPos pos, String path) {
        helper.setBlock(
                pos,
                ModBlocks.batteryBlocksById().get(id(path)).get()
                        .defaultBlockState());
        BatteryBlockEntity battery = helper.getBlockEntity(pos);
        helper.assertTrue(battery != null, "Missing battery block entity");
        return battery;
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(3, 3, slots);
        return helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(
                        input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
