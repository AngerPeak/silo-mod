package com.silomod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.silomod.block.TerminalBlock;
import com.silomod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Генератор мира «Укрытие»: цилиндрическая башня в толще породы. */
public class SiloChunkGenerator extends ChunkGenerator {
    public static final Codec<SiloChunkGenerator> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource))
                    .apply(inst, inst.stable(SiloChunkGenerator::new)));

    private static final int MIN_Y = SiloLayout.MIN_Y;
    private static final int MAX_Y = SiloLayout.MAX_Y;

    public SiloChunkGenerator(BiomeSource source) {
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
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return MIN_Y;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return Math.min(level.getMaxBuildHeight(), MAX_Y + 1);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] states = new BlockState[level.getHeight()];
        Arrays.fill(states, Blocks.STONE.defaultBlockState());
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                        StructureManager structures, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = cp.getMinBlockX() + lx;
                int wz = cp.getMinBlockZ() + lz;
                Col c = Col.of(wx, wz);
                for (int y = MIN_Y; y <= MAX_Y; y++) {
                    BlockState st = c.r >= SiloLayout.R_WALL ? rock(wx, y, wz) : blockAt(c, y);
                    if (st != null && !st.isAir()) {
                        chunk.setBlockState(pos.set(wx, y, wz), st, false);
                    }
                }
            }
        }
        placeFeatures(chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    // ------------------------------------------------------------------ геометрия

    private static final class Col {
        double r, deg, arcC, distB;
        int sector;

        static Col of(int wx, int wz) {
            double cx = wx + 0.5, cz = wz + 0.5;
            Col c = new Col();
            c.r = Math.sqrt(cx * cx + cz * cz);
            double d = Math.toDegrees(Math.atan2(cz, cx));
            if (d < 0) d += 360.0;
            c.deg = d;
            c.sector = Math.min(7, (int) (d / 45.0));
            double within = d - c.sector * 45.0;
            c.arcC = c.r * Math.toRadians(Math.abs(within - 22.5));
            c.distB = c.r * Math.toRadians(Math.min(within, 45.0 - within));
            return c;
        }
    }

    private static final BlockState GRAY_CONCRETE = Blocks.GRAY_CONCRETE.defaultBlockState();
    private static final BlockState IRON_BARS = Blocks.IRON_BARS.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState FARMLAND = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
    private static final BlockState SERVER_BLOCK = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState SCREEN_GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
    private static final BlockState SERVER_LIGHT = Blocks.SEA_LANTERN.defaultBlockState();

    private static BlockState blockAt(Col c, int y) {
        double r = c.r;
        int n = SiloLayout.levelOf(y);
        int dy = Math.floorMod(y - MIN_Y, SiloLayout.LEVEL_H);
        Theme t = Theme.of(n);
        boolean slab = dy == 0 || dy == 7;

        // бетонная оболочка
        if (r >= SiloLayout.R_ROOMS) return GRAY_CONCRETE;

        // центральная колонна серверов
        if (r < SiloLayout.R_COLUMN) {
            if (slab) return SERVER_BLOCK;
            if (dy == 3) return SCREEN_GLASS;
            if (dy == 6 && ((int) (c.deg / 90.0) & 1) == 0) return SERVER_LIGHT;
            return SERVER_BLOCK;
        }

        // спиральная лестница: по одной ступени на сектор
        if (r < SiloLayout.R_STAIR) {
            return dy == c.sector ? t.floor : null;
        }

        // перила (с проходом на уровень в секторе 0)
        if (r < SiloLayout.R_RAIL) {
            if (c.sector != 0) return IRON_BARS;
            if (dy == 0) return t.floor;
            if (dy == 7) return t.wall;
            return null;
        }

        // кольцевой коридор
        if (r < SiloLayout.R_RING) {
            if (dy == 0) return t.floor;
            if (dy == 7) {
                boolean lamp = r >= 7.0 && r < 8.0 && (((int) (c.deg / 11.25)) & 1) == 0;
                return lamp ? t.light : t.wall;
            }
            return null;
        }

        // стена между коридором и комнатами (с дверью в центре сектора)
        if (r < SiloLayout.R_RINGWALL) {
            if (dy == 0) return t.floor;
            if (dy == 7) return t.wall;
            if (c.arcC < 1.6 && dy <= 3) return null;
            return dy == 3 ? t.accent : t.wall;
        }

        // комнаты
        if (dy == 7) {
            return roomLight(c, n, t) ? t.light : t.wall;
        }
        if (dy == 0) {
            if (c.distB < 0.75) return t.wall;
            if (t.kind == Theme.FARM && r >= 11 && r < 25.5) {
                boolean water = (r >= 15 && r < 16) || (r >= 21 && r < 22);
                return water ? WATER : FARMLAND;
            }
            return t.floor;
        }
        // радиальные перегородки и внешняя стена
        if (c.distB < 0.75 || r >= 26.0) {
            return dy == 3 ? t.accent : t.wall;
        }
        return decor(c, t, dy);
    }

    private static boolean roomLight(Col c, int level, Theme t) {
        if (Theme.abandoned(level)) return false;
        double r = c.r;
        if (t.kind == Theme.FARM) {
            return c.arcC < 5.0 && ((r >= 12 && r < 13) || (r >= 18 && r < 19) || (r >= 24 && r < 25)) && (((int) c.arcC) & 1) == 0;
        }
        return c.arcC < 1.2 && ((r >= 12 && r < 13) || (r >= 17 && r < 18) || (r >= 22 && r < 23));
    }

    private static BlockState decor(Col c, Theme t, int dy) {
        double r = c.r;
        // стол в центре комнаты
        if (t.kind != Theme.FARM && r >= 17 && r < 19 && c.arcC < 1.5) {
            if (dy == 1) return t.tableLeg;
            if (dy == 2) return t.tablePlate;
        }
        switch (t.kind) {
            case Theme.COMMAND, Theme.ARCHIVE -> {
                if (r >= 25 && r < 26 && c.arcC < 4.5 && dy <= 2) return Blocks.BOOKSHELF.defaultBlockState();
            }
            case Theme.SECURITY -> {
                if (r >= 20 && r < 21 && c.arcC < 5.0 && c.arcC > 1.2 && dy <= 3) return IRON_BARS;
            }
            case Theme.HOUSING -> {
                if (r >= 25 && r < 26 && c.arcC < 4.0 && dy == 1) return Blocks.CRAFTING_TABLE.defaultBlockState();
                if (r >= 23 && r < 25 && c.arcC >= 2.0 && c.arcC < 4.0 && dy == 1) return Blocks.WHITE_WOOL.defaultBlockState();
            }
            case Theme.MEDICAL -> {
                if (r >= 23.5 && r < 25.5 && c.arcC >= 1.5 && c.arcC < 3.5 && dy == 1) return Blocks.WHITE_WOOL.defaultBlockState();
                if (r >= 25 && r < 26 && c.arcC < 4.0 && dy <= 2) return Blocks.LIME_CONCRETE.defaultBlockState();
            }
            case Theme.MECH, Theme.PUMPS -> {
                if (r >= 24 && r < 26 && c.arcC < 3.0 && dy <= 2) return Blocks.SMOOTH_BASALT.defaultBlockState();
                if (r >= 14 && r < 16 && c.arcC >= 3.0 && c.arcC < 4.5) return Blocks.IRON_BARS.defaultBlockState();
            }
            default -> { }
        }
        return null;
    }

    // ------------------------------------------------------------------ порода и руды

    private static BlockState rock(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        boolean deep = y < 0;
        long h = (x * 341873128712L) ^ (z * 132897987541L) ^ (y * 3129871L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        int v = (int) ((h & 0x7fffffffffffffffL) % 10000L);
        if (v < 110) return (deep ? Blocks.DEEPSLATE_COAL_ORE : Blocks.COAL_ORE).defaultBlockState();
        if (v < 190) return (deep ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE).defaultBlockState();
        if (v < 260) return (deep ? Blocks.DEEPSLATE_COPPER_ORE : Blocks.COPPER_ORE).defaultBlockState();
        if (v < 310 && y < 48) return (deep ? Blocks.DEEPSLATE_REDSTONE_ORE : Blocks.REDSTONE_ORE).defaultBlockState();
        if (v < 340 && y < 48) return (deep ? Blocks.DEEPSLATE_LAPIS_ORE : Blocks.LAPIS_ORE).defaultBlockState();
        if (v < 360 && y < 30) return (deep ? Blocks.DEEPSLATE_GOLD_ORE : Blocks.GOLD_ORE).defaultBlockState();
        if (v < 368 && y < 20) return (deep ? Blocks.DEEPSLATE_DIAMOND_ORE : Blocks.DIAMOND_ORE).defaultBlockState();
        return (deep ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
    }

    // ------------------------------------------------------------------ сюжетные блоки

    private static void put(ChunkAccess chunk, BlockPos p, BlockState state) {
        ChunkPos cp = chunk.getPos();
        if ((p.getX() >> 4) == cp.x && (p.getZ() >> 4) == cp.z) {
            chunk.setBlockState(p, state, false);
        }
    }

    private void placeFeatures(ChunkAccess chunk) {
        BlockState terminal = ModBlocks.TERMINAL.get().defaultBlockState().setValue(TerminalBlock.FACING, Direction.NORTH);
        BlockState crate = ModBlocks.SUPPLY_CRATE.get().defaultBlockState();
        for (int n = 1; n <= SiloLayout.LEVELS; n++) {
            put(chunk, SiloLayout.terminalPos(n), terminal);
            put(chunk, SiloLayout.cratePos(n), crate);
        }
        BlockState evidence = ModBlocks.EVIDENCE.get().defaultBlockState();
        for (SiloLayout.EvidenceSpot e : SiloLayout.EVIDENCE) {
            put(chunk, SiloLayout.evidencePos(e), evidence);
        }
        BlockState machine = ModBlocks.MACHINE.get().defaultBlockState();
        for (int i = 0; i < SiloLayout.PUMP_SECTORS.length; i++) {
            put(chunk, SiloLayout.pumpPos(i), machine);
        }
        put(chunk, SiloLayout.airlockPos(), ModBlocks.AIRLOCK.get().defaultBlockState());
    }
}
