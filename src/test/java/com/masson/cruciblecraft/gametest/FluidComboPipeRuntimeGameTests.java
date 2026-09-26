package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.item.PipeBlockItem;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 quadruple/nonuple fluid-pipe runtime. Run with
 * {@code -PgameTestGrid=logistics}.
 */
@GameTestHolder(FluidComboPipeRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class FluidComboPipeRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_logistics";
    private static final String TEMPLATE = "empty";

    private FluidComboPipeRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void comboPipeIndependentTanks(GameTestHelper helper) {
        FluidPipeBlockEntity quad = placePipe(
                helper,
                new BlockPos(2, 2, 2),
                fluidPipe("copper", MaterialPrefixes.QUADRUPLE_FLUID_PIPE));
        FluidPipeBlockEntity nonuple = placePipe(
                helper,
                new BlockPos(3, 2, 2),
                fluidPipe("copper", MaterialPrefixes.NONUPLE_FLUID_PIPE));
        helper.assertTrue(
                quad.tankCount() == 4
                        && quad.capacity() == 600
                        && quad.totalCapacity() == 2400,
                "copper quadruple is not four 600 mB tanks");
        helper.assertTrue(
                nonuple.tankCount() == 9
                        && nonuple.capacity() == 200
                        && nonuple.totalCapacity() == 1800,
                "copper nonuple is not nine 200 mB tanks");
        helper.assertTrue(
                quad.fillInternal(
                        new FluidStack(Fluids.WATER, 80),
                        IFluidHandler.FluidAction.EXECUTE) == 80
                        && quad.fillInternal(
                                new FluidStack(Fluids.LAVA, 40),
                                IFluidHandler.FluidAction.EXECUTE) == 40,
                "combo tanks did not accept independent fluids");
        helper.assertTrue(
                FluidStack.isSameFluidSameComponents(
                        quad.fluidInTank(0),
                        new FluidStack(Fluids.WATER, 80))
                        && quad.fluidInTank(0).getAmount() == 80
                        && FluidStack.isSameFluidSameComponents(
                                quad.fluidInTank(1),
                                new FluidStack(Fluids.LAVA, 40))
                        && quad.fluidInTank(1).getAmount() == 40,
                "matching-then-empty fill mixed independent tanks");
        CompoundTag tag = quad.getUpdateTag(
                helper.getLevel().registryAccess());
        helper.assertTrue(
                tag.contains("tanks")
                        && tag.getList("tanks", Tag.TAG_COMPOUND).size() == 4
                        && tag.getInt("tank_count") == 4,
                "quadruple NBT lost the tanks list");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void comboPipeIsNotHugeAlias(GameTestHelper helper) {
        var quad = PipeCatalog.require(
                "copper",
                MaterialPrefixes.QUADRUPLE_FLUID_PIPE,
                PipeCatalog.Kind.FLUID);
        var huge = PipeCatalog.require(
                "copper",
                MaterialPrefixes.HUGE_FLUID_PIPE,
                PipeCatalog.Kind.FLUID);
        helper.assertTrue(
                quad.tankCount() == 4
                        && huge.tankCount() == 1
                        && quad.fluid().capacityMb() == 600
                        && huge.fluid().capacityMb() == 2400
                        && "quadruple".equals(quad.textureKey())
                        && "16".equals(huge.textureKey()),
                "quadruple was aliased onto the huge single-tank host");
        helper.assertTrue(
                ModBlocks.hasPipeBlock(
                        "copper",
                        MaterialPrefixes.QUADRUPLE_FLUID_PIPE,
                        PipeCatalog.Kind.FLUID)
                        && ModBlocks.hasPipeBlock(
                                "copper",
                                MaterialPrefixes.NONUPLE_FLUID_PIPE,
                                PipeCatalog.Kind.FLUID),
                "combo copper pipes were not registered as FluidPipeBlocks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void comboPipeDummyFoldsOntoLiveHost(GameTestHelper helper) {
        helper.assertTrue(
                liveCombo("copper", MaterialPrefixes.QUADRUPLE_FLUID_PIPE)
                        && liveCombo("copper", MaterialPrefixes.NONUPLE_FLUID_PIPE)
                        && liveCombo("tin_alloy", MaterialPrefixes.QUADRUPLE_FLUID_PIPE)
                        && liveCombo("hslasteel", MaterialPrefixes.QUADRUPLE_FLUID_PIPE),
                "live combo BlockItems disappeared");
        helper.assertTrue(
                withdrawn("fluid_pipe_tile/quadruple_copper_fluid_pipe")
                        && withdrawn("fluid_pipe_tile/nonuple_copper_fluid_pipe")
                        && withdrawn("fluid_pipe_tile/quadruple_tin_alloy_fluid_pipe")
                        && withdrawn("fluid_pipe_tile/quadruple_hsla_steel_fluid_pipe"),
                "folded combo dummies are still registered");
        helper.succeed();
    }

    private static boolean liveCombo(
            String material,
            com.masson.cruciblecraft.api.material.MaterialPrefix form) {
        return ModBlocks.hasPipeBlock(material, form, PipeCatalog.Kind.FLUID)
                && ModBlocks.pipeBlock(material, form, PipeCatalog.Kind.FLUID)
                        .get() instanceof FluidPipeBlock
                && ModItems.hasMaterialItem(material, form)
                && ModItems.materialItem(material, form).get()
                        instanceof PipeBlockItem;
    }

    private static boolean withdrawn(String path) {
        return !BuiltInRegistries.ITEM.containsKey(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }

    private static FluidPipeBlockEntity placePipe(
            GameTestHelper helper, BlockPos pos, FluidPipeBlock block) {
        helper.setBlock(pos, pipeState(block, Direction.EAST));
        return helper.getBlockEntity(pos);
    }

    private static FluidPipeBlock fluidPipe(
            String material,
            com.masson.cruciblecraft.api.material.MaterialPrefix form) {
        return (FluidPipeBlock) ModBlocks.pipeBlock(
                material, form, PipeCatalog.Kind.FLUID).get();
    }

    private static BlockState pipeState(
            AbstractPipeBlock block, Direction... connections) {
        BlockState state = block.defaultBlockState();
        for (Direction direction : connections) {
            state = state.setValue(
                    AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                    true);
        }
        return state;
    }
}
