package com.silomod.world;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Оформление уровня в зависимости от отдела. */
public final class Theme {
    public static final int COMMAND = 0, SECURITY = 1, ARCHIVE = 2, HOUSING = 3,
            MEDICAL = 4, FARM = 5, MECH = 6, PUMPS = 7;

    public final int kind;
    public final BlockState floor, wall, accent, light, tableLeg, tablePlate;

    private Theme(int kind, BlockState floor, BlockState wall, BlockState accent, BlockState light,
                  BlockState tableLeg, BlockState tablePlate) {
        this.kind = kind;
        this.floor = floor;
        this.wall = wall;
        this.accent = accent;
        this.light = light;
        this.tableLeg = tableLeg;
        this.tablePlate = tablePlate;
    }

    private static Theme make(int kind, net.minecraft.world.level.block.Block floor, net.minecraft.world.level.block.Block wall,
                              net.minecraft.world.level.block.Block accent, net.minecraft.world.level.block.Block light,
                              boolean wooden) {
        return new Theme(kind, floor.defaultBlockState(), wall.defaultBlockState(), accent.defaultBlockState(),
                light.defaultBlockState(),
                (wooden ? Blocks.OAK_FENCE : Blocks.ANDESITE_WALL).defaultBlockState(),
                (wooden ? Blocks.OAK_PRESSURE_PLATE : Blocks.STONE_PRESSURE_PLATE).defaultBlockState());
    }

    private static final Theme[] ALL = {
            make(COMMAND, Blocks.SMOOTH_QUARTZ, Blocks.WHITE_CONCRETE, Blocks.RED_CONCRETE, Blocks.SEA_LANTERN, false),
            make(SECURITY, Blocks.DEEPSLATE_TILES, Blocks.GRAY_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.SEA_LANTERN, false),
            make(ARCHIVE, Blocks.POLISHED_ANDESITE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.SEA_LANTERN, false),
            make(HOUSING, Blocks.SPRUCE_PLANKS, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.DARK_OAK_PLANKS, Blocks.GLOWSTONE, true),
            make(MEDICAL, Blocks.SMOOTH_QUARTZ, Blocks.WHITE_CONCRETE, Blocks.LIME_CONCRETE, Blocks.SEA_LANTERN, false),
            make(FARM, Blocks.DIRT, Blocks.GRAY_CONCRETE, Blocks.LIME_TERRACOTTA, Blocks.VERDANT_FROGLIGHT, true),
            make(MECH, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.ORANGE_CONCRETE, Blocks.SHROOMLIGHT, false),
            make(PUMPS, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.YELLOW_CONCRETE, Blocks.SHROOMLIGHT, false)
    };

    public static Theme of(int level) {
        if (level <= 2) return ALL[COMMAND];
        if (level <= 6) return ALL[SECURITY];
        if (level <= 12) return ALL[ARCHIVE];
        if (level <= 24) return ALL[HOUSING];
        if (level <= 30) return ALL[MEDICAL];
        if (level <= 40) return ALL[FARM];
        if (level <= 47) return ALL[MECH];
        return ALL[PUMPS];
    }

    /** Заброшенные уровни: свет только в коридоре, внутри комнат заводятся твари. */
    public static boolean abandoned(int level) {
        return level >= 45 && level <= 47;
    }
}
