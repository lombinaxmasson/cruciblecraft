package com.masson.cruciblecraft.machine.processing;

import java.util.Objects;

import com.masson.cruciblecraft.machine.component.RecipeProcessor;

/** Pure pause/resume/reset/progress state machine shared by processing hosts. */
public final class ProcessingRuntime {
    public enum Result {
        IDLE,
        INVALID_RECIPE,
        OUTPUT_BLOCKED,
        UNDERPOWERED,
        ADVANCED,
        COMPLETE
    }

    private final RecipeProcessor processor;
    private String status = "idle";

    public ProcessingRuntime() {
        this(new RecipeProcessor());
    }

    public ProcessingRuntime(RecipeProcessor processor) {
        this.processor = Objects.requireNonNull(processor, "processor");
    }

    public Result tick(
            String recipeId,
            int duration,
            boolean valid,
            boolean outputCapacity,
            boolean powered,
            ProcessingMachineSpec.BufferPolicy buffering) {
        Objects.requireNonNull(buffering, "buffering");
        if (recipeId == null || recipeId.isBlank()) {
            processor.reset();
            status = "idle";
            return Result.IDLE;
        }
        processor.select(recipeId, duration);
        if (!valid) {
            status = "invalid_recipe";
            return Result.INVALID_RECIPE;
        }
        if (!outputCapacity) {
            if (buffering == ProcessingMachineSpec.BufferPolicy.RESET_ON_BLOCK) {
                processor.reset();
            }
            status = "output_blocked";
            return Result.OUTPUT_BLOCKED;
        }
        if (!powered) {
            status = "underpowered";
            return Result.UNDERPOWERED;
        }
        status = "";
        processor.advance();
        return processor.complete() ? Result.COMPLETE : Result.ADVANCED;
    }

    public Result tickWork(
            String recipeId,
            int displayedDuration,
            boolean valid,
            boolean outputCapacity,
            long completedWork,
            long requiredWork,
            ProcessingMachineSpec.BufferPolicy buffering) {
        Objects.requireNonNull(buffering, "buffering");
        if (recipeId == null || recipeId.isBlank()) {
            processor.reset();
            status = "idle";
            return Result.IDLE;
        }
        processor.select(recipeId, displayedDuration);
        if (!valid) {
            status = "invalid_recipe";
            return Result.INVALID_RECIPE;
        }
        if (!outputCapacity) {
            if (buffering
                    == ProcessingMachineSpec.BufferPolicy.RESET_ON_BLOCK) {
                processor.reset();
            }
            status = "output_blocked";
            return Result.OUTPUT_BLOCKED;
        }
        if (requiredWork <= 0L
                || completedWork < 0L
                || completedWork > requiredWork) {
            throw new IllegalArgumentException(
                    "Invalid machine work progress");
        }
        status = "";
        int displayedProgress = completedWork >= requiredWork
                ? displayedDuration
                : (int) Math.min(
                        displayedDuration - 1L,
                        Math.multiplyExact(
                                        completedWork,
                                        displayedDuration)
                                / requiredWork);
        processor.setProgress(displayedProgress);
        return completedWork >= requiredWork
                ? Result.COMPLETE
                : Result.ADVANCED;
    }

    public void completed(int displayedDuration) {
        processor.clearActive(displayedDuration);
        status = "idle";
    }

    public void recipePowerExceeded(
            String recipeId, int duration) {
        processor.select(recipeId, duration);
        status = "recipe_power_exceeded";
    }

    public void overcharged() {
        status = "overcharged";
    }

    public boolean reset() {
        boolean statusChanged = !"idle".equals(status);
        status = "idle";
        return processor.reset() || statusChanged;
    }

    public void restore(String id, int progress, int duration, String savedStatus) {
        processor.restore(id, progress, duration);
        status = savedStatus == null ? "idle" : savedStatus;
    }

    public RecipeProcessor processor() {
        return processor;
    }

    public String status() {
        return status;
    }

    public boolean active() {
        return !processor.activeId().isEmpty();
    }
}
