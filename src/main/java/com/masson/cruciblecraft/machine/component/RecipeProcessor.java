package com.masson.cruciblecraft.machine.component;

/** Recipe identity and progress state without inventory, energy, or RecipeManager policy. */
public final class RecipeProcessor {
    private String activeId = "";
    private int progress;
    private int duration;

    public boolean select(String recipeId, int recipeDuration) {
        if (recipeId == null || recipeId.isBlank()) {
            throw new IllegalArgumentException("Recipe id must not be blank");
        }
        if (recipeDuration <= 0) {
            throw new IllegalArgumentException("Recipe duration must be positive");
        }
        boolean changed = !recipeId.equals(activeId);
        if (changed) {
            activeId = recipeId;
            progress = 0;
        }
        duration = recipeDuration;
        return changed;
    }

    public boolean advance() {
        return advance(1);
    }

    public boolean advance(int amount) {
        if (duration <= 0 || activeId.isEmpty()) {
            return false;
        }
        if (amount <= 0) {
            return false;
        }
        int previous = progress;
        progress = Math.min(duration, progress + amount);
        return progress != previous;
    }

    public boolean complete() {
        return duration > 0 && progress >= duration;
    }

    public boolean reset() {
        return restore("", 0, 0);
    }

    public boolean clearActive(int displayedDuration) {
        return restore("", 0, displayedDuration);
    }

    public boolean restore(String recipeId, int savedProgress, int savedDuration) {
        String safeId = recipeId == null ? "" : recipeId;
        int safeDuration = Math.max(0, savedDuration);
        int safeProgress = Math.max(0, Math.min(savedProgress, safeDuration));
        boolean changed = !activeId.equals(safeId)
                || progress != safeProgress
                || duration != safeDuration;
        activeId = safeId;
        progress = safeProgress;
        duration = safeDuration;
        return changed;
    }

    public void setProgress(int value) {
        progress = Math.max(0, duration > 0 ? Math.min(value, duration) : value);
    }

    public void setDuration(int value) {
        duration = Math.max(0, value);
        progress = Math.min(progress, duration);
    }

    public String activeId() {
        return activeId;
    }

    public int progress() {
        return progress;
    }

    public int duration() {
        return duration;
    }
}
