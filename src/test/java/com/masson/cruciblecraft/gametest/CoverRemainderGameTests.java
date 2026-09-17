package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverVisuals;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated runtime gate for the GT6 machine-cover remainder.
 *  Run with {@code -PwaveRecipes=runtime/cover-remainder}. */
@GameTestHolder(CoverRemainderGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CoverRemainderGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_cover_remainder";
    private static final String TEMPLATE = "empty";
    private static final BlockPos MACHINE_POS = new BlockPos(2, 2, 2);
    private static final Direction COVER_SIDE = Direction.NORTH;

    private CoverRemainderGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        CoverBehaviorRegistry.validateDefinitions();
        helper.assertTrue(
                CoverDefinitionCatalog.definitions().stream()
                        .filter(definition -> MachineCoverKinds
                                .BEHAVIOR_PATHS
                                .contains(definition.behaviorId().getPath()))
                        .count() == MachineCoverKinds.DEFINITION_COUNT,
                "Machine cover definition denominator drifted");
        helper.assertTrue(
                ModItems.machineCovers().size() == MachineCoverKinds.ITEM_COUNT,
                "Machine cover item registration drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void blankCoverIsSurvivalCraftable(GameTestHelper helper) {
        ItemStack aluminiumPlate = MaterialLookup.stack(
                "aluminium", MaterialPrefixes.PLATE);
        ItemStack aluminiumScrew = MaterialLookup.stack(
                "aluminium", MaterialPrefixes.SCREW);
        ItemStack assembled = craft(
                helper,
                3,
                2,
                List.of(
                        aluminiumScrew,
                        new ItemStack(ModItems.SMITHING_HAMMER.get()),
                        ItemStack.EMPTY,
                        aluminiumPlate,
                        new ItemStack(ModItems.MATERIAL_SCREWDRIVER.get()),
                        ItemStack.EMPTY));
        helper.assertTrue(
                assembled.is(ModItems.machineCover(
                                MachineCoverKinds.itemPath("cover_blank"))
                        .get())
                        && assembled.getCount() == 1,
                "GT6 blank cover recipe did not assemble");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void blankCoverRequiresCraftingTable(GameTestHelper helper) {
        var blank = ModItems.machineCover(
                MachineCoverKinds.itemPath("cover_blank")).get();
        var recipe = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(holder -> holder.value())
                .filter(value -> value.getResultItem(
                        helper.getLevel().registryAccess()).is(blank))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Blank cover crafting recipe is missing"));
        helper.assertFalse(
                recipe.canCraftInDimensions(2, 2),
                "Blank cover recipe advertised a player 2x2 grid");
        helper.assertTrue(
                recipe.canCraftInDimensions(3, 3),
                "Blank cover recipe refused a crafting table");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void controllerStopsProcessingWhenOff(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.OVEN.get(),
                        ModProcessingMachines.OVEN);
        helper.setBlock(
                MACHINE_POS.relative(COVER_SIDE),
                Blocks.REDSTONE_BLOCK);
        helper.assertTrue(
                machine.setCover(
                        COVER_SIDE,
                        PipeCover.of("cruciblecraft:controller_redstone")),
                "Could not install controller cover");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        machine.coverEnabled(),
                        "Redstone controller did not enable the machine"))
                .thenExecute(() -> helper.setBlock(
                        MACHINE_POS.relative(COVER_SIDE),
                        Blocks.AIR))
                .thenIdle(2)
                .thenExecute(() -> helper.assertFalse(
                        machine.coverEnabled(),
                        "Redstone controller did not stop the machine"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void detectorEmitsWhenPossible(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.OVEN.get(),
                        ModProcessingMachines.OVEN);
        machine.inventory().setStackInSlot(
                machine.spec().items().inputs().getFirst(),
                new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(
                machine.setCover(
                        COVER_SIDE,
                        PipeCover.of(
                                "cruciblecraft:detector_running_possible")),
                "Could not install detector cover");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        MachineCoverBehaviors.weakRedstone(
                                machine, COVER_SIDE) == 15,
                        "Possible detector did not emit 15"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void torchRefusesMachineHost(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.OVEN.get(),
                        ModProcessingMachines.OVEN);
        helper.assertFalse(
                MachineCoverBehaviors.canPlace(
                        machine,
                        COVER_SIDE,
                        PipeCover.of("cruciblecraft:redstone_torch")),
                "Redstone torch cover attached to an unsupported host");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void autoSwitchRequiresPossibleAndCanTick(
            GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.OVEN.get(),
                        ModProcessingMachines.OVEN);
        PipeCover auto = PipeCover.of("cruciblecraft:controller_auto");
        helper.assertTrue(machine.canTick(), "Server machine could not tick");
        helper.assertFalse(
                MachineCoverBehaviors.canPlace(machine, COVER_SIDE, auto),
                "Auto switch installed without a usable recipe");
        machine.inventory().setStackInSlot(
                machine.spec().items().inputs().getFirst(),
                new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(
                MachineCoverBehaviors.canPlace(machine, COVER_SIDE, auto),
                "Auto switch refused a machine that could run");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void selectorUsesFakeCircuit(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity assembler =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.ASSEMBLER.get(),
                        ModProcessingMachines.ASSEMBLER);
        int input = assembler.spec().items().inputs().getFirst();
        assembler.inventory().setStackInSlot(
                input, new ItemStack(Items.OAK_PLANKS));
        OrdinaryClosureHostGameTests.fillEnergy(helper, assembler);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        assembler.duration() == 0,
                        "Oak planks selected a circuit recipe without a selector: "
                                + assembler.pausedReason()))
                .thenExecute(() -> {
                    helper.assertTrue(
                            assembler.setCover(
                                    COVER_SIDE,
                                    PipeCover.of("cruciblecraft:selector_tag")
                                            .withDisplay(0, 0)),
                            "Could not install selector tag");
                    OrdinaryClosureHostGameTests.fillEnergy(helper, assembler);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            assembler.duration() > 0,
                            "Selector did not select the circuit-1 recipe: "
                                    + assembler.pausedReason());
                    assembler.runtime().processor().setProgress(
                            assembler.duration() - 1);
                    OrdinaryClosureHostGameTests.fillEnergy(helper, assembler);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            assembler.spec().items().outputs().stream()
                                    .map(assembler.inventory()::getStackInSlot)
                                    .anyMatch(stack -> stack.is(Items.OAK_BUTTON)),
                            "Selector fake circuit did not produce oak_button: "
                                    + assembler.pausedReason());
                    helper.assertTrue(
                            assembler.spec().items().inputs().stream()
                                    .map(assembler.inventory()::getStackInSlot)
                                    .noneMatch(stack -> stack.is(
                                            ModItems.PROGRAMMED_CIRCUIT.get())),
                            "Fake circuit was written into inventory");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void ventFillsCollectableAir(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity mixer =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.MIXER.get(),
                        ModProcessingMachines.MIXER);
        helper.assertTrue(
                mixer.setCover(
                        COVER_SIDE,
                        PipeCover.of("cruciblecraft:vent")),
                "Could not install vent");
        BlockPos neighbor = MACHINE_POS.relative(COVER_SIDE);
        helper.setBlock(neighbor, Blocks.COBBLESTONE);
        int wait = ventWait(helper);
        helper.startSequence()
                .thenIdle(wait)
                .thenExecute(() -> helper.assertTrue(
                        mixer.spec().fluids().inputs().stream().allMatch(
                                tank -> mixer.tanks().get(tank.index())
                                        .getFluid().isEmpty()),
                        "Vent filled through a solid neighbor"))
                .thenExecute(() -> helper.setBlock(neighbor, Blocks.AIR))
                .thenIdle(360)
                .thenExecute(() -> {
                    FluidStack filled = mixer.spec().fluids().inputs().stream()
                            .map(tank -> mixer.tanks().get(tank.index()).getFluid())
                            .filter(stack -> !stack.isEmpty())
                            .findFirst()
                            .orElse(FluidStack.EMPTY);
                    helper.assertTrue(
                            !filled.isEmpty()
                                    && filled.is(ModFluids.materialFluid("air")
                                            .orElseThrow())
                                    && filled.getAmount() > 0,
                            "Vent did not fill collectable overworld air");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void displayVisualUsesGt6Layout(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.OVEN.get(),
                        ModProcessingMachines.OVEN);
        machine.inventory().setStackInSlot(
                machine.spec().items().inputs().getFirst(),
                new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(
                machine.setCover(
                        COVER_SIDE,
                        PipeCover.of("cruciblecraft:controller_display")),
                "Could not install status display");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    PipeCover cover = machine.covers()
                            .get(COVER_SIDE)
                            .orElseThrow();
                    int visual = cover.config().visual();
                    helper.assertTrue(
                            cover.config().redstone() == 0,
                            "Status bits were packed into redstone");
                    helper.assertTrue(
                            MachineCoverVisuals.displayLightOn(visual, 0)
                                    && MachineCoverVisuals.displayLightPresent(
                                            visual, 0)
                                    && MachineCoverVisuals.displayLightPresent(
                                            visual, 3)
                                    && MachineCoverVisuals.displayStyle(visual)
                                            == 0,
                            "Display visual was not the GT6 possible/on layout: "
                                    + visual);
                    helper.assertTrue(
                            MachineCoverBehaviors.onTool(
                                    machine, COVER_SIDE, ToolAction.CHISEL),
                            "Chisel did not cycle the display skin");
                    int next = machine.covers()
                            .get(COVER_SIDE)
                            .orElseThrow()
                            .config()
                            .visual();
                    helper.assertTrue(
                            MachineCoverVisuals.displayStyle(next) == 1
                                    && MachineCoverVisuals.displaySkin(next)
                                            .equals("top"),
                            "Chisel did not move the GT6 visual style bit");
                })
                .thenSucceed();
    }

    private static int ventWait(GameTestHelper helper) {
        int target = 30 + 60 * COVER_SIDE.ordinal();
        int phase = (int) Math.floorMod(
                helper.getLevel().getGameTime(), 360L);
        int wait = Math.floorMod(target - phase, 360);
        return wait < 2 ? wait + 360 : wait + 2;
    }

    private static ItemStack craft(
            GameTestHelper helper,
            int width,
            int height,
            List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(width, height, slots);
        return helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(
                        RecipeType.CRAFTING,
                        input,
                        helper.getLevel())
                .map(holder -> holder.value().assemble(
                        input,
                        helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
}
