package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.worldgen.GtDungeonPiece;
import com.masson.cruciblecraft.worldgen.GtDungeonStructure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, CrucibleCraft.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, CrucibleCraft.MODID);

    public static final DeferredHolder<
            StructureType<?>, StructureType<GtDungeonStructure>> GT_DUNGEON_TYPE =
                    STRUCTURE_TYPES.register(
                            "gt_dungeon",
                            () -> () -> GtDungeonStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType>
            GT_DUNGEON_PIECE =
                    STRUCTURE_PIECES.register(
                            "gt_dungeon",
                            () -> (context, tag) -> new GtDungeonPiece(context, tag));

    private ModStructures() {}
}
