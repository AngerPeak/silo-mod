package com.silomod.registry;

import com.mojang.serialization.Codec;
import com.silomod.SiloMod;
import com.silomod.world.OutsideChunkGenerator;
import com.silomod.world.SiloChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModWorld {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, SiloMod.MODID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> SILO =
            CHUNK_GENERATORS.register("silo", () -> SiloChunkGenerator.CODEC);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> OUTSIDE =
            CHUNK_GENERATORS.register("outside", () -> OutsideChunkGenerator.CODEC);

    private ModWorld() {}
}
