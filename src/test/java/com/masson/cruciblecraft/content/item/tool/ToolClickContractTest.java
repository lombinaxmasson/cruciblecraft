package com.masson.cruciblecraft.content.item.tool;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class ToolClickContractTest {
    private static final List<String> BLOCKS = List.of(
            "src/main/java/com/masson/cruciblecraft/content/block/AbstractPipeBlock.java",
            "src/main/java/com/masson/cruciblecraft/content/block/CableBlock.java",
            "src/main/java/com/masson/cruciblecraft/content/block/HopperBlock.java",
            "src/main/java/com/masson/cruciblecraft/content/block/DustFunnelBlock.java",
            "src/main/java/com/masson/cruciblecraft/content/block/MixingBowlBlock.java",
            "src/main/java/com/masson/cruciblecraft/content/block/GtStoneBlock.java",
            "src/main/java/com/masson/cruciblecraft/content/block/GtBlockObjectBlock.java",
            "src/main/java/com/masson/cruciblecraft/content/block/ElectricEngineBlock.java",
            "src/main/java/com/masson/cruciblecraft/content/block/MassStorageBlock.java",
            "src/main/java/com/masson/cruciblecraft/content/block/Gt6StyleConnections.java",
            "src/main/java/com/masson/cruciblecraft/energy/transformer/TransformerBlock.java");
    private static final List<String> FORBIDDEN = List.of(
            "instanceof MaterialWrenchItem",
            "instanceof MaterialWireCutterItem",
            "instanceof MaterialScrewdriverItem",
            "instanceof MaterialMonkeyWrenchItem",
            "instanceof MaterialCrowbarItem",
            "instanceof MaterialPlungerItem",
            "instanceof MaterialSoftHammerItem",
            "instanceof MaterialUniversalSpadeItem",
            "MachineToolInteractions");

    @Test
    void machineClicksDoNotBranchOnConcreteToolClasses() throws Exception {
        assertFalse(Files.exists(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/item/MachineToolInteractions.java")));
        for (String file : BLOCKS) {
            String source = Files.readString(Path.of(file));
            for (String forbidden : FORBIDDEN) {
                assertFalse(
                        source.contains(forbidden),
                        file + " still dispatches on " + forbidden);
            }
        }
    }

    @Test
    void answeringBlocksImplementTheTypedContract() throws Exception {
        for (String file : BLOCKS) {
            if (file.endsWith("Gt6StyleConnections.java")) {
                continue;
            }
            String source = Files.readString(Path.of(file));
            assertTrue(
                    source.contains("implements")
                            && source.contains("ToolInteractable"),
                    file + " is not a ToolInteractable");
            assertTrue(
                    source.contains("useTool("),
                    file + " has no useTool");
        }
    }
}
