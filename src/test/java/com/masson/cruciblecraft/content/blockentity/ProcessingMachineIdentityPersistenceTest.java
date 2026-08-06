package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.MachineIdentityPolicy;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineState;
import com.masson.cruciblecraft.registry.ModMachineIdentityMigrations;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProcessingMachineIdentityPersistenceTest {
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void quarantineReloadStopsProcessingAndPreservesRecoverableState() {
        MachineVariant variant =
                ModMultiblockControllers.LARGE_CENTRIFUGE_VARIANT;
        MachineIdentityPolicy.Identity current =
                ModMachineIdentityMigrations.identityOf(variant);
        List<MachineIdentityPolicy.Identity> mismatches = List.of(
                new MachineIdentityPolicy.Identity(
                        current.machineKind() + "_wrong",
                        current.tierProfile(),
                        current.materialId(),
                        current.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        current.machineKind(),
                        current.tierProfile() + "_wrong",
                        current.materialId(),
                        current.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        current.machineKind(),
                        current.tierProfile(),
                        current.materialId() + "_wrong",
                        current.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        current.machineKind(),
                        current.tierProfile(),
                        current.materialId(),
                        "KINETIC_PUSH"));

        for (MachineIdentityPolicy.Identity mismatch : mismatches) {
            TestMachine source = seededMachine(variant);
            CompoundTag persisted = source.saveForTest();
            putIdentity(persisted, mismatch);

            TestMachine restored = new TestMachine(variant);
            restored.loadForTest(persisted);

            assertFalse(restored.processingAllowed());
            assertEquals("material_quarantined", restored.pausedReason());
            assertTrue(restored.clientTagForTest()
                    .getString("material_quarantine")
                    .contains("does not match"));
            assertEquals(9, restored.progress());
            assertEquals(
                    777L,
                    restored.stored(EnergyType.KINETIC_ROTATION));
            assertEquals(
                    3,
                    restored.inventory().getStackInSlot(inputSlot(restored))
                            .getCount());
            assertEquals(400, restored.tanks().getFirst().getFluidAmount());

            ProcessingMachineState resaved =
                    ProcessingMachineState.read(restored.saveForTest());
            assertEquals(
                    mismatch,
                    ModMachineIdentityMigrations.identityOf(resaved));

            ItemStack extracted = restored.inventory().extractItem(
                    inputSlot(restored), 2, false);
            assertTrue(extracted.is(Items.IRON_INGOT));
            assertEquals(2, extracted.getCount());
            assertEquals(
                    1,
                    restored.inventory().getStackInSlot(inputSlot(restored))
                            .getCount());
        }
    }

    @Test
    void exactMigrationReloadResumesAndWritesCurrentIdentityOnNextSave() {
        MachineVariant variant =
                ModMultiblockControllers.LARGE_CENTRIFUGE_VARIANT;
        TestMachine source = seededMachine(variant);
        CompoundTag persisted = source.saveForTest();
        putIdentity(
                persisted,
                ModMachineIdentityMigrations
                        .LEGACY_LARGE_CENTRIFUGE_IDENTITY);

        TestMachine restored = new TestMachine(variant);
        restored.loadForTest(persisted);

        assertTrue(restored.processingAllowed());
        assertFalse(restored.pausedReason().equals("material_quarantined"));
        assertFalse(restored.clientTagForTest()
                .contains("material_quarantine"));
        assertEquals(9, restored.progress());
        assertEquals(
                777L,
                restored.stored(EnergyType.KINETIC_ROTATION));
        assertEquals(
                ModMachineIdentityMigrations.LARGE_CENTRIFUGE_IDENTITY,
                ModMachineIdentityMigrations.identityOf(
                        ProcessingMachineState.read(
                                restored.saveForTest())));
    }

    @Test
    void representativeT16LegacyLathePreservesInventoryAndProgress() {
        MachineVariant variant = ModMachineVariants.require(
                ResourceLocation.parse("cruciblecraft:lathe"));
        TestMachine source = seededMachine(variant);
        CompoundTag persisted = source.saveForTest();
        putIdentity(
                persisted,
                ModMachineIdentityMigrations.legacyIdentityOf(variant));

        TestMachine restored = new TestMachine(variant);
        restored.loadForTest(persisted);

        assertTrue(restored.processingAllowed());
        assertFalse(restored.pausedReason().equals("material_quarantined"));
        assertEquals(9, restored.progress());
        assertEquals(
                3,
                restored.inventory().getStackInSlot(inputSlot(restored))
                        .getCount());
        assertEquals(
                ModMachineIdentityMigrations.identityOf(variant),
                ModMachineIdentityMigrations.identityOf(
                        ProcessingMachineState.read(
                                restored.saveForTest())));
    }

    @Test
    void t17LegacyTierOneMigrationPreservesResourcesProgressAndEnergy() {
        for (MachineIdentityPolicy.Migration migration
                : ModMachineIdentityMigrations
                        .T17_LEGACY_TIER1_MIGRATIONS) {
            MachineVariant variant = ModMachineVariants.require(
                    ResourceLocation.parse(migration.variantId()));
            TestMachine source = seededMachine(variant);
            CompoundTag persisted = source.saveForTest();
            putIdentity(persisted, migration.from());
            persisted.putLong("energy", 777L);

            TestMachine restored = new TestMachine(variant);
            restored.loadForTest(persisted);

            assertTrue(restored.processingAllowed(), migration.variantId());
            assertFalse(
                    restored.pausedReason().equals("material_quarantined"),
                    migration.variantId());
            assertEquals(9, restored.progress(), migration.variantId());
            assertEquals(
                    3,
                    restored.inventory().getStackInSlot(inputSlot(restored))
                            .getCount(),
                    migration.variantId());
            assertEquals(
                    400,
                    restored.tanks().getFirst().getFluidAmount(),
                    migration.variantId());

            ProcessingMachineState resaved =
                    ProcessingMachineState.read(restored.saveForTest());
            assertEquals(777L, resaved.energy(), migration.variantId());
            assertEquals(
                    ModMachineIdentityMigrations.identityOf(variant),
                    ModMachineIdentityMigrations.identityOf(resaved),
                    migration.variantId());
        }
    }

    private static TestMachine seededMachine(MachineVariant variant) {
        TestMachine machine = new TestMachine(variant);
        machine.inventory().setStackInSlot(
                inputSlot(machine), new ItemStack(Items.IRON_INGOT, 3));
        if (!machine.tanks().isEmpty()) {
            machine.tanks().getFirst().setFluid(
                    new FluidStack(Fluids.WATER, 400));
        }
        machine.restoreEnergyForTest(777L);
        machine.runtime().restore(
                "cruciblecraft:test_identity_recipe",
                9,
                40,
                "processing");
        return machine;
    }

    private static int inputSlot(TestMachine machine) {
        return machine.spec().items().inputs().getFirst();
    }

    private static void putIdentity(
            CompoundTag tag,
            MachineIdentityPolicy.Identity identity) {
        tag.putString("machine_kind", identity.machineKind());
        tag.putString("tier_profile", identity.tierProfile());
        tag.putString("tier_material", identity.materialId());
        tag.putString("energy_identity", identity.energyIdentity());
    }

    private static final class TestMachine
            extends ProcessingMachineBlockEntity {
        private TestMachine(MachineVariant variant) {
            super(
                    BlockEntityType.FURNACE,
                    BlockPos.ZERO,
                    Blocks.FURNACE.defaultBlockState(),
                    variant);
        }

        private CompoundTag saveForTest() {
            CompoundTag tag = new CompoundTag();
            saveAdditional(tag, registries);
            return tag;
        }

        private void loadForTest(CompoundTag tag) {
            loadAdditional(tag, registries);
        }

        private CompoundTag clientTagForTest() {
            return writeClientTag(registries);
        }

        private void restoreEnergyForTest(long value) {
            restoreEnergy(value);
        }

        @Override
        protected Direction machineFront() {
            return Direction.NORTH;
        }
    }
}
