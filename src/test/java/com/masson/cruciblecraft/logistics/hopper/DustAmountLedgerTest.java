package com.masson.cruciblecraft.logistics.hopper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.logistics.hopper.DustAmountLedger.Form;

class DustAmountLedgerTest {
    @Test
    void nineTinyBecomeOneDust() {
        DustAmountLedger ledger = new DustAmountLedger();
        assertTrue(ledger.accept("cruciblecraft:iron", Form.TINY_DUST, 9));
        assertEquals(36, ledger.units());
        assertEquals(1, ledger.outputItemCount());
        assertTrue(ledger.tryEmit(1));
        assertTrue(ledger.isEmpty());
    }

    @Test
    void fourSmallBecomeOneDust() {
        DustAmountLedger ledger = new DustAmountLedger();
        assertTrue(ledger.accept("cruciblecraft:iron", Form.SMALL_DUST, 4));
        ledger.setOutputMode(Form.DUST);
        assertTrue(ledger.tryEmit(1));
        assertTrue(ledger.isEmpty());
    }

    @Test
    void oneDustBecomesFourSmallOrNineTiny() {
        DustAmountLedger small = new DustAmountLedger();
        assertTrue(small.accept("cruciblecraft:iron", Form.DUST, 1));
        small.setOutputMode(Form.SMALL_DUST);
        assertEquals(4, small.outputItemCount());
        assertTrue(small.tryEmit(4));
        assertTrue(small.isEmpty());

        DustAmountLedger tiny = new DustAmountLedger();
        assertTrue(tiny.accept("cruciblecraft:iron", Form.DUST, 1));
        tiny.setOutputMode(Form.TINY_DUST);
        assertEquals(9, tiny.outputItemCount());
        assertTrue(tiny.tryEmit(9));
        assertTrue(tiny.isEmpty());
    }

    @Test
    void mixedSizeOfOneMaterialIsConserved() {
        DustAmountLedger ledger = new DustAmountLedger();
        assertTrue(ledger.accept("cruciblecraft:iron", Form.SMALL_DUST, 2));
        assertTrue(ledger.accept("cruciblecraft:iron", Form.TINY_DUST, 4));
        assertTrue(ledger.accept("cruciblecraft:iron", Form.DUST, 1));
        assertEquals(18 + 16 + 36, ledger.units());
        assertEquals(
                ledger.units(),
                ledger.decompose().reconstructedUnits());
    }

    @Test
    void mixedMaterialIsRejectedAndNotConsumed() {
        DustAmountLedger ledger = new DustAmountLedger();
        assertTrue(ledger.accept("cruciblecraft:iron", Form.DUST, 1));
        assertFalse(ledger.accept("cruciblecraft:copper", Form.TINY_DUST, 9));
        assertEquals("cruciblecraft:iron", ledger.materialId());
        assertEquals(36, ledger.units());
    }

    @Test
    void saveLoadDoesNotDrift() {
        DustAmountLedger ledger = new DustAmountLedger();
        ledger.accept("cruciblecraft:iron", Form.TINY_DUST, 3);
        ledger.setOutputMode(Form.SMALL_DUST);
        DustAmountLedger.Snapshot first = ledger.snapshot();
        DustAmountLedger restored = DustAmountLedger.load(first);
        assertEquals(first, restored.snapshot());
        restored.accept("cruciblecraft:iron", Form.TINY_DUST, 1);
        DustAmountLedger.Snapshot second = restored.snapshot();
        assertEquals(second, DustAmountLedger.load(second).snapshot());
        assertEquals(16, restored.units());
    }

    @Test
    void blockedOutputDoesNotDeduct() {
        DustAmountLedger ledger = new DustAmountLedger();
        ledger.accept("cruciblecraft:iron", Form.DUST, 1);
        ledger.setOutputMode(Form.SMALL_DUST);
        assertFalse(ledger.tryEmit(3));
        assertEquals(36, ledger.units());
        assertTrue(ledger.tryEmit(4));
        assertTrue(ledger.isEmpty());
    }

    @Test
    void modeSwitchDoesNotChangeUnits() {
        DustAmountLedger ledger = new DustAmountLedger();
        ledger.accept("cruciblecraft:iron", Form.TINY_DUST, 5);
        ledger.setOutputMode(Form.TINY_DUST);
        int units = ledger.units();
        ledger.cycleOutputMode(false);
        ledger.cycleOutputMode(true);
        assertEquals(units, ledger.units());
        assertEquals(Form.TINY_DUST, ledger.outputMode());
    }

    @Test
    void unknownSchemaIsFailClosed() {
        DustAmountLedger.Snapshot future = new DustAmountLedger.Snapshot(
                99, "cruciblecraft:iron", 4, Form.DUST);
        assertThrows(
                IllegalArgumentException.class,
                () -> DustAmountLedger.load(future));
    }

    @Test
    void remainderBelowBatchStaysAndDecomposesExactlyForLegalInputs() {
        DustAmountLedger ledger = new DustAmountLedger();
        ledger.accept("cruciblecraft:iron", Form.TINY_DUST, 8);
        assertEquals(32, ledger.units());
        assertEquals(0, ledger.outputItemCount());
        DustAmountLedger.Decomposition split = ledger.decompose();
        assertEquals(0, split.dust());
        assertEquals(0, split.smallDust());
        assertEquals(8, split.tinyDust());
        assertEquals(0, split.leftoverUnits());
        assertTrue(split.lossless());
    }
}
