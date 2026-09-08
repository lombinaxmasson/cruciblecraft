package com.masson.cruciblecraft.api.tool;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;

/**
 * Outcome of one tool action on one target.
 *
 * <p>{@link #PASS} lets the dispatcher try the next action or a vanilla
 * adapter. {@link #REJECT} means this target owns the action but did no
 * work — adapters must not run for that action (empty fluid pipes must
 * not fall through to a 1000 mB capability drain). {@link #SUCCESS}
 * consumes the click.
 */
public enum ToolResult {
    PASS,
    REJECT,
    SUCCESS;

    public boolean consumesClick() {
        return this == SUCCESS;
    }

    public InteractionResult toInteractionResult(boolean clientSide) {
        return this == SUCCESS
                ? InteractionResult.sidedSuccess(clientSide)
                : InteractionResult.PASS;
    }

    public ItemInteractionResult toItemResult(boolean clientSide) {
        return this == SUCCESS
                ? ItemInteractionResult.sidedSuccess(clientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
