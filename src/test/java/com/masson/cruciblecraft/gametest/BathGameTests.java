package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.item.PrefixMaterialItem;
import com.masson.cruciblecraft.content.mte.BathingPotRuntime;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.recipe.crafting.ShapedCatalystRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated Bath host + bathing-pot gate. Run with
 * {@code -PgameTestGrid=machines}.
 */
@GameTestHolder(BathGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class BathGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private BathGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bathHostAcquisitionIsSourceExact(GameTestHelper helper) {
        RecipeHolder<?> holder = helper.getLevel().getRecipeManager()
                .byKey(id("machines/bath"))
                .orElse(null);
        helper.assertTrue(
                holder != null && holder.value() instanceof ShapedCatalystRecipe,
                "Bath 22002 acquisition recipe missing");
        ShapedCatalystRecipe recipe = (ShapedCatalystRecipe) holder.value();
        helper.assertTrue(
                recipe.pattern().equals(List.of("CwC", "PMP", "PPP")),
                "Bath grid is not GT6 CwC/PMP/PPP");
        ItemStack plate = material("stainless_steel", MaterialPrefixes.PLATE);
        ItemStack casing = material("stainless_steel", MaterialPrefixes.MACHINE_CASING);
        ItemStack small = material(
                "stainless_steel",
                MaterialPrefixCatalog.require("small_casing"));
        helper.assertTrue(
                recipe.ingredients().get("P").test(plate)
                        && recipe.ingredients().get("M").test(casing)
                        && recipe.ingredients().get("C").test(small)
                        && recipe.catalysts().get("w").test(
                                new ItemStack(ModItems.MATERIAL_WRENCH.get()))
                        && recipe.ingredients().values().stream().noneMatch(
                                ingredient -> ingredient.test(
                                        new ItemStack(Items.COPPER_INGOT)))
                        && recipe.ingredients().values().stream().noneMatch(
                                ingredient -> ingredient.test(
                                        new ItemStack(Items.FURNACE))),
                "Bath 22002 is not stainless casing/small_casing/plate + wrench");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bathMachineUsesCubeNotPotVoxel(GameTestHelper helper) {
        String model = resource("/assets/cruciblecraft/models/block/bath.json");
        helper.assertTrue(
                model.contains("machine_cube_2_layer")
                        && model.contains("block/machine/bath/colored/front")
                        && !model.contains("insides"),
                "Bath machine model is still the bathing-pot voxel");
        helper.setBlock(
                POS,
                ModBlocks.BATH.get().defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, Direction.NORTH));
        helper.assertTrue(
                helper.getBlockState(POS).getBlock() == ModBlocks.BATH.get(),
                "Bath machine did not place");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bathingPotClickProcessesWithoutGui(GameTestHelper helper) {
        var block = ModBlocks.mteInPlaceBlocksById()
                .get(id("misc_tool/bathing_pot"))
                .get();
        helper.setBlock(
                POS,
                block.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.NORTH));
        MteInPlaceBlockEntity host = helper.getBlockEntity(POS);
        helper.assertTrue(
                host != null && host.bathingPot() != null,
                "Stainless bathing pot has no click-to-process runtime");
        GTRecipe recipe = ModRecipeMaps.BATH.entries().stream()
                .map(RecipeMap.Entry::recipe)
                .filter(candidate -> candidate.itemInputs().size() == 1
                        && candidate.fluidInputs().size() == 1
                        && candidate.itemInputs().getFirst().getItems().length > 0
                        && (!candidate.fluidOutputs().isEmpty()
                                || !candidate.itemOutputs().isEmpty()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No Bath recipe with one item and one fluid input"));
        ItemStack sample = recipe.itemInputs().getFirst().getItems()[0].copy();
        sample.setCount(Math.max(1, recipe.itemInputCounts().getFirst()));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, sample);
        helper.getBlockState(POS).useItemOn(
                sample,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                hit(helper, POS));
        helper.assertTrue(
                player.containerMenu == player.inventoryMenu,
                "Bathing pot opened a GUI");
        helper.assertTrue(
                !host.bathingPot().items().getStackInSlot(0).isEmpty(),
                "Click did not insert into the bathing pot");
        host.bathingPot().tank(0).setFluid(recipe.fluidInputs().getFirst().copy());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            host.bathingPot().processor().duration() > 0,
                            "Bathing pot did not select a Bath recipe");
                    host.bathingPot().processor().setProgress(
                            host.bathingPot().processor().duration() - 1);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    boolean produced = false;
                    for (int slot : BathingPotRuntime.OUTPUT_SLOTS) {
                        if (!host.bathingPot().items().getStackInSlot(slot).isEmpty()) {
                            produced = true;
                            break;
                        }
                    }
                    for (int tank = 1; tank < BathingPotRuntime.TANK_COUNT; tank++) {
                        if (!host.bathingPot().tank(tank).isEmpty()) {
                            produced = true;
                            break;
                        }
                    }
                    helper.assertTrue(produced, "Bathing pot did not produce output");
                    player.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bathingPotManifestResolvesLocalGt6(GameTestHelper helper) {
        String manifest = resource(
                "/assets/cruciblecraft/gt6_bath_art_manifest.json");
        helper.assertTrue(
                manifest.contains("basicmachines/bath")
                        && manifest.contains("tools/bathing_pot")
                        && manifest.contains("gt6_referencable_port_code/gregtech6_w")
                        && !manifest.contains("minecraft:block")
                        && !manifest.contains("multiblock_casing"),
                "Bath art manifest drifted from local GT6");
        helper.assertTrue(
                classpathExists(
                        "/assets/cruciblecraft/textures/block/machine/bath/colored/front.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6_import/"
                                        + "mte/bathing_pot/colored/insides.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6_import/"
                                        + "mte/bathing_pot_wood/colored/insides.png"),
                "GT6 bath cube or bathing-pot insides were not imported");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stainlessBathingPotTableHasAcquisitionRecipe(
            GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("misc_tool/bathing_pot_table"))
                        .isPresent(),
                "Stainless bathing pot table recipe missing");
        RecipeHolder<?> holder = helper.getLevel().getRecipeManager()
                .byKey(id("misc_tool/bathing_pot_table"))
                .orElseThrow();
        helper.assertTrue(
                holder.value() instanceof ShapedCatalystRecipe recipe
                        && recipe.ingredients().get("M").test(new ItemStack(
                                ModItems.mteInPlaceItemsById()
                                        .get(id("misc_tool/bathing_pot"))
                                        .get()))
                        && recipe.ingredients().get("S").test(
                                new ItemStack(Items.BRICK_SLAB)),
                "Table recipe is not pot + brick_slab");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void woodenBathingPotStaysBlockedOnGlue(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("misc_tool/wooden_bathing_pot"))
                        .isEmpty()
                        && helper.getLevel().getRecipeManager()
                                .byKey(id("misc_tool/wooden_bathing_pot_table"))
                                .isEmpty(),
                "Wooden bathing pot/table craft must stay blocked on OD.itemGlue");
        helper.succeed();
    }

    private static ItemStack material(
            String materialId,
            com.masson.cruciblecraft.api.material.MaterialPrefix form) {
        ItemStack stack = new ItemStack(
                ModItems.materialItem(materialId, form).get());
        if (stack.getItem() instanceof PrefixMaterialItem) {
            stack.set(ModComponents.PREFIX_MATERIAL, materialId);
        }
        return stack;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(
                new Vec3(
                        absolute.getX() + 0.5,
                        absolute.getY() + 0.5,
                        absolute.getZ() + 0.5),
                Direction.NORTH,
                absolute,
                false);
    }

    private static boolean classpathExists(String path) {
        return BathGameTests.class.getResource(path) != null;
    }

    private static String resource(String path) {
        try (InputStream stream = BathGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing classpath resource " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(path, failure);
        }
    }
}
