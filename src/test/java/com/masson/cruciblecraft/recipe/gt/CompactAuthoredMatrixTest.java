package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.network.connection.ConnectionType;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactAuthoredMatrixTest {
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void expandReplaysSharedShapeRows() {
        CompactGTRecipeFamilyDefinition definition = matrixDefinition();
        List<CompactGTRecipeFamilyDefinition.Relation> expanded =
                CompactAuthoredMatrix.expand(definition);
        assertEquals(2, expanded.size());
        assertEquals(id("assembler/matrix/first"), expanded.get(0).stableId());
        assertEquals(id("assembler/matrix/second"), expanded.get(1).stableId());
        assertEquals(Items.IRON_INGOT, expanded.get(0).itemInputs().getFirst().getItems()[0].getItem());
        assertEquals(Items.GOLD_INGOT, expanded.get(1).itemInputs().getFirst().getItems()[0].getItem());
        assertEquals(Items.IRON_NUGGET, expanded.get(0).itemOutputs().getFirst().getItem());
        assertEquals("SOURCE_BACKED", expanded.get(0).provenance().sourceKind());
        assertEquals(0, definition.relations().size());
    }

    @Test
    void unknownParameterizedTemplateFailsClosed() {
        CompactGTRecipeFamilyDefinition definition = new CompactGTRecipeFamilyDefinition(
                "future.assembler#parameterized",
                ModRecipeMaps.ASSEMBLER.id(),
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                List.of(),
                Optional.of(new CompactGTRecipeFamilyDefinition.ParameterizedSpec(
                        "bath_template",
                        Map.of("axis", "material"))));
        List<CompactGTRecipeFamilyDefinition.Relation> expanded =
                CompactAuthoredMatrix.expand(definition);
        assertTrue(expanded.isEmpty());
    }

    @Test
    void compactMatrixAliasRequiresMatrix() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> new CompactGTRecipeFamilyDefinition(
                        "future.assembler#alias",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(),
                        Optional.of(new CompactGTRecipeFamilyDefinition.ParameterizedSpec(
                                CompactAuthoredMatrix.TEMPLATE_COMPACT_MATRIX_V1,
                                Map.of())),
                        Optional.empty(),
                        Optional.empty()));
        assertTrue(thrown.getMessage().contains("requires matrix"));
    }

    @Test
    void matrixAndInlineRelationsAreXor() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> new CompactGTRecipeFamilyDefinition(
                        "future.assembler#xor",
                        ModRecipeMaps.ASSEMBLER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(inlineRelation("assembler/matrix/inline", Items.IRON_INGOT, Items.IRON_NUGGET, 0)),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(matrix())));
        assertTrue(thrown.getMessage().contains("cannot mix matrix"));
    }

    @Test
    void outOfRangeRowIndexFailsClosed() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> new CompactGTRecipeFamilyDefinition.AuthoredMatrixV1(
                        shared(),
                        dicts(),
                        List.of(new CompactGTRecipeFamilyDefinition.MatrixRow(
                                9,
                                0,
                                0,
                                id("assembler/matrix/bad"),
                                0))));
        assertTrue(thrown.getMessage().contains("item_inputs"));
        assertTrue(thrown.getMessage().contains("out of range"));
    }

    @Test
    void rowCeilingFailsClosed() {
        List<CompactGTRecipeFamilyDefinition.MatrixRow> rows = new ArrayList<>();
        for (int index = 0; index < CompactRecipeWireLimits.DECODE_RELATIONS_CEILING + 1; index++) {
            rows.add(new CompactGTRecipeFamilyDefinition.MatrixRow(
                    0,
                    0,
                    0,
                    id("assembler/matrix/row_" + index),
                    index));
        }
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> new CompactGTRecipeFamilyDefinition.AuthoredMatrixV1(
                        shared(),
                        dicts(),
                        rows));
        assertTrue(thrown.getMessage().contains("row count"));
    }

    @Test
    void streamCodecRoundTripsMatrixWithoutAllocatingInlineRelations() {
        CompactGTRecipeFamilyEntry original = new CompactGTRecipeFamilyEntry(matrixDefinition());
        CompactGTRecipeFamilySerializer serializer = new CompactGTRecipeFamilySerializer();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        serializer.streamCodec().encode(buffer, original);
        CompactGTRecipeFamilyEntry decoded = serializer.streamCodec().decode(buffer);
        assertEquals(0, decoded.definition().relations().size());
        assertTrue(decoded.definition().matrix().isPresent());
        assertEquals(2, decoded.definition().authoredRelations().size());
        assertEquals(
                original.definition().authoredRelations().get(0).stableId(),
                decoded.definition().authoredRelations().get(0).stableId());
        assertEquals(
                original.definition().authoredRelations().get(1).stableId(),
                decoded.definition().authoredRelations().get(1).stableId());
    }

    @Test
    void brokenOreHostStateRoundTripsOnMatrixWire() {
        CompactGTRecipeFamilyGeneratedSupport.installPrefixMaterialRouting();
        BlockItemStateProperties state = new BlockItemStateProperties(Map.of(
                "flower", "tungstus",
                "host", "granite_black"));
        Ingredient input = DataComponentIngredient.of(
                false,
                DataComponentPredicate.builder()
                        .expect(ModComponents.ORE_MATERIAL.get(), "actinium")
                        .expect(DataComponents.BLOCK_STATE, state)
                        .build(),
                Items.COBBLESTONE);
        ItemStack output = new ItemStack(Items.GRAVEL);
        output.set(DataComponents.BLOCK_STATE, state);
        CompactGTRecipeFamilyEntry original = new CompactGTRecipeFamilyEntry(
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.crusher#block_state",
                        ModRecipeMaps.CRUSHER.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(new CompactGTRecipeFamilyDefinition.AuthoredMatrixV1(
                                shared(),
                                new CompactGTRecipeFamilyDefinition.MatrixDicts(
                                        List.of(List.of(input)),
                                        List.of(List.of(output)),
                                        List.of(new CompactGTRecipeFamilyDefinition.FluidIo(
                                                List.of(), List.of()))),
                                List.of(new CompactGTRecipeFamilyDefinition.MatrixRow(
                                        0, 0, 0, id("crusher/block_state"), 0))))));
        CompactGTRecipeFamilySerializer serializer = new CompactGTRecipeFamilySerializer();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        serializer.streamCodec().encode(buffer, original);
        CompactGTRecipeFamilyEntry decoded = serializer.streamCodec().decode(buffer);
        assertEquals(0, buffer.readableBytes());

        Ingredient decodedInput = decoded.definition().matrix().orElseThrow()
                .dicts().itemInputs().getFirst().getFirst();
        DataComponentIngredient components = assertInstanceOf(
                DataComponentIngredient.class, decodedInput.getCustomIngredient());
        assertFalse(components.isStrict());
        assertEquals(
                "actinium",
                components.components().asPatch()
                        .get(ModComponents.ORE_MATERIAL.get())
                        .orElseThrow());
        assertEquals(
                state,
                components.components().asPatch()
                        .get(DataComponents.BLOCK_STATE)
                        .orElseThrow());
        assertEquals(
                state,
                decoded.definition().matrix().orElseThrow()
                        .dicts().itemOutputs().getFirst().getFirst()
                        .get(DataComponents.BLOCK_STATE));
    }

    @Test
    void blockStatePropertyCeilingStaysBounded() {
        CompactGTRecipeFamilyGeneratedSupport.installPrefixMaterialRouting();
        Map<String, String> properties = new LinkedHashMap<>();
        for (int index = 0;
                index < CompactRecipeWireLimits.MAX_BLOCK_STATE_PROPERTIES + 1;
                index++) {
            properties.put("host_" + index, "stone");
        }
        Ingredient input = DataComponentIngredient.of(
                false,
                DataComponentPredicate.builder()
                        .expect(
                                DataComponents.BLOCK_STATE,
                                new BlockItemStateProperties(properties))
                        .build(),
                Items.COBBLESTONE);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        EncoderException thrown = assertThrows(
                EncoderException.class,
                () -> CompactRecipeWireValues.encodeIngredient(buffer, input));
        assertTrue(thrown.getMessage().contains("block state"));
    }

    private static CompactGTRecipeFamilyDefinition matrixDefinition() {
        return new CompactGTRecipeFamilyDefinition(
                "future.assembler#matrix",
                ModRecipeMaps.ASSEMBLER.id(),
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(matrix()));
    }

    private static CompactGTRecipeFamilyDefinition.AuthoredMatrixV1 matrix() {
        return new CompactGTRecipeFamilyDefinition.AuthoredMatrixV1(
                shared(),
                dicts(),
                List.of(
                        new CompactGTRecipeFamilyDefinition.MatrixRow(
                                0, 0, 0, id("assembler/matrix/first"), 0),
                        new CompactGTRecipeFamilyDefinition.MatrixRow(
                                1, 1, 0, id("assembler/matrix/second"), 1)));
    }

    private static CompactGTRecipeFamilyDefinition.SharedSpec shared() {
        return new CompactGTRecipeFamilyDefinition.SharedSpec(
                32,
                16L,
                0L,
                true,
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                "SOURCE_BACKED",
                "gt.recipe.assembler#matrix");
    }

    private static CompactGTRecipeFamilyDefinition.MatrixDicts dicts() {
        return new CompactGTRecipeFamilyDefinition.MatrixDicts(
                List.of(
                        List.of(Ingredient.of(Items.IRON_INGOT)),
                        List.of(Ingredient.of(Items.GOLD_INGOT))),
                List.of(
                        List.of(new ItemStack(Items.IRON_NUGGET)),
                        List.of(new ItemStack(Items.GOLD_NUGGET))),
                List.of(new CompactGTRecipeFamilyDefinition.FluidIo(List.of(), List.of())));
    }

    private static CompactGTRecipeFamilyDefinition.Relation inlineRelation(
            String path,
            net.minecraft.world.item.Item input,
            net.minecraft.world.item.Item output,
            int shadowOrder) {
        return new CompactGTRecipeFamilyDefinition.Relation(
                id(path),
                List.of(Ingredient.of(input)),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(new ItemStack(output)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                32,
                16L,
                0L,
                true,
                shadowOrder,
                new GTRecipeProvenance(
                        "SOURCE_BACKED",
                        Optional.of("gt.recipe.assembler#matrix")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
