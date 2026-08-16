package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.machine.processing.MachineIdentityPolicy;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineState;

/** Resolves current persisted machine identities without compatibility tables. */
public final class ModMachineIdentities {
    public static MachineIdentityPolicy.Decision resolve(
            MachineVariant variant,
            ProcessingMachineState state) {
        if (state.identityQuarantine().isPresent()) {
            return new MachineIdentityPolicy.Decision(
                    MachineIdentityPolicy.Resolution.QUARANTINED,
                    identityOf(state),
                    state.identityQuarantine());
        }
        return MachineIdentityPolicy.resolve(
                identityOf(state), identityOf(variant));
    }

    public static MachineIdentityPolicy.Identity identityOf(
            ProcessingMachineState state) {
        return new MachineIdentityPolicy.Identity(
                state.machineKind(),
                state.tierBand(),
                state.materialId(),
                state.energyIdentity());
    }

    public static MachineIdentityPolicy.Identity identityOf(
            MachineVariant variant) {
        return new MachineIdentityPolicy.Identity(
                variant.kind().id().toString(),
                variant.tierBand().tierBandId().toString(),
                variant.tierBand().materialId(),
                variant.tierBand().energyType().name());
    }

    private ModMachineIdentities() {}
}
