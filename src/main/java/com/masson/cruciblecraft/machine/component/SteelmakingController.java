package com.masson.cruciblecraft.machine.component;

import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.air.AirOutputModel;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.recipe.SteelmakingProcess;
import com.masson.cruciblecraft.recipe.SteelmakingProcess.Batch;
import com.masson.cruciblecraft.recipe.SteelmakingTickDecisions;
import com.masson.cruciblecraft.recipe.SteelmakingTickDecisions.Action;

/** Crucible steelmaking state and transitions, independent of world and networking. */
public final class SteelmakingController {
    private long storedAir;
    private int batchIronUnits;
    private int reactionTicks;

    public InjectionResult previewInjection(
            Map<String, Integer> composition,
            float temperature) {
        return acceptance(composition, temperature).result();
    }

    public InjectionResult insertAir(
            long air,
            Map<String, Integer> composition,
            float temperature) {
        Acceptance acceptance = acceptance(composition, temperature);
        Batch batch = acceptance.batch().orElse(null);
        if (batch == null) {
            return acceptance.result();
        }
        batchIronUnits = batch.ironUnits();
        storedAir = AirOutputModel.addToBuffer(storedAir, air);
        return acceptance.result();
    }

    public TickResult tick(CompositionTank contents, float temperature) {
        SteelmakingTickDecisions.Decision decision = SteelmakingTickDecisions.evaluate(
                contents.composition(),
                batchIronUnits,
                storedAir);
        if (decision.action() == Action.RESET_STALE) {
            return new TickResult(reset());
        }
        if (decision.action() != Action.PROCESS) {
            return TickResult.NONE;
        }
        Batch batch = decision.batch().orElse(null);
        if (batch == null) {
            return TickResult.NONE;
        }

        storedAir = AirOutputModel.consumeProcessingTick(storedAir);
        if (temperature
                < MaterialCatalog.require(SteelmakingProcess.IRON).thermal().meltingPoint()) {
            return TickResult.NONE;
        }
        reactionTicks++;
        if (reactionTicks < SteelmakingProcess.REACTION_INTERVAL_TICKS) {
            return TickResult.NONE;
        }
        reactionTicks = 0;

        int carbon = contents.units(SteelmakingProcess.CARBON);
        var carbonStep = SteelmakingProcess.consumeCarbon(batch, carbon);
        if (carbonStep.steelRangeReached()) {
            contents.setUnits(SteelmakingProcess.IRON, 0);
            contents.setUnits(SteelmakingProcess.CARBON, 0);
            contents.merge(SteelmakingProcess.STEEL, batch.ironUnits());
            reset();
        } else {
            contents.setUnits(SteelmakingProcess.CARBON, carbonStep.remainingCarbonUnits());
        }
        return TickResult.IMMEDIATE_MUTATION;
    }

    public boolean reset() {
        boolean changed = storedAir != 0L || batchIronUnits != 0 || reactionTicks != 0;
        storedAir = 0L;
        batchIronUnits = 0;
        reactionTicks = 0;
        return changed;
    }

    public boolean onCompositionChanged() {
        return reset();
    }

    public void restore(long savedAir, int savedBatchIronUnits, int savedReactionTicks) {
        storedAir = AirOutputModel.clampStoredAir(savedAir);
        batchIronUnits = SteelmakingProcess.resume(savedBatchIronUnits).isPresent()
                ? savedBatchIronUnits
                : 0;
        reactionTicks = Math.max(
                0,
                Math.min(
                        SteelmakingProcess.REACTION_INTERVAL_TICKS - 1,
                        savedReactionTicks));
    }

    public Optional<Batch> currentBatch(Map<String, Integer> composition) {
        Optional<Batch> batch = batchIronUnits > 0
                ? SteelmakingProcess.resume(batchIronUnits)
                : SteelmakingProcess.begin(composition);
        return batch.filter(candidate -> SteelmakingTickDecisions.matches(composition, candidate));
    }

    public boolean active() {
        return batchIronUnits > 0;
    }

    public boolean hasState() {
        return storedAir != 0L || batchIronUnits != 0 || reactionTicks != 0;
    }

    public boolean blocksFluidTransfer() {
        return hasState();
    }

    public long storedAir() {
        return storedAir;
    }

    public int batchIronUnits() {
        return batchIronUnits;
    }

    public int reactionTicks() {
        return reactionTicks;
    }

    private Acceptance acceptance(Map<String, Integer> composition, float temperature) {
        if (composition.keySet().stream().anyMatch(id -> !MaterialCatalog.contains(id))) {
            return Acceptance.rejected(InjectionResult.INVALID_CHARGE);
        }
        if (temperature
                < MaterialCatalog.require(SteelmakingProcess.IRON).thermal().meltingPoint()) {
            return Acceptance.rejected(InjectionResult.TOO_COLD);
        }
        boolean continuing = batchIronUnits > 0;
        Optional<Batch> batch = continuing
                ? SteelmakingProcess.resume(batchIronUnits)
                : SteelmakingProcess.begin(composition);
        Batch candidate = batch.orElse(null);
        if (candidate == null
                || !SteelmakingTickDecisions.matches(composition, candidate)) {
            return Acceptance.rejected(InjectionResult.INVALID_CHARGE);
        }
        return new Acceptance(
                Optional.of(candidate),
                continuing ? InjectionResult.CONTINUED : InjectionResult.STARTED);
    }

    public enum InjectionResult {
        STARTED,
        CONTINUED,
        TOO_COLD,
        INVALID_CHARGE
    }

    public record TickResult(boolean immediateMutation) {
        private static final TickResult NONE = new TickResult(false);
        private static final TickResult IMMEDIATE_MUTATION = new TickResult(true);
    }

    private record Acceptance(Optional<Batch> batch, InjectionResult result) {
        private static Acceptance rejected(InjectionResult result) {
            return new Acceptance(Optional.empty(), result);
        }
    }
}
