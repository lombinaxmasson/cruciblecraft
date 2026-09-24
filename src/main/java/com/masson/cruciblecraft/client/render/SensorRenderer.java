package com.masson.cruciblecraft.client.render;

import java.util.Locale;

import com.masson.cruciblecraft.content.block.SensorBlock;
import com.masson.cruciblecraft.content.blockentity.SensorBlockEntity;
import com.masson.cruciblecraft.content.sensor.SensorKind;
import com.masson.cruciblecraft.content.sensor.SensorMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * Dynamic GT6-style sensor readout.
 *
 * <p>The imported sensor overlay supplies the static icon and dark display
 * cells. The original GT6 renderer adds six character passes on top of that
 * overlay; this renderer draws the same logical slots with the client font:
 * five numeric positions (or a mode symbol plus four threshold positions) and
 * one unit position.
 */
public final class SensorRenderer
        implements BlockEntityRenderer<SensorBlockEntity> {
    private static final float FACE_OFFSET = 0.377F;
    private static final float TEXT_OFFSET = 0.003F;
    // The imported face puts the display cells above the block centre.  The
    // previous baseline placed the font origin at the lower edge of those
    // cells, making every readout visibly sag.
    private static final float BASELINE = 0.3125F;
    private static final float TEXT_SCALE = 0.0125F;

    public SensorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            SensorBlockEntity sensor,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        Direction facing = sensor.getBlockState().getValue(SensorBlock.FACING);
        String text = displayText(sensor);
        if (text.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        float width = font.width(text) * TEXT_SCALE;
        poseStack.pushPose();
        poseStack.translate(
                0.5 - facing.getStepX() * FACE_OFFSET,
                0.5 - facing.getStepY() * FACE_OFFSET,
                0.5 - facing.getStepZ() * FACE_OFFSET);
        orientToFace(poseStack, facing);
        poseStack.translate(-width / 2.0F, BASELINE, TEXT_OFFSET);
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        font.drawInBatch(
                text,
                0.0F,
                0.0F,
                color(sensor.kind(), sensor.mode()),
                false,
                poseStack.last().pose(),
                buffers,
                Font.DisplayMode.SEE_THROUGH,
                0,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void orientToFace(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> {
                // The default text plane faces SOUTH.
            }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
    }

    private static String displayText(SensorBlockEntity sensor) {
        SensorMode mode = sensor.mode();
        String unit = unit(sensor.kind());
        if (mode == SensorMode.FULL) {
            return "=100%" + unit;
        }
        if (mode == SensorMode.NOT_FULL) {
            return "<100%" + unit;
        }
        if (mode.usesSetNumber()) {
            String symbol = switch (mode) {
                case GREATER -> ">";
                case EQUAL -> "=";
                case SMALLER -> "<";
                case SCALE -> "~";
                default -> "";
            };
            return symbol
                    + digits(sensor.setNumber(), 4, sensor.hexadecimal())
                    + unit;
        }
        String suffix = mode == SensorMode.PERCENT ? "%" : unit;
        String prefix = sensor.hexadecimal() ? "0x" : "";
        int width = sensor.hexadecimal() ? 4 : 5;
        return prefix
                + digits(sensor.displayedNumber(), width, sensor.hexadecimal())
                + suffix;
    }

    private static String digits(long value, int width, boolean hexadecimal) {
        long bounded = Math.max(0L, Math.min(0xFFFFL, value));
        int radix = hexadecimal ? 16 : 10;
        String text = Long.toString(bounded, radix).toUpperCase(Locale.ROOT);
        if (text.length() >= width) {
            return text.substring(text.length() - width);
        }
        return "0".repeat(width - text.length()) + text;
    }

    private static String unit(SensorKind kind) {
        return switch (kind) {
            case THERMOMETER -> "K";
            case GIBBLOMETER, KILO_GIBBLOMETER -> "G";
            case LUMINOMETER -> "lm";
            case CHRONOMETER -> "T";
            case ITEMOMETER, STACKOMETER -> "";
            case FLUIDOMETER -> "L";
            case BUCKETOMETER -> "m3";
            case LIGHT_WEIGHTOMETER -> "g";
            case MEDIUM_WEIGHTOMETER -> "kg";
            case HEAVY_WEIGHTOMETER -> "t";
            case SUPER_HEAVY_WEIGHTOMETER -> "kt";
            case ELECTROMETER -> "EU";
            case TPS_METER -> "TPS";
            case PLAYER_COUNTER -> "P";
            case PROGRESS_METER -> "S";
            case TACHOMETER -> "RU";
            case GEIGER_COUNTER -> "n";
            case LASEROMETER -> "LU";
            case KILO_BUCKETOMETER -> "dam3";
        };
    }

    private static int color(SensorKind kind, SensorMode mode) {
        if (mode == SensorMode.FULL || mode == SensorMode.NOT_FULL) {
            return 0xFFC02020;
        }
        return switch (kind) {
            case THERMOMETER, ELECTROMETER, TPS_METER -> 0xFFFF4040;
            case GIBBLOMETER, KILO_GIBBLOMETER, LASEROMETER -> 0xFFFFFF40;
            case LUMINOMETER, LIGHT_WEIGHTOMETER, MEDIUM_WEIGHTOMETER,
                    HEAVY_WEIGHTOMETER, SUPER_HEAVY_WEIGHTOMETER ->
                    0xFFFFFFC0;
            case CHRONOMETER, GEIGER_COUNTER, TACHOMETER -> 0xFF40FF40;
            case FLUIDOMETER, BUCKETOMETER, KILO_BUCKETOMETER ->
                    0xFF4080FF;
            case PLAYER_COUNTER, PROGRESS_METER -> 0xFF80D8FF;
            case ITEMOMETER, STACKOMETER -> 0xFFFFFFFF;
        };
    }
}
