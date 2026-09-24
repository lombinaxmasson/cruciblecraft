package com.masson.cruciblecraft.content.mte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MteFluidAttachmentCatalogTest {
    @Test
    void allGt6FluidAttachmentProfilesArePresent() {
        long attachmentCount = MteInPlaceCatalog.specs().stream()
                .filter(spec -> spec.kind().attachment())
                .count();
        assertEquals(46, attachmentCount);
        assertEquals(24, MteFluidAttachmentProfile.all().size());
        assertEquals(
                6,
                MteFluidAttachmentProfile.all().stream()
                        .filter(profile -> profile.kind() == MteInPlaceKind.TAP)
                        .count());
        assertEquals(
                6,
                MteFluidAttachmentProfile.all().stream()
                        .filter(profile -> profile.kind() == MteInPlaceKind.FUNNEL)
                        .count());
        assertEquals(
                6,
                MteFluidAttachmentProfile.all().stream()
                        .filter(profile -> profile.kind() == MteInPlaceKind.NOZZLE)
                        .count());
        assertEquals(
                6,
                MteFluidAttachmentProfile.all().stream()
                        .filter(profile -> profile.kind() == MteInPlaceKind.CAP_NOZZLE)
                        .count());
    }

    @Test
    void phaseAndProofPropertiesMatchGt6MaterialRows() {
        MteFluidAttachmentProfile ceramicTap =
                MteFluidAttachmentProfile.require(
                        MteInPlaceCatalog.require(
                                net.minecraft.resources.ResourceLocation.parse(
                                        "cruciblecraft:fluid_attachment/ceramic_tap")));
        MteFluidAttachmentProfile tungstenNozzle =
                MteFluidAttachmentProfile.require(
                        MteInPlaceCatalog.require(
                                net.minecraft.resources.ResourceLocation.parse(
                                        "cruciblecraft:tungsten/nozzle")));
        assertEquals(
                MteFluidAttachmentProfile.Phase.LIQUID,
                ceramicTap.phase());
        assertTrue(!ceramicTap.acidProof());
        assertEquals(
                MteFluidAttachmentProfile.Phase.GAS,
                tungstenNozzle.phase());
        assertTrue(tungstenNozzle.acidProof());
        assertTrue(tungstenNozzle.magicProof());
    }
}
