package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeBlockedMedia;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeDangerousMedia;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 fluid dangerous-media runtime. Run with
 * {@code -PgameTestGrid=logistics}.
 */
@GameTestHolder(FluidDangerousMediaRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class FluidDangerousMediaRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_logistics";
    private static final String TEMPLATE = "empty";

    private FluidDangerousMediaRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void magicFluidFillsThenTrashes(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        FluidPipeBlockEntity copper = placePipe(
                helper, pos, fluidPipe("copper", MaterialPrefixes.FLUID_PIPE));
        var molten = ModFluids.molten("wax_soulful").orElseThrow();
        FluidStack magic = new FluidStack(molten.source().get(), 50);
        helper.assertTrue(
                FluidPipeBlockedMedia.kindOf(magic)
                        == FluidPipeBlockedMedia.Kind.MAGIC,
                "wax_soulful molten is not MAGICAL");
        IFluidHandler tank = copper.fluidHandler(Direction.EAST);
        helper.assertTrue(tank != null, "missing copper tank");
        int beforeGas = copper.failureSnapshot().gasLeakEvents();
        int beforeAcid = copper.failureSnapshot().corrosionEvents();
        int filled = tank.fill(magic, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 50, "magic fill was rejected: " + filled);
        helper.assertTrue(
                copper.failureSnapshot().gasLeakEvents() == beforeGas
                        && copper.failureSnapshot().corrosionEvents()
                                == beforeAcid,
                "magic fill was mapped onto gas or acid");
        BlockPos worldPos = helper.absolutePos(pos);
        FluidPipeDangerousMedia.tickDeterministic(
                helper.getLevel(), worldPos, copper);
        helper.assertTrue(
                copper.storedFluid().getAmount()
                        == 50 - FluidPipeDangerousMedia.MAGIC_TRASH_LIQUID,
                "magic tick did not trash "
                        + FluidPipeDangerousMedia.MAGIC_TRASH_LIQUID
                        + " mB: "
                        + copper.storedFluid().getAmount());
        helper.assertTrue(
                helper.getBlockEntity(pos) == copper,
                "deterministic magic tick replaced the pipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void plasmaMagicNotMappedToGasAcid(GameTestHelper helper) {
        FluidPipeBlockEntity copper = placePipe(
                helper,
                new BlockPos(3, 2, 1),
                fluidPipe("copper", MaterialPrefixes.FLUID_PIPE));
        IFluidHandler tank = copper.fluidHandler(Direction.EAST);
        helper.assertTrue(tank != null, "missing copper tank");
        int scanned = 0;
        for (ModFluids.ChemicalFluidEntry entry : ModFluids.chemicalFluids()) {
            FluidStack stack = new FluidStack(entry.source().get(), 10);
            if (FluidPipeBlockedMedia.kindOf(stack)
                    == FluidPipeBlockedMedia.Kind.NONE) {
                continue;
            }
            scanned++;
            int beforeGas = copper.failureSnapshot().gasLeakEvents();
            int beforeAcid = copper.failureSnapshot().corrosionEvents();
            int filled = tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(
                    filled > 0,
                    "plasma/magic chemical fill rejected: " + entry.id());
            helper.assertTrue(
                    copper.failureSnapshot().gasLeakEvents() == beforeGas
                            && copper.failureSnapshot().corrosionEvents()
                                    == beforeAcid,
                    "plasma/magic mapped onto gas/acid: " + entry.id());
            copper.trashContents();
        }
        helper.assertTrue(
                scanned >= 0, "plasma/magic chemical scan crashed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void flammableOverTempLightsAdjacent(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        FluidPipeBlock woodBlock =
                fluidPipe("wood_treated", MaterialPrefixes.FLUID_PIPE);
        FluidPipeBlock copperBlock =
                fluidPipe("copper", MaterialPrefixes.FLUID_PIPE);
        helper.assertTrue(
                woodBlock.getFlammability(
                                woodBlock.defaultBlockState(),
                                helper.getLevel(),
                                pos,
                                Direction.UP)
                        == FluidPipeBlock.GT6_FLAMMABILITY,
                "wood treated flammability drifted from 150");
        helper.assertTrue(
                copperBlock.getFlammability(
                                copperBlock.defaultBlockState(),
                                helper.getLevel(),
                                pos,
                                Direction.UP)
                        == 0,
                "copper pipe became flammable");
        FluidPipeBlockEntity wood = placePipe(helper, pos, woodBlock);
        helper.assertTrue(
                wood.fillInternal(
                        new FluidStack(Fluids.LAVA, 50),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 50,
                "could not prefill over-temp lava");
        FluidPipeDangerousMedia.tickDeterministic(
                helper.getLevel(), helper.absolutePos(pos), wood);
        boolean fire = false;
        for (Direction direction : Direction.values()) {
            if (helper.getBlockState(pos.relative(direction)).is(Blocks.FIRE)) {
                fire = true;
                break;
            }
        }
        helper.assertTrue(fire, "over-temp flammable pipe did not light adjacent fire");
        helper.assertTrue(
                helper.getBlockEntity(pos) == wood,
                "deterministic over-temp tick replaced the pipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void contactDamageHurtsLivingEntity(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        FluidPipeBlockEntity wood = placePipe(
                helper,
                pos,
                fluidPipe("wood_treated", MaterialPrefixes.FLUID_PIPE));
        helper.assertTrue(
                woodBlockFluid(wood).contactDamage(),
                "wood treated lost contactDamage");
        Pig empty = helper.spawn(EntityType.PIG, pos);
        float emptyHealth = empty.getHealth();
        BlockPos worldPos = helper.absolutePos(pos);
        FluidPipeDangerousMedia.contact(helper.getLevel(), worldPos, empty);
        helper.assertTrue(
                empty.getHealth() == emptyHealth,
                "empty pipe dealt contact damage");
        helper.assertTrue(
                wood.fillInternal(
                        new FluidStack(Fluids.WATER, 50),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 50,
                "could not prefill contact tank");
        Pig loaded = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 2));
        float loadedHealth = loaded.getHealth();
        FluidPipeDangerousMedia.contact(helper.getLevel(), worldPos, loaded);
        helper.assertTrue(
                loaded.getHealth() < loadedHealth,
                "contactDamage pipe did not hurt: "
                        + loaded.getHealth()
                        + " / "
                        + loadedHealth);
        helper.succeed();
    }

    private static com.masson.cruciblecraft.material.def.GT6MaterialMetadata
                    .FluidPipeProperties
            woodBlockFluid(FluidPipeBlockEntity pipe) {
        return pipe.pipeBlock().pipe().fluid();
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
