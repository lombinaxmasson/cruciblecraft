package com.masson.cruciblecraft.client.multiblockpreview;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.compat.emi.multiblock.MultiblockProjectionGrid;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Holds the one active in-world structure overlay and draws it. */
@EventBusSubscriber(modid = CrucibleCraft.MODID, value = Dist.CLIENT)
public final class WorldPreviewRenderer {
    private static WorldStructurePreview active;

    private WorldPreviewRenderer() {}

    /**
     * Toggles the overlay on the controller under the crosshair.
     * Reports why nothing happened when there is no controller there.
     */
    public static void toggle(MultiblockProjectionGrid grid) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        if (active != null && active.grid() == grid) {
            active = null;
            return;
        }
        BlockPos controller = WorldStructurePreview.aimedController(level, grid);
        if (controller == null) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.translatable(
                                "emi.cruciblecraft.multiblock.preview_needs_controller"),
                        true);
            }
            return;
        }
        BlockState state = level.getBlockState(controller);
        Direction facing = ControllerFacing.of(state);
        active = new WorldStructurePreview(
                grid,
                controller,
                facing,
                level.getGameTime() + WorldStructurePreview.DURATION_TICKS);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (active == null
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (active.expired(minecraft.level.getGameTime())) {
            active = null;
            return;
        }
        var camera = event.getCamera();
        active.render(
                minecraft.level,
                camera.getPosition(),
                event.getPoseStack(),
                event.getModelViewMatrix(),
                minecraft.renderBuffers().bufferSource());
    }
}
