package com.silomod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.silomod.block.TerminalBlock;
import com.silomod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** «Снаружи»: пепельная пустошь с люком Укрытия в центре и радиомаяком на востоке. */
public class OutsideChunkGenerator extends ChunkGenerator {
    public static final Codec<OutsideChunkGenerator> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource))
                    .apply(inst, inst.stable(OutsideChunkGenerator::new)));

    public static final int HATCH_Y = 70;
    public static final int TOWER_X = 192;
    public static final int TOWER_Y = 72;

    private static final SimplexNoise NOISE = new SimplexNoise(new LegacyRandomSource(8128L));

    public OutsideChunkGenerator(BiomeSource source) {
        super(source);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes,
                             StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return heightAt(x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] states = new BlockState[level.getHeight()];
        int top = heightAt(x, z);
        for (int i = 0; i < states.length; i++) {
            states[i] = (level.getMinBuildHeight() + i) <= top ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    }

    // ------------------------------------------------------------------ рельеф

    public static int heightAt(int x, int z) {
        double h = 70.0
                + 14.0 * NOISE.getValue(x / 90.0, z / 90.0)
                + 6.0 * NOISE.getValue(x / 28.0 + 100.0, z / 28.0)
                + 2.0 * NOISE.getValue(x / 9.0, z / 9.0 + 50.0);
        h = flatten(h, x, z, 0, 0, HATCH_Y);
        h = flatten(h, x, z, TOWER_X, 0, TOWER_Y);
        return (int) Math.floor(h);
    }

    private static double flatten(double h, int x, int z, int cx, int cz, double target) {
        double dx = x - cx, dz = z - cz;
        double d = Math.sqrt(dx * dx + dz * dz);
        double t = Mth.clamp((d - 9.0) / 18.0, 0.0, 1.0);
        t = t * t * (3.0 - 2.0 * t);
        return Mth.lerp(t, target, h);
    }

    private static long hash(int x, int z) {
        long h = (x * 341873128712L) ^ (z * 132897987541L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return h & 0x7fffffffffffffffL;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                        StructureManager structures, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState andesite = Blocks.ANDESITE.defaultBlockState();
        BlockState tuff = Blocks.TUFF.defaultBlockState();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = cp.getMinBlockX() + lx;
                int wz = cp.getMinBlockZ() + lz;
                int top = heightAt(wx, wz);
                for (int y = 0; y <= top; y++) {
                    BlockState st;
                    if (y == 0) st = bedrock;
                    else if (y == top) st = tuff;
                    else if (y >= top - 4) st = andesite;
                    else st = y < 16 ? deepslate : stone;
                    chunk.setBlockState(pos.set(wx, y, wz), st, false);
                }

                // люк и маяк
                if (Math.abs(wx) <= 3 && Math.abs(wz) <= 3) {
                    for (int y = HATCH_Y; y <= HATCH_Y + 5; y++) {
                        BlockState st = hatch(wx, y, wz);
                        if (st != null) chunk.setBlockState(pos.set(wx, y, wz), st, false);
                    }
                } else if (Math.abs(wx - TOWER_X) <= 4 && Math.abs(wz) <= 4) {
                    for (int y = TOWER_Y; y <= TOWER_Y + 20; y++) {
                        BlockState st = tower(wx - TOWER_X, y - TOWER_Y, wz);
                        if (st != null) chunk.setBlockState(pos.set(wx, y, wz), st, false);
                    }
                } else if (Math.sqrt((double) wx * wx + (double) wz * wz) > 12 && hash(wx, wz) % 420 == 0) {
                    // мёртвое дерево
                    int height = 3 + (int) (hash(wz, wx) % 5);
                    BlockState log = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
                    for (int i = 1; i <= height && top + i < 255; i++) {
                        chunk.setBlockState(pos.set(wx, top + i, wz), log, false);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /** Лючный домик 7x7: внутри возвратный шлюз. */
    private static BlockState hatch(int x, int y, int z) {
        int dy = y - HATCH_Y;
        boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
        if (dy == 0) return Blocks.POLISHED_ANDESITE.defaultBlockState();
        if (dy == 5) return (x == 0 && z == 0) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
        if (edge) {
            if (x == 3 && z == 0 && dy <= 2) return null; // дверной проём
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (x == -2 && z == 0 && dy == 1) return ModBlocks.AIRLOCK.get().defaultBlockState();
        return null;
    }

    /** Радиомаяк: решётчатая мачта, терминал и площадка. */
    private static BlockState tower(int dx, int dy, int dz) {
        if (dy == 0) return Blocks.STONE_BRICKS.defaultBlockState();
        if (dx == 0 && dz == 0) {
            if (dy <= 18) return Blocks.IRON_BARS.defaultBlockState();
            if (dy == 19) return Blocks.LIGHTNING_ROD.defaultBlockState();
        }
        if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy <= 6) return Blocks.IRON_BARS.defaultBlockState();
        if (dx == -2 && dz == 0 && dy == 1) {
            return ModBlocks.TERMINAL.get().defaultBlockState().setValue(TerminalBlock.FACING, Direction.EAST);
        }
        return null;
    }
}
