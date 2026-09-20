package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.machine.processing.MachineTransaction;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProcessingMachinePowerCommitTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void failedEnergyExecutionLeavesTransactionResourcesUntouched() {
        MemoryResources resources = new MemoryResources(new ItemStack(Items.IRON_INGOT));
        MachineTransaction transaction = transaction(resources, ItemStack.EMPTY);

        assertThrows(IllegalStateException.class, () ->
                ProcessingMachineBlockEntity.executePowerForTransaction(
                        transaction, resources, () -> false));

        assertEquals(Items.IRON_INGOT, resources.item(0).getItem());
        assertEquals(1, resources.item(0).getCount());
    }

    @Test
    void reentrantEnergyMutationFailsWithoutOverwritingTheNewState() {
        MemoryResources resources = new MemoryResources(new ItemStack(Items.IRON_INGOT));
        MachineTransaction transaction = transaction(resources, ItemStack.EMPTY);

        assertThrows(IllegalStateException.class, () ->
                ProcessingMachineBlockEntity.executePowerForTransaction(
                        transaction,
                        resources,
                        () -> {
                            resources.setItem(0, new ItemStack(Items.GOLD_INGOT));
                            return true;
                        }));

        assertEquals(Items.GOLD_INGOT, resources.item(0).getItem());
    }

    @Test
    void successfulEnergyExecutionAllowsThePreparedCommit() {
        MemoryResources resources = new MemoryResources(new ItemStack(Items.IRON_INGOT));
        MachineTransaction transaction = transaction(resources, ItemStack.EMPTY);

        ProcessingMachineBlockEntity.executePowerForTransaction(
                transaction, resources, () -> true);

        assertTrue(transaction.commit(resources));
        assertTrue(resources.item(0).isEmpty());
    }

    private static MachineTransaction transaction(
            MemoryResources resources,
            ItemStack after) {
        return new MachineTransaction(
                List.of(resources.item(0)),
                List.of(after),
                List.of(),
                List.of());
    }

    private static final class MemoryResources implements MachineTransaction.ResourceAccess {
        private final List<ItemStack> items;

        private MemoryResources(ItemStack stack) {
            items = new ArrayList<>(List.of(stack.copy()));
        }

        @Override public int itemCount() { return items.size(); }
        @Override public ItemStack item(int slot) { return items.get(slot).copy(); }
        @Override public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack.copy());
        }
        @Override public int fluidCount() { return 0; }
        @Override public FluidStack fluid(int tank) {
            throw new IndexOutOfBoundsException(tank);
        }
        @Override public void setFluid(int tank, FluidStack stack) {
            throw new IndexOutOfBoundsException(tank);
        }
    }
}
