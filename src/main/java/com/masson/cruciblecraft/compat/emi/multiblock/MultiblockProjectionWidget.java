package com.masson.cruciblecraft.compat.emi.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/**
 * Perspective preview of one structure. Drag orbits the camera; the scroll
 * wheel is claimed by {@link PreviewScrollHandler} while the pointer is
 * inside this rectangle.
 */
public final class MultiblockProjectionWidget extends Widget {
    private static final int MAX_CANDIDATES = 8;
    private static final List<MultiblockProjectionWidget> LIVE = new ArrayList<>();

    private final MultiblockProjectionGrid grid;
    private final MultiblockProjectionView view;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    /** Skips redrawing the offscreen picture while the view is unchanged. */
    private int drawnStamp = Integer.MIN_VALUE;
    private boolean renderedThisFrame;

    public MultiblockProjectionWidget(
            MultiblockProjectionGrid grid,
            MultiblockProjectionView view,
            int x,
            int y,
            int width,
            int height) {
        this.grid = grid;
        this.view = view;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        LIVE.add(this);
    }

    /** The projection currently drawn under the pointer, if any. */
    static MultiblockProjectionWidget underPointer(int mouseX, int mouseY) {
        LIVE.removeIf(widget -> !widget.renderedThisFrame);
        for (MultiblockProjectionWidget widget : LIVE) {
            if (widget.getBounds().contains(mouseX, mouseY)) {
                return widget;
            }
        }
        return null;
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(x, y, width, height);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && getBounds().contains(mouseX, mouseY)) {
            view.armDrag(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderedThisFrame = true;
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean leftDown = GLFW.glfwGetMouseButton(
                window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        view.drag(mouseX, mouseY, leftDown);

        graphics.fill(x, y, x + width, y + height, 0xFF141414);
        PreviewScene scene = PreviewScene.of(grid, view.layer(grid.layers()));
        if (!scene.isEmpty()) {
            PreviewCamera camera = camera(scene);
            int stamp = viewStamp();
            if (stamp != drawnStamp) {
                int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
                PreviewRenderer.draw(
                        PreviewBlockGetter.of(scene),
                        camera,
                        null,
                        width * scale,
                        height * scale);
                drawnStamp = stamp;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, 150f);
            PreviewRenderer.blit(
                    graphics.pose().last().pose(), x, y, width, height);
            graphics.pose().popPose();
        }
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        PreviewScene scene = PreviewScene.of(grid, view.layer(grid.layers()));
        if (scene.isEmpty()) {
            return List.of();
        }
        PreviewCamera camera = PreviewCamera.orbit(
                scene, view.yaw(), view.pitch(), view.zoom(), width, height);
        return hovered(scene, camera, mouseX, mouseY)
                .flatMap(scene::at)
                .map(this::tooltip)
                .orElse(List.of());
    }

    void zoomBy(float factor) {
        view.zoomBy(factor);
    }

    private int viewStamp() {
        Integer layer = view.layer(grid.layers());
        return Float.floatToIntBits(view.yaw())
                * 31 + Float.floatToIntBits(view.pitch())
                * 31 + Float.floatToIntBits(view.zoom())
                * 31 + (layer == null ? 0 : layer);
    }

    private PreviewCamera camera(PreviewScene scene) {
        return PreviewCamera.orbit(
                scene, view.yaw(), view.pitch(), view.zoom(), width, height);
    }

    private Optional<BlockPos> hovered(
            PreviewScene scene, PreviewCamera camera, int mouseX, int mouseY) {
        if (!getBounds().contains(mouseX, mouseY)) {
            return Optional.empty();
        }
        return camera.pick(scene, mouseX - x, mouseY - y, width, height);
    }

    private List<ClientTooltipComponent> tooltip(MultiblockProjectionGrid.Cell cell) {
        List<ClientTooltipComponent> lines = new ArrayList<>();
        lines.add(line(blockName(cell.block())));
        if (!cell.description().isBlank()) {
            lines.add(line(Component.literal(cell.description())));
        }
        List<Block> candidates = candidates(cell.paletteKey());
        if (candidates.size() > 1) {
            lines.add(line(Component.translatable(
                    "emi.cruciblecraft.multiblock.candidates", candidates.size())));
            int shown = Math.min(candidates.size(), MAX_CANDIDATES);
            for (int i = 0; i < shown; i++) {
                lines.add(line(Component.literal("· ").append(candidates.get(i).getName())));
            }
            if (candidates.size() > shown) {
                lines.add(line(Component.translatable(
                        "emi.cruciblecraft.multiblock.candidates_more",
                        candidates.size() - shown)));
            }
        }
        return lines;
    }

    private List<Block> candidates(String paletteKey) {
        return grid.materials().stream()
                .filter(material -> material.paletteKey().equals(paletteKey))
                .findFirst()
                .map(MultiblockProjectionGrid.Material::itemBlocks)
                .orElse(List.of())
                .stream()
                .map(BuiltInRegistries.BLOCK::getOptional)
                .flatMap(Optional::stream)
                .toList();
    }

    private static Component blockName(net.minecraft.resources.ResourceLocation id) {
        return BuiltInRegistries.BLOCK.getOptional(id)
                .<Component>map(Block::getName)
                .orElseGet(() -> Component.literal(id.toString()));
    }

    private static ClientTooltipComponent line(Component text) {
        return ClientTooltipComponent.create(text.getVisualOrderText());
    }
}
