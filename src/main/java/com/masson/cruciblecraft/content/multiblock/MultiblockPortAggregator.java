package com.masson.cruciblecraft.content.multiblock;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Maintains controller-to-port bindings only for loaded positions from the
 * latest validation.
 *
 * <p>GT6 keeps matching parts claimed while another cell is missing so a
 * builder-wand click on that part can reach the controller. Capabilities
 * remain gated by the controller's {@code structureValid()} state.</p>
 */
public final class MultiblockPortAggregator {
    private MultiblockPortAggregator() {}

    public static Set<BlockPos> refresh(
            Level level,
            BlockPos controller,
            ResourceLocation structureId,
            MultiblockStructureValidator.ValidationResult validation,
            Set<BlockPos> previous) {
        LinkedHashSet<BlockPos> desired = new LinkedHashSet<>();
        validation.ports().forEach(
                matched -> desired.add(matched.position().immutable()));
        unbindLoaded(
                level,
                controller,
                previous.stream()
                        .filter(position -> !desired.contains(position))
                        .collect(java.util.stream.Collectors.toSet()));

        LinkedHashSet<BlockPos> bound = new LinkedHashSet<>();
        MultiblockPortHost host = level.getBlockEntity(controller)
                instanceof MultiblockControllerBinding binding
                ? binding.portHost()
                : null;
        Map<BlockPos, PortStore.Assignment> assignments = host == null
                ? Map.of()
                : PortStoreRegistry.assignments(validation.ports(), host);
        for (MultiblockStructureValidator.MatchedPort matched
                : validation.ports()) {
            BlockPos position = matched.position();
            if (!level.hasChunkAt(position)) {
                continue;
            }
            if (level.getBlockEntity(position) instanceof MultiblockPort port
                    && port.accepts(matched.type())
                    && (port.controllerPosition().isEmpty()
                            || port.controllerPosition()
                                    .orElseThrow()
                                    .equals(controller))) {
                port.bind(controller, structureId, matched.type());
                if (host != null
                        && port instanceof PortStoreCarrier carrier) {
                    PortStore.Assignment assignment = assignments.get(
                            position.immutable());
                    if (assignment != null) {
                        carrier.configurePortStore(host, assignment);
                        PortStoreRegistry.bind(host, position, carrier);
                    }
                }
                bound.add(position.immutable());
            }
        }
        return Set.copyOf(bound);
    }

    public static void unbindLoaded(
            Level level,
            BlockPos controller,
            Set<BlockPos> positions) {
        MultiblockPortHost host = null;
        if (level.hasChunkAt(controller)
                && level.getBlockEntity(controller)
                        instanceof MultiblockControllerBinding binding) {
            host = binding.portHost();
        }
        for (BlockPos position : positions) {
            if (level.hasChunkAt(position)
                    && level.getBlockEntity(position)
                            instanceof MultiblockPort port) {
                if (host != null
                        && port instanceof PortStoreCarrier carrier) {
                    PortStoreRegistry.unbind(host, position, carrier);
                }
                port.unbind(controller);
            }
        }
    }
}
