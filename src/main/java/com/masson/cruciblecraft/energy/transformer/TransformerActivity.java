package com.masson.cruciblecraft.energy.transformer;

/**
 * GT6 {@code TE_Behavior_Active_Trinary}: shift a 64-tick history and
 * expose overlay state 0 / 1 / 2.
 */
public final class TransformerActivity {
    private long data;
    private byte state;
    private boolean activeThisTick;

    public void setActiveThisTick(boolean active) {
        this.activeThisTick = active;
    }

    public byte state() {
        return state;
    }

    public void restore(boolean active, long data, int state) {
        this.activeThisTick = active;
        this.data = data;
        this.state = (byte) Math.max(0, Math.min(2, state));
    }

    public boolean active() {
        return activeThisTick;
    }

    public long data() {
        return data;
    }

    /**
     * @return true when the overlay state changed
     */
    public boolean check() {
        data <<= 1;
        if (activeThisTick) {
            data |= 1L;
        }
        byte previous = state;
        if (data == 0L) {
            state = 0;
        } else if (data == ~0L) {
            state = 1;
        } else {
            state = 2;
        }
        return previous != state;
    }
}
