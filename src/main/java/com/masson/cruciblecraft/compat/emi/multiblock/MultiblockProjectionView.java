package com.masson.cruciblecraft.compat.emi.multiblock;

import java.util.List;

/**
 * Orbit camera and layer filter kept on the recipe so a later
 * {@code addWidgets} call redraws the same view. EMI does not deliver drag
 * to a recipe widget, so the drag is applied while rendering.
 */
public final class MultiblockProjectionView {
    /** Front three-quarter view: {@link PreviewCamera} yaw 180 faces north. */
    public static final float DEFAULT_YAW = 145f;
    public static final float DEFAULT_PITCH = 30f;
    private static final float MIN_PITCH = -89f;
    private static final float MAX_PITCH = 89f;
    private static final float MIN_ZOOM = 0.35f;
    private static final float MAX_ZOOM = 6f;
    private static final float DEGREES_PER_PIXEL = 0.9f;

    private float yaw = DEFAULT_YAW;
    private float pitch = DEFAULT_PITCH;
    private float zoom = 1f;
    private int layerCursor;
    private boolean dragging;
    private int lastDragX;
    private int lastDragY;

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float zoom() {
        return zoom;
    }

    public boolean dragging() {
        return dragging;
    }

    /** {@code null} draws every local Y. */
    public Integer layer(List<Integer> layers) {
        if (layerCursor == 0 || layers.isEmpty()) {
            return null;
        }
        int index = Math.min(layerCursor, layers.size()) - 1;
        return layers.get(index);
    }

    public void zoomBy(float factor) {
        zoom = Math.clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
    }

    public void cycleLayer(int layerCount) {
        int choices = Math.max(layerCount, 0) + 1;
        layerCursor = (layerCursor + 1) % choices;
    }

    public void reset() {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
        zoom = 1f;
    }

    public void armDrag(int mouseX, int mouseY) {
        dragging = true;
        lastDragX = mouseX;
        lastDragY = mouseY;
    }

    public void drag(int mouseX, int mouseY, boolean buttonDown) {
        if (!dragging) {
            return;
        }
        if (!buttonDown) {
            dragging = false;
            return;
        }
        yaw += (mouseX - lastDragX) * DEGREES_PER_PIXEL;
        pitch = Math.clamp(
                pitch + (mouseY - lastDragY) * DEGREES_PER_PIXEL,
                MIN_PITCH,
                MAX_PITCH);
        lastDragX = mouseX;
        lastDragY = mouseY;
    }
}
