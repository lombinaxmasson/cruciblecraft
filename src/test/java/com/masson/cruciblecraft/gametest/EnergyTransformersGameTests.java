package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerCatalog;
import com.masson.cruciblecraft.energy.transformer.TransformerBlock;
import com.masson.cruciblecraft.energy.transformer.TransformerBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.verification.PlayerCompleteSmoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated transformers gate. Run with {@code -PwaveRecipes=runtime/transformers}.
 */
@GameTestHolder(EnergyTransformersGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class EnergyTransformersGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_transformers";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY = "energy/transformers";
    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static final List<String> SIGNOFF_ITEMS = List.of(
            "electric_transformer_ulv_lv",
            "electric_transformer_lv_mv",
            "electric_transformer_mv_hv",
            "electric_transformer_hv_ev",
            "electric_transformer_ev_iv",
            "electric_transformer_iv_luv",
            "electric_transformer_luv_zpm",
            "electric_transformer_zpm_uv",
            "electric_transformer_uv_puv1");

    private EnergyTransformersGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                EnergyTransformerCatalog.profiles().size() == 14,
                "Transformer catalog drifted from 14 long-voltage pairs");
        helper.assertTrue(
                ModItems.transformerItemsById()
                        .get(id("electric_transformer_ulv_lv"))
                        .get()
                        != null,
                "ULV-LV transformer item missing");
        PlayerCompleteSmoke.writeIfConfigured("gameTestServer", CAPABILITY);
        helper.assertTrue(
                PlayerCompleteSmoke.snapshot("gameTestServer", CAPABILITY)
                        .get("status")
                        .getAsString()
                        .equals("PASS"),
                "Player-complete registry snapshot failed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stepDownConvertsHvToLv(GameTestHelper helper) {
        TransformerBlockEntity transformer = place(helper);
        long accepted = transformer.insert(
                EnergyType.ELECTRIC, 32L, 1L, Direction.UP, false);
        helper.assertTrue(
                accepted == 1L
                        && transformer.stored(EnergyType.ELECTRIC) == 32L,
                "ULV transformer did not accept one HV packet");
        long extracted = transformer.extract(
                EnergyType.ELECTRIC, 8L, 4L, Direction.NORTH, false);
        helper.assertTrue(
                extracted == 4L
                        && transformer.stored(EnergyType.ELECTRIC) == 0L,
                "ULV transformer did not emit four LV packets");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reverseStepUpConvertsLvToHv(GameTestHelper helper) {
        TransformerBlockEntity transformer = place(helper);
        transformer.toggleReversed();
        helper.assertTrue(
                transformer.reversed(),
                "Wrench reverse did not stick");
        long accepted = transformer.insert(
                EnergyType.ELECTRIC, 8L, 4L, Direction.NORTH, false);
        helper.assertTrue(
                accepted == 4L
                        && transformer.stored(EnergyType.ELECTRIC) == 32L,
                "Reversed ULV transformer did not accept four LV packets");
        long extracted = transformer.extract(
                EnergyType.ELECTRIC, 32L, 1L, Direction.UP, false);
        helper.assertTrue(
                extracted == 1L
                        && transformer.stored(EnergyType.ELECTRIC) == 0L,
                "Reversed ULV transformer did not emit one HV packet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void onlyMonkeyWrenchReversesTransformer(
            GameTestHelper helper) {
        place(helper);
        Player player = helper.makeMockPlayer(
                net.minecraft.world.level.GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(helper.absolutePos(POS)),
                Direction.UP,
                helper.absolutePos(POS),
                false);
        ItemStack wrench = new ItemStack(ModItems.MATERIAL_WRENCH.get());
        helper.getBlockState(POS).useItemOn(
                wrench,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                hit);
        TransformerBlockEntity transformer =
                helper.getBlockEntity(POS);
        helper.assertTrue(
                !transformer.reversed(),
                "Regular wrench must not reverse transformer");
        ItemStack monkeyWrench =
                new ItemStack(ModItems.MATERIAL_MONKEY_WRENCH.get());
        helper.getBlockState(POS).useItemOn(
                monkeyWrench,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                hit);
        helper.assertTrue(
                transformer.reversed(),
                "Monkey wrench must reverse transformer");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reloadPreservesFacingAndReverse(GameTestHelper helper) {
        var block = ModBlocks.transformerBlocksById()
                .get(id("electric_transformer_ulv_lv"))
                .get();
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(TransformerBlock.FACING, Direction.NORTH));
        TransformerBlockEntity transformer = helper.getBlockEntity(POS);
        helper.assertTrue(transformer != null, "Missing transformer block entity");
        transformer.toggleReversed();
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = transformer.saveWithoutMetadata(registries);
        helper.setBlock(POS, block.defaultBlockState());
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(TransformerBlock.FACING, Direction.NORTH));
        TransformerBlockEntity reloaded = helper.getBlockEntity(POS);
        helper.assertTrue(reloaded != null, "Missing reloaded transformer");
        reloaded.loadWithComponents(saved, registries);
        helper.assertTrue(
                helper.getBlockState(POS).getValue(TransformerBlock.FACING)
                        == Direction.NORTH,
                "Facing did not survive reload");
        helper.assertTrue(
                reloaded.reversed(),
                "Reverse flag did not survive BlockEntity reload");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void rejectsNonEu(GameTestHelper helper) {
        TransformerBlockEntity transformer = place(helper);
        helper.assertTrue(
                transformer.insert(
                        EnergyType.LU, 32L, 1L, Direction.UP, false)
                        == 0L
                        && transformer.stored(EnergyType.ELECTRIC) == 0L,
                "Transformer accepted LU");
        helper.assertTrue(
                transformer.insert(
                        EnergyType.ELECTRIC, 32L, 1L, Direction.NORTH, false)
                        == 0L,
                "Forward transformer accepted HV on an output face");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void representativeRecipesAreSurvivalCraftable(
            GameTestHelper helper) {
        for (String path : SIGNOFF_ITEMS) {
            helper.assertTrue(
                    helper.getLevel().getRecipeManager().byKey(id(path)).isPresent(),
                    "Missing survival recipe " + path);
        }
        var wire = MaterialLookup.stack("copper", MaterialPrefixes.WIRE);
        var iron = MaterialLookup.stack("iron", MaterialPrefixes.DOUBLE_PLATE);
        var quad = MaterialLookup.stack(
                "copper", MaterialPrefixes.QUADRUPLE_WIRE);
        var casing = MaterialLookup.stack(
                "tin_alloy", MaterialPrefixes.MACHINE_CASING);
        List<ItemStack> slots = List.of(
                wire,
                iron,
                wire.copy(),
                quad,
                casing,
                ItemStack.EMPTY,
                wire.copy(),
                iron.copy(),
                wire.copy());
        ItemStack assembled = craft(helper, 3, 3, slots);
        helper.assertTrue(
                assembled.is(
                        ModItems.transformerItemsById()
                                .get(id("electric_transformer_ulv_lv"))
                                .get())
                        && assembled.getCount() == 1,
                "ULV-LV transformer 3x3 recipe missing");
        helper.succeed();
    }

    private static TransformerBlockEntity place(GameTestHelper helper) {
        helper.setBlock(
                POS,
                ModBlocks.transformerBlocksById()
                        .get(id("electric_transformer_ulv_lv"))
                        .get()
                        .defaultBlockState());
        TransformerBlockEntity transformer = helper.getBlockEntity(POS);
        helper.assertTrue(transformer != null, "Missing transformer block entity");
        return transformer;
    }

    private static ItemStack craft(
            GameTestHelper helper, int width, int height, List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(width, height, slots);
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
