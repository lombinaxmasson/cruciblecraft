package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.content.item.MaterialToolItem;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class MaterialItemColor {
    private static final int FIRST_GLOW_COLOR = 0xFFFF4A1F;
    private static final int HOT_GLOW_COLOR = 0xFFFFE0A3;
    private static final float GLOW_START_TEMPERATURE = 500.0f;

    private MaterialItemColor() {}

    public static int color(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        MaterialDefinition material;
        boolean neutralTexture;
        if (stack.getItem() instanceof MaterialToolItem tool) {
            var materialId = tool.material(stack);
            if (materialId.isEmpty()) {
                return 0xFFFFFFFF;
            }
            material = MaterialCatalog.find(materialId.orElseThrow())
                    .orElse(null);
            neutralTexture = true;
        } else {
            var entry = MaterialUnits.resolve(stack);
            if (entry.isEmpty()) {
                return 0xFFFFFFFF;
            }
            material = MaterialCatalog.find(
                    entry.orElseThrow().materialId()).orElse(null);
            neutralTexture = stack.getItem() instanceof MaterialFormItem;
        }
        if (material == null) {
            return 0xFF000000 | baseColor(null, neutralTexture);
        }
        // Generated material textures are neutral and need the material's base
        // tint. Canonical vanilla items already contain their cold color, so
        // white preserves that texture while still allowing the heat glow.
        int baseColor = baseColor(material, neutralTexture);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0xFF000000 | baseColor;
        }

        float temperature = ItemHeat.temperature(stack, minecraft.level.getGameTime());
        float meltingPoint = (float) material.thermal().meltingPoint();
        return colorAtTemperature(baseColor, temperature, meltingPoint);
    }

    static int baseColor(
            MaterialDefinition material, boolean neutralTexture) {
        if (material == null) {
            return 0xFFFFFF;
        }
        return neutralTexture
                ? styleColor(material.colorRgb(), material.tintStyle())
                : 0xFFFFFF;
    }

    public static int styleColor(int baseColor, String tintStyle) {
        return switch (tintStyle) {
            case "shiny" -> mix(baseColor, 0xFFFFFF, 0.15F);
            case "matte" -> mix(baseColor, 0x808080, 0.10F);
            default -> baseColor;
        };
    }

    public static int colorAtTemperature(int baseColor, float temperature, float meltingPoint) {
        if (temperature <= GLOW_START_TEMPERATURE) {
            return 0xFF000000 | baseColor;
        }

        float hotPoint = Math.max(GLOW_START_TEMPERATURE + 1.0f, meltingPoint);
        float glow = clamp((temperature - GLOW_START_TEMPERATURE)
                / (hotPoint - GLOW_START_TEMPERATURE));
        int firstStage = mix(baseColor, FIRST_GLOW_COLOR, Math.min(1.0f, glow * 2.0f));
        int result = glow <= 0.5f
                ? firstStage
                : mix(FIRST_GLOW_COLOR, HOT_GLOW_COLOR, (glow - 0.5f) * 2.0f);
        return 0xFF000000 | result;
    }

    private static int mix(int from, int to, float amount) {
        float clamped = clamp(amount);
        int red = Math.round(((from >> 16) & 0xFF) * (1.0f - clamped)
                + ((to >> 16) & 0xFF) * clamped);
        int green = Math.round(((from >> 8) & 0xFF) * (1.0f - clamped)
                + ((to >> 8) & 0xFF) * clamped);
        int blue = Math.round((from & 0xFF) * (1.0f - clamped)
                + (to & 0xFF) * clamped);
        return (red << 16) | (green << 8) | blue;
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
