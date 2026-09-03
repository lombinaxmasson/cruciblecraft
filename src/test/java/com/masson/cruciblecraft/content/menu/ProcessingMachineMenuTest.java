package com.masson.cruciblecraft.content.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.masson.cruciblecraft.TestExtruderShapes;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProcessingMachineMenuTest {
    private static final ProcessingMachineSpec SPEC = spec();

    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void shiftMergeIntoExistingMachineInputNotifiesHandler() {
        CountingItemStackHandler machine = new CountingItemStackHandler(2);
        machine.setStackInSlot(0, new ItemStack(Items.COAL, 10));
        SimpleContainer player = new SimpleContainer(36);
        player.setItem(9, new ItemStack(Items.COAL, 5));
        TestMenu menu = new TestMenu(player, machine);
        machine.resetChanges();

        ItemStack moved = menu.quickMoveStack(null, 2);

        assertEquals(5, moved.getCount());
        assertEquals(15, machine.getStackInSlot(0).getCount());
        assertTrue(player.getItem(9).isEmpty());
        assertEquals(1, machine.changes());
        assertEquals(15, itemCount(machine, player));
    }

    @Test
    void partialShiftIntoMachineInputPreservesRemainderAndTotal() {
        CountingItemStackHandler machine = new CountingItemStackHandler(2);
        machine.setStackInSlot(0, new ItemStack(Items.COAL, 60));
        SimpleContainer player = new SimpleContainer(36);
        player.setItem(9, new ItemStack(Items.COAL, 10));
        TestMenu menu = new TestMenu(player, machine);
        machine.resetChanges();

        ItemStack moved = menu.quickMoveStack(null, 2);

        assertEquals(10, moved.getCount());
        assertEquals(64, machine.getStackInSlot(0).getCount());
        assertEquals(6, player.getItem(9).getCount());
        assertEquals(1, machine.changes());
        assertEquals(70, coalCount(machine, player));
    }

    @Test
    void partialShiftFromMachineOutputNotifiesLiveSourceMutation() {
        CountingItemStackHandler machine = new CountingItemStackHandler(2);
        machine.setStackInSlot(1, new ItemStack(Items.COAL, 10));
        SimpleContainer player = blockedPlayerInventory(8);
        player.setItem(8, new ItemStack(Items.COAL, 60));
        TestMenu menu = new TestMenu(player, machine);
        AtomicReference<ItemStack> taken = new AtomicReference<>();
        ProcessingMachineMenu.NotifyingSlotItemHandler sourceSlot =
                new ProcessingMachineMenu.NotifyingSlotItemHandler(machine, 1, 18, 0) {
                    @Override public void onTake(Player takingPlayer, ItemStack stack) {
                        taken.set(stack.copy());
                        super.onTake(takingPlayer, stack);
                    }
                };
        // Deliberately bypass addSlot: this keeps Slot.index at zero while the
        // inherited protected SlotItemHandler.index remains handler slot one.
        // The test therefore guards against accidentally using the menu index.
        menu.slots.set(1, sourceSlot);
        machine.resetChanges();

        ItemStack moved = menu.quickMoveStack(null, 1);

        assertEquals(10, moved.getCount());
        assertEquals(6, machine.getStackInSlot(1).getCount());
        assertEquals(6, taken.get().getCount(),
                "onTake receives the live remainder like vanilla quick-move menus");
        assertEquals(64, player.getItem(8).getCount());
        assertEquals(1, machine.changes());
        assertEquals(70, coalCount(machine, player));
    }

    @Test
    void externalMachineWriteBeforeShiftStillNotifiesTransferOnce() {
        CountingItemStackHandler machine = new CountingItemStackHandler(2);
        SimpleContainer player = blockedPlayerInventory(8);
        player.setItem(8, new ItemStack(Items.COAL, 60));
        TestMenu menu = new TestMenu(player, machine);
        machine.setStackInSlot(1, new ItemStack(Items.COAL, 10));
        machine.resetChanges();

        ItemStack moved = menu.quickMoveStack(null, 1);

        assertEquals(10, moved.getCount());
        assertEquals(6, machine.getStackInSlot(1).getCount());
        assertEquals(64, player.getItem(8).getCount());
        assertEquals(1, machine.changes(),
                "a stale slot snapshot must not duplicate the real transfer notification");
        assertEquals(70, coalCount(machine, player));
    }

    @Test
    void completeShiftFromMachineOutputStillClearsAndNotifies() {
        CountingItemStackHandler machine = new CountingItemStackHandler(2);
        machine.setStackInSlot(1, new ItemStack(Items.COAL, 10));
        SimpleContainer player = new SimpleContainer(36);
        TestMenu menu = new TestMenu(player, machine);
        machine.resetChanges();

        ItemStack moved = menu.quickMoveStack(null, 1);

        assertEquals(10, moved.getCount());
        assertTrue(machine.getStackInSlot(1).isEmpty());
        assertEquals(1, machine.changes());
        assertEquals(10, coalCount(machine, player));
    }

    @Test
    void shiftClickUsesTheSameExtruderSlotAcceptanceAsAutomation() {
        ProcessingMachineSpec shapeSpec = shapeSpec();
        ItemStack shape = TestExtruderShapes.stack();
        CountingItemStackHandler machine = new CountingItemStackHandler(3) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return shapeSpec.items().accepts(slot, stack);
            }
        };
        SimpleContainer player = new SimpleContainer(36);
        player.setItem(9, shape.copy());
        TestMenu menu = new TestMenu(player, machine, shapeSpec);

        assertEquals(1, menu.quickMoveStack(null, 3).getCount());
        assertTrue(machine.getStackInSlot(0).isEmpty());
        assertTrue(ExtruderShapeCatalog.isShape(machine.getStackInSlot(1)));

        CountingItemStackHandler materialMachine = new CountingItemStackHandler(3) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return shapeSpec.items().accepts(slot, stack);
            }
        };
        SimpleContainer materialPlayer = new SimpleContainer(36);
        materialPlayer.setItem(9, new ItemStack(Items.IRON_INGOT));
        TestMenu materialMenu =
                new TestMenu(materialPlayer, materialMachine, shapeSpec);
        assertEquals(1, materialMenu.quickMoveStack(null, 3).getCount());
        assertEquals(Items.IRON_INGOT, materialMachine.getStackInSlot(0).getItem());
        assertTrue(materialMachine.getStackInSlot(1).isEmpty());
    }

    @Test
    void exactClientStateBypassesShortSizedMenuData() {
        SimpleContainerData truncated = new SimpleContainerData(
                ConfiguredProcessingMachineMenu.dataCount());
        truncated.set(0, 30_000);
        truncated.set(1, -5_536);
        ExactStateMenu menu = new ExactStateMenu(
                new SimpleContainer(36),
                truncated);

        assertEquals(9, menu.scaledProgress(18));
        assertEquals(60_000, menu.duration());
        assertEquals(409_600, menu.tankAmount(0));
        assertEquals(4_000_000, menu.tankCapacity(0));
    }

    @Test
    void configuredStyleMenuScalesOnlyTheSynchronizedPermilleSlot() {
        SimpleContainerData synchronizedData =
                new SimpleContainerData(ConfiguredProcessingMachineMenu.dataCount());
        synchronizedData.set(
                ConfiguredProcessingMachineMenu.PROGRESS_PERMILLE_DATA_INDEX,
                500);
        PermilleMenu menu = new PermilleMenu(
                new SimpleContainer(36),
                synchronizedData);

        assertEquals(63, menu.progress());
        assertEquals(64, menu.duration());
        assertEquals(9, menu.scaledProgress(18));

        synchronizedData.set(
                ConfiguredProcessingMachineMenu.PROGRESS_PERMILLE_DATA_INDEX,
                1_000);
        assertEquals(18, menu.scaledProgress(18));
        synchronizedData.set(
                ConfiguredProcessingMachineMenu.PROGRESS_PERMILLE_DATA_INDEX,
                1_001);
        assertEquals(18, menu.scaledProgress(18));
        synchronizedData.set(
                ConfiguredProcessingMachineMenu.PROGRESS_PERMILLE_DATA_INDEX,
                -1);
        assertEquals(0, menu.scaledProgress(18));
    }

    private static SimpleContainer blockedPlayerInventory(int openSlot) {
        SimpleContainer player = new SimpleContainer(36);
        for (int slot = 0; slot < player.getContainerSize(); slot++) {
            if (slot != openSlot) {
                player.setItem(slot, new ItemStack(Items.DIRT, 64));
            }
        }
        return player;
    }

    private static int itemCount(
            CountingItemStackHandler machine,
            SimpleContainer player) {
        int total = 0;
        for (int slot = 0; slot < machine.getSlots(); slot++) {
            total += machine.getStackInSlot(slot).getCount();
        }
        for (int slot = 0; slot < player.getContainerSize(); slot++) {
            total += player.getItem(slot).getCount();
        }
        return total;
    }

    private static int coalCount(
            CountingItemStackHandler machine,
            SimpleContainer player) {
        int total = 0;
        for (int slot = 0; slot < machine.getSlots(); slot++) {
            if (machine.getStackInSlot(slot).is(Items.COAL)) {
                total += machine.getStackInSlot(slot).getCount();
            }
        }
        for (int slot = 0; slot < player.getContainerSize(); slot++) {
            if (player.getItem(slot).is(Items.COAL)) {
                total += player.getItem(slot).getCount();
            }
        }
        return total;
    }

    private static ProcessingMachineSpec spec() {
        ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath("test", "menu");
        return new ProcessingMachineSpec(
                id,
                id,
                () -> new RecipeMap(id),
                new ProcessingMachineSpec.SlotLayout(
                        2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        100,
                        16),
                new ProcessingMachineSpec.SidedIoPolicy(
                        (front, side) -> ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) -> ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) -> ProcessingMachineSpec.CapabilityAccess.NONE),
                recipe -> Optional.empty(),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                new ProcessingMachineSpec.UiLayout(
                        List.of(
                                new ProcessingMachineSpec.SlotPosition(0, 0),
                                new ProcessingMachineSpec.SlotPosition(18, 0)),
                        new ProcessingMachineSpec.ProgressBar(0, 0, 1, 1),
                        List.of(),
                        List.of("idle")));
    }

    private static ProcessingMachineSpec shapeSpec() {
        ProcessingMachineSpec base = spec();
        return new ProcessingMachineSpec(
                base.id(),
                base.recipeMapId(),
                base.recipeMap(),
                new ProcessingMachineSpec.SlotLayout(
                        3,
                        List.of(0, 1),
                        List.of(2),
                        Map.of(
                                0, ProcessingMachineSpec.SlotRole.MATERIAL,
                                1, ProcessingMachineSpec.SlotRole.TOOL,
                                2, ProcessingMachineSpec.SlotRole.OUTPUT),
                        (slot, stack) -> slot == 1
                                ? ExtruderShapeCatalog.isShape(stack)
                                : !ExtruderShapeCatalog.isShape(stack)),
                base.fluids(),
                base.energy(),
                base.sidedIo(),
                base.validator(),
                base.buffering(),
                new ProcessingMachineSpec.UiLayout(
                        List.of(
                                new ProcessingMachineSpec.SlotPosition(0, 0),
                                new ProcessingMachineSpec.SlotPosition(18, 0),
                                new ProcessingMachineSpec.SlotPosition(36, 0)),
                        base.ui().progress(),
                        List.of(),
                        List.of("idle")));
    }

    private static final class TestMenu extends ProcessingMachineMenu {
        private TestMenu(
                SimpleContainer player,
                CountingItemStackHandler machine) {
            this(player, machine, SPEC);
        }

        private TestMenu(
                SimpleContainer player,
                CountingItemStackHandler machine,
                ProcessingMachineSpec spec) {
            super(
                    null,
                    0,
                    player,
                    machine,
                    new SimpleContainerData(0),
                    spec,
                    ignored -> true);
        }
    }

    private static final class ExactStateMenu extends ProcessingMachineMenu {
        private final FluidStack fluid = new FluidStack(Fluids.WATER, 409_600);

        private ExactStateMenu(
                SimpleContainer player,
                SimpleContainerData truncated) {
            super(
                    null,
                    0,
                    player,
                    new ItemStackHandler(
                            ModProcessingMachines.AUTOCLAVE.items().slotCount()),
                    truncated,
                    ModProcessingMachines.AUTOCLAVE,
                    ignored -> true);
        }

        @Override public int progress() {
            return 30_000;
        }

        @Override public int duration() {
            return 60_000;
        }

        @Override public FluidStack tankFluid(int tank) {
            return tank == 0 ? fluid.copy() : FluidStack.EMPTY;
        }

        @Override public int tankAmount(int tank) {
            return tankFluid(tank).getAmount();
        }
    }

    private static final class PermilleMenu extends ProcessingMachineMenu {
        private PermilleMenu(
                SimpleContainer player,
                SimpleContainerData synchronizedData) {
            super(
                    null,
                    0,
                    player,
                    new ItemStackHandler(SPEC.items().slotCount()),
                    synchronizedData,
                    SPEC,
                    ignored -> true);
        }

        @Override public int progress() {
            return 63;
        }

        @Override public int duration() {
            return 64;
        }

        @Override protected int progressPermilleDataIndex() {
            return ConfiguredProcessingMachineMenu.PROGRESS_PERMILLE_DATA_INDEX;
        }
    }

    private static class CountingItemStackHandler extends ItemStackHandler {
        private int changes;

        private CountingItemStackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            changes++;
        }

        private int changes() {
            return changes;
        }

        private void resetChanges() {
            changes = 0;
        }
    }
}
