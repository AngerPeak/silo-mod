package com.silomod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/**
 * Геометрия Укрытия-17. 48 уровней по 8 блоков, центр — в точке (0,0).
 * Уровень 1 — самый верх (y 312..319), уровень 48 — самое дно (y -64..-57).
 */
public final class SiloLayout {
    public static final int LEVELS = 48;
    public static final int LEVEL_H = 8;
    public static final int MIN_Y = -64;
    public static final int MAX_Y = 319;

    public static final double R_COLUMN = 2.6;     // центральная колонна серверов
    public static final double R_STAIR = 5.4;      // спиральная лестница
    public static final double R_RAIL = 5.9;       // перила
    public static final double R_RING = 9.0;       // кольцевой коридор
    public static final double R_RINGWALL = 10.0;  // стена комнат
    public static final double R_ROOMS = 27.0;     // внешняя стена комнат
    public static final double R_WALL = 30.0;      // бетонная оболочка

    /** Где лежат улики: id, уровень, сектор (0..7). id 0 — тело, 1..4 — улики на подозреваемых. */
    public record EvidenceSpot(int id, int level, int sector) {}

    public static final EvidenceSpot[] EVIDENCE = {
            new EvidenceSpot(0, 48, 2),
            new EvidenceSpot(1, 2, 5),
            new EvidenceSpot(2, 27, 4),
            new EvidenceSpot(3, 9, 1),
            new EvidenceSpot(4, 34, 6)
    };

    public static final int[] PUMP_SECTORS = {1, 4, 6};

    private SiloLayout() {}

    public static int baseY(int level) {
        return 320 - LEVEL_H * level;
    }

    public static int levelOf(int y) {
        return Mth.clamp(LEVELS - Math.floorDiv(y - MIN_Y, LEVEL_H), 1, LEVELS);
    }

    public static BlockPos polar(double r, double deg, int y) {
        double rad = Math.toRadians(deg);
        return new BlockPos((int) Math.floor(r * Math.cos(rad)), y, (int) Math.floor(r * Math.sin(rad)));
    }

    public static BlockPos terminalPos(int level) {
        return polar(7.2, 90, baseY(level) + 1);
    }

    public static BlockPos cratePos(int level) {
        return polar(7.2, 270, baseY(level) + 1);
    }

    public static BlockPos evidencePos(EvidenceSpot e) {
        return polar(23.5, e.sector() * 45 + 22.5, baseY(e.level()) + 1);
    }

    public static BlockPos pumpPos(int index) {
        return polar(21, PUMP_SECTORS[index] * 45 + 22.5, baseY(48) + 1);
    }

    public static BlockPos airlockPos() {
        return polar(24, 22.5, baseY(1) + 1);
    }

    /** Куда попадает игрок, вернувшийся снаружи. */
    public static BlockPos airlockLanding() {
        return polar(20, 22.5, baseY(1) + 1);
    }

    /** Жилая каюта на 20 уровне — отсюда всё начинается. */
    public static BlockPos startPos() {
        return polar(12, 157.5, baseY(20) + 1);
    }

    /** Телепорт-точка для отладки: коридор нужного уровня. */
    public static BlockPos corridorPos(int level) {
        return polar(7.5, 180, baseY(level) + 1);
    }

    public static int evidenceIdAtLevel(int level) {
        for (EvidenceSpot e : EVIDENCE) {
            if (e.level() == level) return e.id();
        }
        return -1;
    }

    public static String department(int n) {
        if (n == 1) return "Верхний шлюз";
        if (n == 2) return "Зал Совета";
        if (n <= 6) return "Служба порядка";
        if (n <= 12) return "Информационный отдел";
        if (n <= 24) return "Жилой сектор";
        if (n <= 30) return "Медицинский сектор";
        if (n <= 40) return "Гидропоника";
        if (n <= 47) return "Механический цех";
        return "Насосная и генераторная";
    }
}
