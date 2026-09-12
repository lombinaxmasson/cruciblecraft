package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** Exact GT6 food operand needed by the Pressure Washer live family. */
public final class PressureWasherOperandCatalog {
    private static final List<Operand> OPERANDS = List.of(
            new Operand(
                    ResourceLocation.fromNamespaceAndPath(
                            "cruciblecraft",
                            "cinnamon/bark_don_t_let_anyone_challenge_you"),
                    "cinnamon/bark_don_t_let_anyone_challenge_you",
                    280,
                    "Cinnamon Bark",
                    "肉桂树皮"));

    private PressureWasherOperandCatalog() {}

    public static List<Operand> operands() {
        return OPERANDS;
    }

    public record Operand(
            ResourceLocation id,
            String registryPath,
            int meta,
            String englishName,
            String chineseName) {
        public Operand {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(registryPath, "registryPath");
            Objects.requireNonNull(englishName, "englishName");
            Objects.requireNonNull(chineseName, "chineseName");
        }
    }
}
