package com.masson.cruciblecraft.logistics.hopper;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Integer 36-unit dust ledger for the steel Dust Funnel.
 *
 * <p>dust=36, small=9, tiny=4. Mixed sizes of one material are allowed.
 * Output emits only a complete 36-unit batch as 1 dust / 4 small / 9 tiny.
 */
public final class DustAmountLedger {
    public static final int SCHEMA_VERSION = 1;
    public static final int DUST_UNITS = 36;
    public static final int SMALL_UNITS = 9;
    public static final int TINY_UNITS = 4;

    public enum Form {
        DUST(DUST_UNITS, 1),
        SMALL_DUST(SMALL_UNITS, 4),
        TINY_DUST(TINY_UNITS, 9);

        private final int units;
        private final int itemsPerBatch;

        Form(int units, int itemsPerBatch) {
            this.units = units;
            this.itemsPerBatch = itemsPerBatch;
        }

        public int units() {
            return units;
        }

        public int itemsPerBatch() {
            return itemsPerBatch;
        }
    }

    private String materialId = "";
    private int units;
    private Form outputMode = Form.DUST;

    public String materialId() {
        return materialId;
    }

    public int units() {
        return units;
    }

    public Form outputMode() {
        return outputMode;
    }

    public boolean isEmpty() {
        return units <= 0 || materialId.isEmpty();
    }

    public void setOutputMode(Form mode) {
        this.outputMode = Objects.requireNonNull(mode, "mode");
    }

    public Form cycleOutputMode(boolean reverse) {
        Form[] values = Form.values();
        int next = (outputMode.ordinal() + (reverse ? values.length - 1 : 1))
                % values.length;
        outputMode = values[next];
        return outputMode;
    }

    public boolean accept(String material, Form form, int count) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(form, "form");
        if (material.isEmpty() || count <= 0) {
            return false;
        }
        if (!materialId.isEmpty() && !materialId.equals(material)) {
            return false;
        }
        long added = (long) form.units() * count;
        long next = (long) units + added;
        if (next > Integer.MAX_VALUE) {
            return false;
        }
        materialId = material;
        units = (int) next;
        return true;
    }

    public boolean hasOutputBatch() {
        return !isEmpty() && units >= DUST_UNITS;
    }

    public int outputItemCount() {
        return hasOutputBatch() ? outputMode.itemsPerBatch() : 0;
    }

    /**
     * Emit one 36-unit batch only when {@code destSpace} can take the full
     * 1/4/9 items. Blocked output leaves the ledger unchanged.
     */
    public boolean tryEmit(int destSpace) {
        int items = outputItemCount();
        if (items <= 0 || destSpace < items) {
            return false;
        }
        units -= DUST_UNITS;
        if (units == 0) {
            materialId = "";
        }
        return true;
    }

    public Decomposition decompose() {
        int remaining = Math.max(0, units);
        int dust = remaining / DUST_UNITS;
        remaining -= dust * DUST_UNITS;
        int small = 0;
        int tiny = 0;
        boolean exact = false;
        for (int candidate = remaining / SMALL_UNITS; candidate >= 0; candidate--) {
            int leftover = remaining - candidate * SMALL_UNITS;
            if (leftover % TINY_UNITS == 0) {
                small = candidate;
                tiny = leftover / TINY_UNITS;
                remaining = 0;
                exact = true;
                break;
            }
        }
        if (!exact) {
            small = remaining / SMALL_UNITS;
            remaining -= small * SMALL_UNITS;
            tiny = remaining / TINY_UNITS;
            remaining -= tiny * TINY_UNITS;
        }
        return new Decomposition(materialId, dust, small, tiny, remaining);
    }

    public Snapshot snapshot() {
        return new Snapshot(SCHEMA_VERSION, materialId, units, outputMode);
    }

    public void restore(Snapshot snapshot) {
        DustAmountLedger loaded = load(snapshot);
        materialId = loaded.materialId;
        units = loaded.units;
        outputMode = loaded.outputMode;
    }

    public static DustAmountLedger load(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.schemaVersion() != SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unknown dust ledger schema " + snapshot.schemaVersion());
        }
        if (snapshot.units() < 0) {
            throw new IllegalArgumentException("Dust ledger units must be >= 0");
        }
        DustAmountLedger ledger = new DustAmountLedger();
        ledger.materialId = snapshot.materialId() == null
                ? ""
                : snapshot.materialId();
        ledger.units = snapshot.units();
        ledger.outputMode = snapshot.outputMode() == null
                ? Form.DUST
                : snapshot.outputMode();
        if (ledger.units == 0) {
            ledger.materialId = "";
        }
        return ledger;
    }

    public record Snapshot(
            int schemaVersion,
            String materialId,
            int units,
            Form outputMode) {
        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.INT.fieldOf("schema_version")
                                .forGetter(Snapshot::schemaVersion),
                        Codec.STRING.optionalFieldOf("material_id", "")
                                .forGetter(Snapshot::materialId),
                        Codec.INT.fieldOf("units").forGetter(Snapshot::units),
                        Codec.STRING.xmap(Form::valueOf, Form::name)
                                .fieldOf("output_mode")
                                .forGetter(Snapshot::outputMode))
                        .apply(instance, Snapshot::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot>
                STREAM_CODEC = StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        Snapshot::schemaVersion,
                        ByteBufCodecs.STRING_UTF8,
                        Snapshot::materialId,
                        ByteBufCodecs.VAR_INT,
                        Snapshot::units,
                        ByteBufCodecs.STRING_UTF8,
                        snapshot -> snapshot.outputMode().name(),
                        (schema, material, amount, mode) -> new Snapshot(
                                schema,
                                material,
                                amount,
                                Form.valueOf(mode)));

        public Snapshot {
            materialId = materialId == null ? "" : materialId;
            outputMode = outputMode == null ? Form.DUST : outputMode;
        }
    }

    public record Decomposition(
            String materialId,
            int dust,
            int smallDust,
            int tinyDust,
            int leftoverUnits) {
        public boolean lossless() {
            return leftoverUnits == 0;
        }

        public int reconstructedUnits() {
            return dust * DUST_UNITS
                    + smallDust * SMALL_UNITS
                    + tinyDust * TINY_UNITS
                    + leftoverUnits;
        }
    }
}
