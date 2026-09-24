package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.item.CatalogNamedItem;
import com.masson.cruciblecraft.content.item.CellItem;
import com.masson.cruciblecraft.content.mte.FluidAttachmentTransfer;
import com.masson.cruciblecraft.content.mte.MteFaucetProfile;
import com.masson.cruciblecraft.content.mte.MteFluidAttachmentProfile;
import com.masson.cruciblecraft.content.mte.MteInPlaceCatalog;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverItems;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated fluid-attachment runtime. Run with
 * {@code -PwaveRecipes=content/gt6-mte-fluid-attachments-runtime}.
 */
@GameTestHolder(MteFluidAttachmentsRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteFluidAttachmentsRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_fluid_attachments_runtime";
    private static final String TEMPLATE = "empty";

    private MteFluidAttachmentsRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneFaucetIsLiveBlockNotCover(GameTestHelper helper) {
        Item item = item("fluid_attachment/crucible_faucet_stone");
        helper.assertTrue(
                item instanceof CatalogNamedBlockItem
                        && !(item instanceof CatalogNamedItem),
                "stone faucet is still a dummy CatalogNamedItem");
        helper.assertTrue(
                ModBlocks.mteInPlaceBlocksById().get(id(
                        "fluid_attachment/crucible_faucet_stone"))
                        .get() instanceof MteInPlaceBlock,
                "stone faucet lost its in-place block");
        helper.assertTrue(
                ((MteInPlaceBlock) ((CatalogNamedBlockItem) item).getBlock())
                        .spec()
                        .kind()
                        == MteInPlaceKind.FAUCET,
                "stone faucet kind drifted");
        helper.assertTrue(
                PipeCoverItems.stackFor(null).isEmpty(),
                "cover helper must still treat null as empty");
        helper.assertTrue(
                BuiltInRegistries.ITEM.getKey(item).getPath().equals(
                        "fluid_attachment/crucible_faucet_stone"),
                "stone faucet left its dummy modern id");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void stoneFaucetPoursIntoTankBelow(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(2, 3, 2);
        BlockPos faucetPos = new BlockPos(2, 3, 3);
        BlockPos destPos = new BlockPos(2, 2, 3);
        FluidPipeBlockEntity source = placePipe(helper, sourcePos);
        FluidPipeBlockEntity dest = placePipe(helper, destPos);
        MteInPlaceBlock faucetBlock = ModBlocks.mteInPlaceBlocksById()
                .get(id("fluid_attachment/crucible_faucet_stone"))
                .get();
        helper.setBlock(
                faucetPos,
                faucetBlock.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.NORTH));
        helper.assertTrue(
                helper.getBlockEntity(faucetPos) instanceof MteInPlaceBlockEntity,
                "stone faucet did not place a block entity");
        helper.assertTrue(
                source.fillInternal(
                        new FluidStack(Fluids.WATER, 200),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 200,
                "source pipe rejected water");
        MteInPlaceBlockEntity faucet = helper.getBlockEntity(faucetPos);
        faucet.transferOnce();
        helper.assertTrue(
                dest.storedFluid().isEmpty(),
                "faucet piped water; GT6 only pours molten material from a crucible");
        helper.assertTrue(
                source.storedFluid().getAmount() == 200,
                "faucet consumed pipe fluid without a crucible");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stainlessTapIsLiveAttachment(GameTestHelper helper) {
        Item item = item("fluid_attachment/stainless_tap");
        helper.assertTrue(
                item instanceof CatalogNamedBlockItem,
                "stainless tap is not a BlockItem");
        helper.assertTrue(
                ((MteInPlaceBlock) ((CatalogNamedBlockItem) item).getBlock())
                        .spec()
                        .kind()
                        == MteInPlaceKind.TAP,
                "stainless tap kind drifted");
        helper.assertTrue(
                ModItems.smelterMteItemsById().get(id(
                        "fluid_attachment/stainless_tap"))
                        == null,
                "stainless tap dummy CatalogNamedItem is still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void allFluidAttachmentTiersAreLive(GameTestHelper helper) {
        long attachments = MteInPlaceCatalog.specs().stream()
                .filter(spec -> spec.kind().attachment())
                .count();
        helper.assertTrue(attachments == 47, "fluid attachment count drifted: " + attachments);
        helper.assertTrue(
                MteFaucetProfile.all().size() == 23,
                "faucet profile count drifted");
        for (MteFaucetProfile profile : MteFaucetProfile.all()) {
            helper.assertTrue(
                    ModItems.mteInPlaceItemsById().containsKey(profile.id()),
                    "faucet has no live BlockItem: " + profile.id());
            helper.assertTrue(
                    MteInPlaceCatalog.require(profile.id()).kind()
                            == MteInPlaceKind.FAUCET,
                    "faucet profile kind drifted: " + profile.id());
        }
        helper.assertTrue(
                MteFluidAttachmentProfile.all().size() == 24,
                "phase/proof profile count drifted");
        for (MteFluidAttachmentProfile profile
                : MteFluidAttachmentProfile.all()) {
            helper.assertTrue(
                    ModItems.mteInPlaceItemsById().containsKey(profile.id()),
                    "profile has no live BlockItem: " + profile.id());
            helper.assertTrue(
                    MteFluidAttachmentProfile.require(
                            MteInPlaceCatalog.require(profile.id()))
                            == profile,
                    "profile lookup drifted: " + profile.id());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidAttachmentPhaseFilterMatchesGt6(
            GameTestHelper helper) {
        var tap = MteFluidAttachmentProfile.require(
                MteInPlaceCatalog.require(id("fluid_attachment/stainless_tap")));
        var nozzle = MteFluidAttachmentProfile.require(
                MteInPlaceCatalog.require(id("tungsten/nozzle")));
        FluidStack water = new FluidStack(Fluids.WATER, 1000);
        FluidStack steam = new FluidStack(ModFluids.STEAM_SOURCE.get(), 1000);
        helper.assertTrue(
                FluidAttachmentTransfer.accepted(water, tap),
                "Tap rejected a liquid");
        helper.assertFalse(
                FluidAttachmentTransfer.accepted(steam, tap),
                "Tap accepted a gas");
        helper.assertTrue(
                FluidAttachmentTransfer.accepted(steam, nozzle),
                "Nozzle rejected a gas");
        helper.assertFalse(
                FluidAttachmentTransfer.accepted(water, nozzle),
                "Nozzle accepted a liquid");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void funnelFillsHostFromFluidCell(GameTestHelper helper) {
        BlockPos funnelPos = new BlockPos(3, 2, 2);
        BlockPos hostPos = new BlockPos(4, 2, 2);
        helper.setBlock(
                hostPos,
                ModBlocks.BRONZE_BOILER.get().defaultBlockState());
        BoilerBlockEntity host = helper.getBlockEntity(hostPos);
        FluidStack water = new FluidStack(Fluids.WATER, 1000);
        IFluidHandler hostSide = host.fluids(Direction.WEST);
        helper.assertTrue(hostSide != null, "Host has no west fluid capability");
        helper.assertTrue(
                hostSide.fill(
                        water.copy(),
                        IFluidHandler.FluidAction.SIMULATE) > 0,
                "Host capability rejected water");
        ItemStack cell = new ItemStack(Items.WATER_BUCKET);
        MteInPlaceBlock funnel = ModBlocks.mteInPlaceBlocksById()
                .get(id("fluid_attachment/stainless_funnel"))
                .get();
        helper.setBlock(
                funnelPos,
                funnel.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.EAST));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, cell);
        helper.assertTrue(
                helper.getBlockState(funnelPos).useItemOn(
                                cell,
                                helper.getLevel(),
                                player,
                                InteractionHand.MAIN_HAND,
                                hit(helper, funnelPos))
                        .consumesAction(),
                "Funnel did not consume a filled fluid-cell click");
        helper.assertTrue(
                host.waterAmount() == 1000,
                "Funnel did not fill the adjacent host");
        helper.assertTrue(
                player.getMainHandItem().is(Items.BUCKET),
                "Funnel did not return an empty bucket");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void creativeFunnelKeepsHeldFluidContainer(
            GameTestHelper helper) {
        BlockPos funnelPos = new BlockPos(3, 2, 2);
        BlockPos hostPos = new BlockPos(4, 2, 2);
        helper.setBlock(
                hostPos,
                ModBlocks.BRONZE_BOILER.get().defaultBlockState());
        BoilerBlockEntity host = helper.getBlockEntity(hostPos);
        MteInPlaceBlock funnel = ModBlocks.mteInPlaceBlocksById()
                .get(id("fluid_attachment/stainless_funnel"))
                .get();
        helper.setBlock(
                funnelPos,
                funnel.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.EAST));
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        helper.assertTrue(
                helper.getBlockState(funnelPos).useItemOn(
                                bucket,
                                helper.getLevel(),
                                player,
                                InteractionHand.MAIN_HAND,
                                hit(helper, funnelPos))
                        .consumesAction(),
                "Creative Funnel did not consume the click");
        helper.assertTrue(
                player.getMainHandItem().is(Items.WATER_BUCKET),
                "Creative Funnel consumed the held water bucket");
        helper.assertTrue(
                host.waterAmount() == 1000,
                "Creative Funnel did not fill the host");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void nozzleFillsGasCellFromHost(GameTestHelper helper) {
        BlockPos nozzlePos = new BlockPos(3, 2, 2);
        BlockPos hostPos = new BlockPos(4, 2, 2);
        FluidPipeBlockEntity host = placePipeConnected(
                helper, hostPos, "tungsten", Direction.WEST);
        helper.assertTrue(
                host.fillInternal(
                        new FluidStack(ModFluids.STEAM_SOURCE.get(), 1000),
                        IFluidHandler.FluidAction.EXECUTE) == 1000,
                "Could not prime the gas host");
        MteInPlaceBlock nozzle = ModBlocks.mteInPlaceBlocksById()
                .get(id("tungsten/nozzle"))
                .get();
        helper.setBlock(
                nozzlePos,
                nozzle.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.EAST));
        ItemStack cell = new ItemStack(ModItems.GAS_CELL.get());
        CellItem cellItem = (CellItem) ModItems.GAS_CELL.get();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, cell);
        helper.assertTrue(
                helper.getBlockState(nozzlePos).useItemOn(
                                cell,
                                helper.getLevel(),
                                player,
                                InteractionHand.MAIN_HAND,
                                hit(helper, nozzlePos))
                        .consumesAction(),
                "Nozzle did not consume a gas-cell click");
        helper.assertTrue(
                cellItem.content(player.getMainHandItem()).getAmount() == 1000,
                "Nozzle did not fill the gas cell");
        helper.assertTrue(
                host.storedFluid().isEmpty(),
                "Nozzle did not drain the gas host");
        helper.succeed();
    }

    private static FluidPipeBlockEntity placePipe(
            GameTestHelper helper, BlockPos pos) {
        return placePipe(helper, pos, "copper");
    }

    private static FluidPipeBlockEntity placePipe(
            GameTestHelper helper,
            BlockPos pos,
            String material) {
        FluidPipeBlock block = (FluidPipeBlock) ModBlocks.pipeBlock(
                material,
                MaterialPrefixes.FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        BlockState state = block.defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(
                    AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                    true);
        }
        helper.setBlock(pos, state);
        return helper.getBlockEntity(pos);
    }

    private static FluidPipeBlockEntity placePipeConnected(
            GameTestHelper helper,
            BlockPos pos,
            String material,
            Direction connection) {
        FluidPipeBlock block = (FluidPipeBlock) ModBlocks.pipeBlock(
                material,
                MaterialPrefixes.FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        BlockState state = block.defaultBlockState().setValue(
                AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(connection),
                true);
        helper.setBlock(pos, state);
        return helper.getBlockEntity(pos);
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(
                Vec3.atCenterOf(absolute),
                Direction.NORTH,
                absolute,
                false);
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(id(path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
