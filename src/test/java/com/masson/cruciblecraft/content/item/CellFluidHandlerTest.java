package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CellFluidHandlerTest {
    private static DataComponentType<SimpleFluidContent> content;

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        content = DataComponentType.<SimpleFluidContent>builder()
                .persistent(SimpleFluidContent.CODEC)
                .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                .build();
    }

    @Test
    void splitSingleCellIsSimulatedAndExecutedWithoutMutatingSourceStack() {
        ItemStack source = new ItemStack(Items.GLASS_BOTTLE, 64);
        CellFluidHandler sourceHandler =
                new CellFluidHandler(() -> content, source, fluid -> true);
        FluidStack water = new FluidStack(Fluids.WATER, 1_000);
        assertEquals(
                0,
                sourceHandler.fill(
                        water,
                        IFluidHandler.FluidAction.EXECUTE));
        assertFalse(source.has(content));

        ItemStack single = source.copyWithCount(1);
        CellFluidHandler handler =
                new CellFluidHandler(() -> content, single, fluid -> true);
        assertEquals(
                1_000,
                handler.fill(
                        water,
                        IFluidHandler.FluidAction.SIMULATE));
        assertFalse(single.has(content));
        assertEquals(
                1_000,
                handler.fill(
                        water,
                        IFluidHandler.FluidAction.EXECUTE));
        assertEquals(1_000, handler.getFluidInTank(0).getAmount());

        assertEquals(
                1_000,
                handler.drain(
                        1_000,
                        IFluidHandler.FluidAction.SIMULATE).getAmount());
        assertTrue(single.has(content));
        assertEquals(
                1_000,
                handler.drain(
                        1_000,
                        IFluidHandler.FluidAction.EXECUTE).getAmount());
        assertFalse(single.has(content));
    }

    @Test
    void contentPolicyAndDynamicStackLimitsFailClosed() {
        assertEquals(64, CellItem.stackLimit(SimpleFluidContent.EMPTY));
        assertEquals(
                1,
                CellItem.stackLimit(SimpleFluidContent.copyOf(
                        new FluidStack(Fluids.WATER, 1_000))));

        CellFluidHandler rejecting =
                new CellFluidHandler(
                        () -> content,
                        new ItemStack(Items.GLASS_BOTTLE),
                        fluid -> false);
        assertEquals(
                0,
                rejecting.fill(
                        new FluidStack(Fluids.WATER, 1_000),
                        IFluidHandler.FluidAction.SIMULATE));
    }

    @Test
    void manualInteractionDelegatesSingleAndStackedTransactionsToFluidUtil()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/item/CellItem.java"));

        assertFalse(source.contains("held.getCount() <= 1"));
        assertFalse(source.contains("CellStackOps"));
        assertTrue(source.contains("FluidUtil.interactWithFluidHandler"));
    }
}
