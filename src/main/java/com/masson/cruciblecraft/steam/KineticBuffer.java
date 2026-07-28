package com.masson.cruciblecraft.steam;

/** Bounded, rate-limited integer KU storage with side-effect-free simulation. */
public final class KineticBuffer {
    private final long capacity;
    private final long outputRate;
    private long stored;
    private int strokeSign = 1;
    public KineticBuffer(long capacity, long outputRate) {
        if (capacity <= 0 || outputRate <= 0) throw new IllegalArgumentException("Positive limits required");
        this.capacity = capacity; this.outputRate = outputRate;
    }
    public KineticBuffer(long capacity, long outputRate, long stored) {
        this(capacity, outputRate); this.stored = Math.max(0, Math.min(capacity, stored));
    }
    public KineticBuffer(long capacity, long outputRate, long stored, int strokeSign) {
        this(capacity, outputRate, stored);
        this.strokeSign = strokeSign < 0 ? -1 : 1;
    }
    public long insert(long amount) {
        long accepted = Math.max(0, Math.min(amount, capacity - stored)); stored += accepted; return accepted;
    }
    public long extract(long amount, boolean simulate) {
        long extracted = Math.max(0, Math.min(Math.min(amount, outputRate), stored));
        if (!simulate && extracted > 0) {
            stored -= extracted;
            strokeSign = -strokeSign;
        }
        return extracted;
    }
    public long stored() { return stored; }
    public long room() { return capacity - stored; }
    public int strokeSign() { return strokeSign; }
}
