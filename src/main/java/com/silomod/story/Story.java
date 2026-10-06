package com.silomod.story;

import com.silomod.SiloMod;
import com.silomod.registry.ModItems;
import com.silomod.world.SiloChunkGenerator;
import com.silomod.world.SiloLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Arrays;

/** Сюжет, квесты, терминалы и механика выживания. */
public final class Story {
    public static final int ST_BODY = 1, ST_CLUES = 2, ST_ACCUSE = 3, ST_PUMPS = 4,
            ST_CHOICE = 5, ST_EXIT = 6, ST_DONE = 7;

    public static final ResourceKey<Level> OUTSIDE =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(SiloMod.MODID, "outside"));

    private static final int CRATE_COOLDOWN = 6000; // 5 минут

    private Story() {}

    // ================================================================= данные

    public static CompoundTag data(ServerPlayer p) {
        CompoundTag persistent = p.getPersistentData();
        if (!persistent.contains(Player.PERSISTED_NBT_TAG, 10)) {
            persistent.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains("silo", 10)) {
            persisted.put("silo", new CompoundTag());
        }
        return persisted.getCompound("silo");
    }

    public static boolean isSiloWorld(MinecraftServer server) {
        return server != null && server.overworld().getChunkSource().getGenerator() instanceof SiloChunkGenerator;
    }

    public static int stage(ServerPlayer p) {
        return data(p).getInt("stage");
    }

    public static int culprit(ServerPlayer p) {
        return data(p).getInt("culprit");
    }

    public static boolean inOutside(ServerPlayer p) {
        return p.level().dimension().equals(OUTSIDE);
    }

    public static boolean inSilo(ServerPlayer p) {
        return p.level().dimension().equals(Level.OVERWORLD);
    }

    public static int levelOf(ServerPlayer p) {
        return SiloLayout.levelOf(p.blockPosition().getY());
    }

    // ================================================================= сообщения

    static void say(ServerPlayer p, String s, ChatFormatting... f) {
        p.sendSystemMessage(T.t(s, f));
    }

    static void gap(ServerPlayer p) {
        p.sendSystemMessage(Component.literal(" "));
    }

    static void sound(ServerPlayer p, net.minecraft.sounds.SoundEvent ev, float pitch) {
        p.playNotifySound(ev, SoundSource.PLAYERS, 0.8F, pitch);
    }

    public static void award(ServerPlayer p, String id) {
        Advancement adv = p.server.getAdvancements().getAdvancement(new ResourceLocation(SiloMod.MODID, id));
        if (adv == null) return;
        AdvancementProgress progress = p.getAdvancements().getOrStartProgress(adv);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            p.getAdvancements().award(adv, criterion);
        }
    }

    static void give(ServerPlayer p, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(p, stack);
    }

    static int count(Player p, Item item) {
        int c = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) c += s.getCount();
        }
        return c;
    }

    static void take(Player p, Item item, int amount) {
        int left = amount;
        for (ItemStack s : p.getInventory().items) {
            if (left <= 0) break;
            if (s.is(item)) {
                int d = Math.min(left, s.getCount());
                s.shrink(d);
                left -= d;
            }
        }
    }

    // ================================================================= старт

    public static void ensureStarted(ServerPlayer p) {
        if (!isSiloWorld(p.server)) return;
        CompoundTag d = data(p);
        if (d.getBoolean("init")) return;
        d.putBoolean("init", true);
        d.putInt("culprit", p.getRandom().nextInt(4));
        d.putInt("stage", ST_BODY);

        BlockPos s = SiloLayout.startPos();
        ServerLevel overworld = p.server.overworld();
        p.teleportTo(overworld, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, 90.0F, 0.0F);
        p.setRespawnPosition(Level.OVERWORLD, s, 0.0F, true, false);

        give(p, new ItemStack(ModItems.JOURNAL.get()));
        give(p, new ItemStack(ModItems.BADGE.get()));
        give(p, new ItemStack(ModItems.RATION.get(), 3));
        give(p, new ItemStack(Items.STONE_PICKAXE));
        give(p, new ItemStack(Items.STONE_SHOVEL));

        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 30));
        p.connection.send(new ClientboundSetSubtitleTextPacket(T.t("Мир, где воздух — это приговор", ChatFormatting.GRAY)));
        p.connection.send(new ClientboundSetTitleTextPacket(T.t("УКРЫТИЕ-17", ChatFormatting.GOLD, ChatFormatting.BOLD)));

        gap(p);
        say(p, "Сирена стихает. Над дверью мигает табличка: «Смена 03:40». Вы — следователь Службы порядка Укрытия-17: "
                + "сорок восемь уровней вниз по спирали, четыреста восемьдесят живых людей и ни одного окна.", ChatFormatting.GRAY);
        say(p, "Ночью в насосной на 48-м уровне нашли главного инженера Илью Роста. Совет называет это несчастным случаем. "
                + "Вам приказано поставить подпись и закрыть дело.", ChatFormatting.GRAY);
        say(p, "Но Рост отправил вам записку за час до смерти.", ChatFormatting.YELLOW);
        gap(p);
        say(p, "▶ Откройте журнал (ПКМ) — в нём цели и улики. Лестница — в центре этажа (за решёткой, "
                + "проход в секторе справа от терминала). Спуститесь на 48-й уровень.", ChatFormatting.AQUA);
        award(p, "root");
    }

    public static void onRespawn(ServerPlayer p) {
        if (!isSiloWorld(p.server) || !data(p).getBoolean("init")) return;
        if (count(p, ModItems.JOURNAL.get()) == 0) {
            give(p, new ItemStack(ModItems.JOURNAL.get()));
        }
        say(p, "Вас вернули в жилой отсек. Журнал и память — при вас.", ChatFormatting.GRAY);
    }

    // ================================================================= цели

    public static String objective(ServerPlayer p) {
        CompoundTag d = data(p);
        return switch (d.getInt("stage")) {
            case ST_BODY -> "Спуститесь на 48-й уровень и осмотрите место происшествия в насосной (жёлтый ящик в одной из комнат).";
            case ST_CLUES -> "Найдите улики на подозреваемых: уровни 2, 9, 27, 34. Собрано: " + Integer.bitCount(d.getInt("clues")) + "/4.";
            case ST_ACCUSE -> "Все улики собраны. Поднимитесь в Зал Совета (уровень 2) и назовите виновного на терминале.";
            case ST_PUMPS -> "Почините три насоса на 48-м уровне (оранжевые машины). Нужно на каждый: 3 железа, 2 красной пыли, 1 медь. "
                    + "Починено: " + d.getLongArray("pumps").length + "/3.";
            case ST_CHOICE -> "Воздух очищается. На терминале Совета (уровень 2) решите судьбу Укрытия: остаться или выйти.";
            case ST_EXIT -> "Наденьте скафандр и поднимитесь к шлюзу на 1-м уровне. Снаружи идите на восток (~190 блоков) к радиомаяку.";
            default -> "Основная линия завершена. Помогайте жителям (терминалы отделов) и выживайте.";
        };
    }

    // ================================================================= терминал

    public static void useTerminal(ServerPlayer p, BlockPos pos) {
        if (!isSiloWorld(p.server)) {
            say(p, "Терминал не отвечает. Он работает только в мире «Укрытие».", ChatFormatting.RED);
            return;
        }
        ensureStarted(p);
        sound(p, SoundEvents.LEVER_CLICK, 1.2F);
        if (inOutside(p)) {
            outsideTerminal(p);
            return;
        }
        int n = SiloLayout.levelOf(pos.getY());
        gap(p);
        p.sendSystemMessage(T.t("══ ТЕРМИНАЛ ▸ Уровень " + n + " • " + SiloLayout.department(n) + " ══", ChatFormatting.GOLD, ChatFormatting.BOLD));
        CompoundTag d = data(p);
        int st = d.getInt("stage");

        if (n == 2) {
            councilTerminal(p, st);
        } else if (n == 1) {
            say(p, "Верхний шлюз. Допуск наружу — только по решению Совета. Снаружи — ядовитая атмосфера, "
                    + "скафандр выдерживает около десяти минут.", ChatFormatting.GRAY);
            say(p, d.getBoolean("exitOpen") ? "Статус шлюза: ОТКРЫТ для вас." : "Статус шлюза: ЗАБЛОКИРОВАН.",
                    d.getBoolean("exitOpen") ? ChatFormatting.GREEN : ChatFormatting.RED);
        }

        for (Side side : SIDES) {
            if (n >= side.lo && n <= side.hi) {
                sideQuestTerminal(p, side);
            }
        }

        long slot = p.level().getGameTime() / 12000L;
        int idx = (int) Math.floorMod(slot * 7 + n * 3L, (long) Texts.RUMORS.length);
        say(p, "▫ " + Texts.RUMORS[idx], ChatFormatting.DARK_GRAY);
        say(p, "Цель: " + objective(p), ChatFormatting.AQUA);
    }

    private static void councilTerminal(ServerPlayer p, int st) {
        switch (st) {
            case ST_BODY, ST_CLUES -> {
                say(p, "Мэр Корво: «Дело Роста — несчастный случай. Подпишите заключение, следователь».", ChatFormatting.GRAY);
                say(p, "Для обвинения нужны все четыре фрагмента ключа Роста.", ChatFormatting.GRAY);
            }
            case ST_ACCUSE -> {
                say(p, "Совет ждёт вашего заключения. Назовите виновного (у вас " + (3 - data(p).getInt("wrong")) + " попытки):", ChatFormatting.YELLOW);
                for (int i = 0; i < 4; i++) {
                    p.sendSystemMessage(T.t(" ▸ " + Texts.SUSPECT_FULL[i] + "  ", ChatFormatting.WHITE)
                            .append(T.btn("Обвинить", "/silo accuse " + i, "Назвать виновным: " + Texts.SUSPECT_SHORT[i])));
                }
                say(p, "Три ошибки — и Совет отправит на «очистку» вас самого.", ChatFormatting.RED);
            }
            case ST_PUMPS -> say(p, "Совет молчит и делает вид, что всё в порядке. Но воздух всё тяжелее — насосы на 48-м ждут вас.", ChatFormatting.GRAY);
            case ST_CHOICE -> {
                say(p, "Насосы запущены. Совет готов выслушать вас:", ChatFormatting.YELLOW);
                p.sendSystemMessage(T.t(" ▸ Остаться и стать Хранителем Укрытия  ", ChatFormatting.WHITE)
                        .append(T.btn("Остаться", "/silo choose stay", "Закончить расследование и остаться")));
                p.sendSystemMessage(T.t(" ▸ Выйти наружу, чтобы проверить правду  ", ChatFormatting.WHITE)
                        .append(T.btn("Выйти", "/silo choose leave", "Получить допуск и скафандр")));
            }
            case ST_EXIT -> say(p, "Шлюз на 1-м уровне открыт для вас. Идите, пока Совет не передумал.", ChatFormatting.GRAY);
            default -> say(p, "Совет держит заседание. Теперь вы — часть его решений.", ChatFormatting.GRAY);
        }
    }

    // ================================================================= обвинение и выбор

    public static int accuse(ServerPlayer p, int who) {
        if (!isSiloWorld(p.server) || !inSilo(p)) return 0;
        CompoundTag d = data(p);
        if (d.getInt("stage") != ST_ACCUSE) {
            say(p, "Сейчас вы не можете выдвинуть обвинение.", ChatFormatting.RED);
            return 0;
        }
        if (levelOf(p) != 2) {
            say(p, "Обвинение подают только в Зале Совета (уровень 2).", ChatFormatting.RED);
            return 0;
        }
        if (who == d.getInt("culprit")) {
            d.putInt("stage", ST_PUMPS);
            award(p, "detective");
            sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
            gap(p);
            say(p, "Вы называете имя — и в зале становится тихо.", ChatFormatting.GOLD);
            say(p, Texts.CONFESSION[who], ChatFormatting.WHITE);
            gap(p);
            say(p, Texts.TRUTH, ChatFormatting.YELLOW);
            give(p, new ItemStack(ModItems.ACCESS_KEY.get()));
            say(p, "Вы получаете собранный ключ Роста.", ChatFormatting.GRAY);
            say(p, "Цель: " + objective(p), ChatFormatting.AQUA);
            return 1;
        }
        int wrong = d.getInt("wrong") + 1;
        d.putInt("wrong", wrong);
        award(p, "wrong_accuse");
        sound(p, SoundEvents.ANVIL_USE, 0.6F);
        if (wrong >= 3) {
            cleaning(p);
        } else {
            say(p, "Совет отклоняет обвинение: улики против «" + Texts.SUSPECT_SHORT[who] + "» не сходятся. Осталось попыток: " + (3 - wrong) + ".", ChatFormatting.RED);
            say(p, "Перечитайте журнал: сравните время и алиби.", ChatFormatting.GRAY);
        }
        return 1;
    }

    private static void cleaning(ServerPlayer p) {
        CompoundTag d = data(p);
        d.putInt("wrong", 0);
        award(p, "cleaning");
        gap(p);
        say(p, "«Следователь, вы потеряли доверие Совета. Вам назначена очистка».", ChatFormatting.DARK_RED);
        say(p, "Вас без скафандра ведут к верхнему шлюзу. Дверь за спиной закрывается.", ChatFormatting.GRAY);
        sendOutside(p);
    }

    public static int choose(ServerPlayer p, String what) {
        if (!isSiloWorld(p.server) || !inSilo(p)) return 0;
        CompoundTag d = data(p);
        if (d.getInt("stage") != ST_CHOICE) {
            say(p, "Время выбора ещё не пришло.", ChatFormatting.RED);
            return 0;
        }
        if (levelOf(p) != 2) {
            say(p, "Свой выбор называют в Зале Совета (уровень 2).", ChatFormatting.RED);
            return 0;
        }
        gap(p);
        if (what.equals("stay")) {
            d.putInt("stage", ST_DONE);
            d.putString("ending", "keeper");
            award(p, "keeper");
            give(p, new ItemStack(ModItems.MEDALLION.get()));
            sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
            say(p, "Вы остаётесь. Корво уходит в отставку, а на её место садится тот, кого Рост считал достойным — вы.", ChatFormatting.GOLD);
            say(p, "Укрытие-17 дышит. Впереди — пайки, ремонт, ферма и тайны, которые вы никому не расскажете. "
                    + "Игра продолжается: отвечайте на терминалах, выполняйте задания отделов.", ChatFormatting.WHITE);
        } else {
            d.putInt("stage", ST_EXIT);
            d.putString("ending", "exit");
            d.putBoolean("exitOpen", true);
            award(p, "exit");
            give(p, new ItemStack(ModItems.SUIT_HELMET.get()));
            give(p, new ItemStack(ModItems.SUIT_CHESTPLATE.get()));
            give(p, new ItemStack(ModItems.SUIT_LEGGINGS.get()));
            give(p, new ItemStack(ModItems.SUIT_BOOTS.get()));
            sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.7F);
            say(p, "Вы просите открыть шлюз. Совет молчит так долго, что слышно, как гудят насосы. Потом Корво кивает.", ChatFormatting.GOLD);
            say(p, "Вам выдают скафандр. Он держит воздух около десяти минут — каждая секунда снаружи стоит прочности.", ChatFormatting.WHITE);
            say(p, "Цель: " + objective(p), ChatFormatting.AQUA);
        }
        return 1;
    }

    // ================================================================= улики

    public static void examine(ServerPlayer p, BlockPos pos) {
        if (!isSiloWorld(p.server)) return;
        ensureStarted(p);
        int level = SiloLayout.levelOf(pos.getY());
        int id = SiloLayout.evidenceIdAtLevel(level);
        CompoundTag d = data(p);
        int st = d.getInt("stage");
        gap(p);
        if (id == 0) {
            p.sendSystemMessage(T.t("══ МЕСТО ПРОИСШЕСТВИЯ ══", ChatFormatting.GOLD, ChatFormatting.BOLD));
            say(p, Texts.BODY, ChatFormatting.WHITE);
            if (st == ST_BODY) {
                d.putInt("stage", ST_CLUES);
                award(p, "body");
                sound(p, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F);
                say(p, "Цель: " + objective(p), ChatFormatting.AQUA);
            }
            return;
        }
        if (id < 1) return;
        if (st < ST_CLUES) {
            say(p, "Папки, схемы, чьи-то подписи. Без записки Роста непонятно, что здесь искать. Начните с насосной (48-й уровень).", ChatFormatting.GRAY);
            return;
        }
        int suspect = id - 1;
        boolean guilty = d.getInt("culprit") == suspect;
        p.sendSystemMessage(T.t("══ УЛИКА: " + Texts.SUSPECT_SHORT[suspect].toUpperCase() + " ══", ChatFormatting.GOLD, ChatFormatting.BOLD));
        say(p, Texts.CLUE[suspect][guilty ? 0 : 1], ChatFormatting.WHITE);

        int bit = 1 << suspect;
        int clues = d.getInt("clues");
        if ((clues & bit) == 0) {
            clues |= bit;
            d.putInt("clues", clues);
            give(p, new ItemStack(ModItems.KEY_FRAGMENT.get()));
            award(p, new String[]{"clue_mayor", "clue_doctor", "clue_archive", "clue_farmer"}[suspect]);
            sound(p, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F);
            say(p, "Вы нашли фрагмент ключа Роста. (" + Integer.bitCount(clues) + "/4)", ChatFormatting.GREEN);
            if (clues == 0b1111 && st == ST_CLUES) {
                d.putInt("stage", ST_ACCUSE);
                say(p, "Четыре фрагмента сходятся в один ключ. Пора назвать виновного.", ChatFormatting.YELLOW);
            }
            say(p, "Цель: " + objective(p), ChatFormatting.AQUA);
        }
    }

    // ================================================================= снабжение

    public static void openCrate(ServerPlayer p, BlockPos pos) {
        if (!isSiloWorld(p.server)) return;
        ensureStarted(p);
        CompoundTag d = data(p);
        if (!d.contains("crates", 10)) d.put("crates", new CompoundTag());
        CompoundTag crates = d.getCompound("crates");
        String key = Long.toString(pos.asLong());
        long now = p.level().getGameTime();
        long last = crates.contains(key) ? crates.getLong(key) : -CRATE_COOLDOWN;
        if (now - last < CRATE_COOLDOWN) {
            long leftSec = (CRATE_COOLDOWN - (now - last)) / 20;
            say(p, "Ящик пуст. Следующая выдача через " + (leftSec / 60) + " мин " + (leftSec % 60) + " сек.", ChatFormatting.GRAY);
            return;
        }
        crates.putLong(key, now);
        int n = SiloLayout.levelOf(pos.getY());
        give(p, new ItemStack(ModItems.RATION.get(), 2));
        if (n >= 31 && n <= 40) {
            give(p, new ItemStack(Items.WHEAT_SEEDS, 4));
            give(p, new ItemStack(Items.CARROT, 2));
            give(p, new ItemStack(Items.POTATO, 2));
        } else if (n >= 25 && n <= 30) {
            give(p, new ItemStack(Items.BREAD, 3));
        } else if (n >= 41) {
            give(p, new ItemStack(Items.COAL, 2));
        }
        sound(p, SoundEvents.CHEST_OPEN, 1.0F);
        say(p, "Вы получили пайки по норме отдела «" + SiloLayout.department(n) + "».", ChatFormatting.GREEN);
        award(p, "supply");
    }

    // ================================================================= насосы

    public static void useMachine(ServerPlayer p, BlockPos pos) {
        if (!isSiloWorld(p.server)) return;
        ensureStarted(p);
        CompoundTag d = data(p);
        int st = d.getInt("stage");
        if (st < ST_PUMPS) {
            say(p, "Насос гудит с надрывом. Пока вы не разобрались с делом Роста, чинить его нет смысла.", ChatFormatting.GRAY);
            return;
        }
        long[] done = d.getLongArray("pumps");
        long key = pos.asLong();
        if (Arrays.stream(done).anyMatch(v -> v == key)) {
            say(p, "Этот насос уже работает ровно.", ChatFormatting.GREEN);
            return;
        }
        if (st > ST_PUMPS) {
            say(p, "Насос исправен.", ChatFormatting.GREEN);
            return;
        }
        int iron = count(p, Items.IRON_INGOT), red = count(p, Items.REDSTONE), cu = count(p, Items.COPPER_INGOT);
        if (iron < 3 || red < 2 || cu < 1) {
            say(p, "Для ремонта нужно: 3 железа (" + iron + "), 2 красной пыли (" + red + "), 1 медный слиток (" + cu + ").", ChatFormatting.RED);
            say(p, "Руды — в породе за бетонной оболочкой, награды — на заданиях отделов (терминалы).", ChatFormatting.GRAY);
            return;
        }
        take(p, Items.IRON_INGOT, 3);
        take(p, Items.REDSTONE, 2);
        take(p, Items.COPPER_INGOT, 1);
        long[] next = Arrays.copyOf(done, done.length + 1);
        next[done.length] = key;
        d.putLongArray("pumps", next);
        sound(p, SoundEvents.ANVIL_USE, 1.1F);
        say(p, "Вы меняете втулки и перепаиваете контакты. Насос набирает ход. (" + next.length + "/3)", ChatFormatting.GREEN);
        if (next.length >= 3) {
            d.putInt("stage", ST_CHOICE);
            award(p, "mechanic");
            sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
            gap(p);
            say(p, "Воздух становится свежее. На последнем листе чертежей Роста — приписка: «Снаружи не так, как нам говорят. Сенсоры врут».", ChatFormatting.YELLOW);
        }
        say(p, "Цель: " + objective(p), ChatFormatting.AQUA);
    }

    // ================================================================= шлюз и «Снаружи»

    public static void useAirlock(ServerPlayer p, BlockPos pos) {
        if (!isSiloWorld(p.server)) return;
        ensureStarted(p);
        if (inOutside(p)) {
            ServerLevel silo = p.server.overworld();
            BlockPos b = SiloLayout.airlockLanding();
            p.teleportTo(silo, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 90.0F, 0.0F);
            say(p, "Шлюз герметично закрывается. Вы дышите — и это почти странно.", ChatFormatting.GRAY);
            return;
        }
        CompoundTag d = data(p);
        if (!d.getBoolean("exitOpen") && !p.isCreative()) {
            say(p, "Шлюз заблокирован допуском Совета. Нужно получить разрешение (основная линия, финал).", ChatFormatting.RED);
            return;
        }
        if (!hasFullSuit(p)) {
            say(p, "Без полного скафандра снаружи вы протянете считанные секунды. Наденьте все четыре части.", ChatFormatting.RED);
            if (!p.isShiftKeyDown()) {
                say(p, "(Пригнитесь и нажмите ещё раз, если хотите выйти без защиты.)", ChatFormatting.DARK_GRAY);
                return;
            }
        }
        sendOutside(p);
    }

    static void sendOutside(ServerPlayer p) {
        ServerLevel outside = p.server.getLevel(OUTSIDE);
        if (outside == null) {
            say(p, "Снаружи пусто: измерение «Снаружи» не загружено. Создайте мир с типом «Укрытие».", ChatFormatting.RED);
            return;
        }
        p.teleportTo(outside, 0.5, 71.0, 0.5, 90.0F, 0.0F);
        sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.6F);
        award(p, "outside");
        gap(p);
        say(p, "Дверь открывается. Небо цвета старого пепла, ветер скребёт по стеклу шлема. Сенсоры в голове пищат: «токсично».", ChatFormatting.GRAY);
        say(p, "Но вы дышите. Пока. Идите на восток — к антенне на горизонте.", ChatFormatting.AQUA);
    }

    static boolean hasFullSuit(Player p) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SUIT_HELMET.get())
                && p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SUIT_CHESTPLATE.get())
                && p.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SUIT_LEGGINGS.get())
                && p.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SUIT_BOOTS.get());
    }

    private static void outsideTerminal(ServerPlayer p) {
        CompoundTag d = data(p);
        gap(p);
        p.sendSystemMessage(T.t("══ РАДИОМАЯК ══", ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (d.getInt("stage") == ST_EXIT) {
            d.putInt("stage", ST_DONE);
            award(p, "exit_final");
            give(p, new ItemStack(ModItems.MEDALLION.get()));
            give(p, new ItemStack(Items.DIAMOND, 3));
            sound(p, SoundEvents.BEACON_ACTIVATE, 1.0F);
            say(p, "Вы вставляете ключ Роста — экран оживает. Маяк просыпается и бьёт в небо тонким лучом.", ChatFormatting.WHITE);
            say(p, "Ответ приходит через минуту. Потом ещё один. И ещё. На горизонте один за другим загораются слабые огни — ", ChatFormatting.WHITE);
            say(p, "десятки, сотни. Другие Укрытия. Всё это время вы были не одни.", ChatFormatting.YELLOW);
            say(p, "КОНЕЦ ПЕРВОЙ ЧАСТИ. Вернитесь в люк — Укрытие-17 должно узнать правду.", ChatFormatting.GOLD, ChatFormatting.BOLD);
        } else {
            say(p, "Маяк работает. На экране мерцают координаты соседних Укрытий.", ChatFormatting.GRAY);
        }
    }

    // ================================================================= задания отделов

    private record Side(String id, int lo, int hi, String title, String desc, Item item, int count, String adv) {}

    private static final Side[] SIDES = {
            new Side("iron", 41, 47, "Нормы выработки", "Сдайте 16 железных слитков на нужды цеха.", Items.IRON_INGOT, 16, "side_iron"),
            new Side("farm", 31, 40, "Урожай для уровня", "Сдайте 24 пшеницы на общий котёл.", Items.WHEAT, 24, "side_farm"),
            new Side("medic", 25, 30, "Аптечные запасы", "Сдайте 8 хлебов для больничного питания.", Items.BREAD, 8, "side_medic"),
            new Side("rats", 3, 6, "Крысы в вентиляции", "Убейте 12 тварей в заброшенных уровнях (45–47) и доложите.", null, 12, "side_rats")
    };

    private static void sideQuestTerminal(ServerPlayer p, Side s) {
        CompoundTag d = data(p);
        gap(p);
        say(p, "▣ Задание отдела: " + s.title, ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        say(p, s.desc, ChatFormatting.GRAY);
        if (d.getBoolean("side_" + s.id)) {
            say(p, "Выполнено. Отдел благодарит вас.", ChatFormatting.GREEN);
            return;
        }
        int have = s.item != null ? count(p, s.item) : d.getInt("kills");
        say(p, "Прогресс: " + Math.min(have, s.count) + "/" + s.count, ChatFormatting.WHITE);
        p.sendSystemMessage(T.btn("Сдать", "/silo handin " + s.id, "Отдать требуемое и получить награду"));
    }

    public static int handin(ServerPlayer p, String id) {
        if (!isSiloWorld(p.server) || !inSilo(p)) return 0;
        Side side = null;
        for (Side s : SIDES) if (s.id.equals(id)) side = s;
        if (side == null) return 0;
        int n = levelOf(p);
        if (n < side.lo || n > side.hi) {
            say(p, "Это задание сдаётся на уровнях " + side.lo + "–" + side.hi + ".", ChatFormatting.RED);
            return 0;
        }
        CompoundTag d = data(p);
        if (d.getBoolean("side_" + id)) {
            say(p, "Вы уже выполнили это задание.", ChatFormatting.GRAY);
            return 0;
        }
        if (side.item != null) {
            if (count(p, side.item) < side.count) {
                say(p, "Не хватает: нужно " + side.count + ", у вас " + count(p, side.item) + ".", ChatFormatting.RED);
                return 0;
            }
            take(p, side.item, side.count);
        } else if (d.getInt("kills") < side.count) {
            say(p, "Вы убили лишь " + d.getInt("kills") + " из " + side.count + ".", ChatFormatting.RED);
            return 0;
        }
        d.putBoolean("side_" + id, true);
        switch (id) {
            case "iron" -> {
                give(p, new ItemStack(ModItems.BADGE.get()));
                give(p, new ItemStack(Items.REDSTONE, 8));
                give(p, new ItemStack(Items.COPPER_INGOT, 8));
            }
            case "farm" -> {
                give(p, new ItemStack(Items.GOLDEN_CARROT, 8));
                give(p, new ItemStack(ModItems.RATION.get(), 4));
            }
            case "medic" -> give(p, new ItemStack(Items.GOLDEN_APPLE, 2));
            default -> {
                give(p, new ItemStack(Items.IRON_INGOT, 8));
                give(p, new ItemStack(Items.DIAMOND, 1));
            }
        }
        award(p, side.adv);
        sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2F);
        say(p, "Задание «" + side.title + "» выполнено. Награда получена.", ChatFormatting.GREEN);
        return 1;
    }

    public static void onKill(ServerPlayer p) {
        if (!isSiloWorld(p.server) || !inSilo(p)) return;
        CompoundTag d = data(p);
        d.putInt("kills", d.getInt("kills") + 1);
    }

    // ================================================================= каждую секунду

    public static void secondTick(ServerPlayer p) {
        if (!isSiloWorld(p.server)) return;
        ensureStarted(p);
        if (inOutside(p)) {
            outsideTick(p);
        } else if (inSilo(p)) {
            interiorTick(p);
        }
    }

    private static void interiorTick(ServerPlayer p) {
        CompoundTag d = data(p);
        int n = levelOf(p);
        if (d.getInt("lastLevel") != n) {
            d.putInt("lastLevel", n);
            p.displayClientMessage(T.t("Уровень " + n + " • " + SiloLayout.department(n), ChatFormatting.GOLD), true);
            if (n == 1) award(p, "level_top");
            if (n == 48) award(p, "level_bottom");
        }
        // случайные тревоги
        if (p.getRandom().nextInt(1200) == 0) {
            boolean airLow = d.getInt("stage") < ST_CHOICE && p.getRandom().nextBoolean();
            if (airLow) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 0, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0, false, false));
                say(p, "⚠ Давление падает. Воздух тяжёлый, ноги не слушаются — фильтры сдают.", ChatFormatting.RED);
            } else {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 400, 0, false, false));
                sound(p, SoundEvents.WARDEN_HEARTBEAT, 0.8F);
                say(p, "⚠ Перебой питания. Свет дрожит и меркнет — держитесь освещённых мест.", ChatFormatting.YELLOW);
            }
        }
    }

    private static void outsideTick(ServerPlayer p) {
        if (hasFullSuit(p)) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack stack = p.getItemBySlot(slot);
                stack.hurtAndBreak(1, p, e -> e.broadcastBreakEvent(slot));
            }
            int left = p.getItemBySlot(EquipmentSlot.CHEST).getMaxDamage() - p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
            if (left == 60 || left == 30 || left == 10) {
                p.displayClientMessage(T.t("⚠ Скафандр: осталось " + left + " сек воздуха", ChatFormatting.RED), true);
            }
        } else {
            p.hurt(p.damageSources().magic(), 2.0F);
            if (p.tickCount % 100 == 0) {
                say(p, "Горло горит от пепла. Вернитесь в люк или наденьте скафандр!", ChatFormatting.RED);
            }
        }
    }

    public static void debugTeleportLevel(ServerPlayer p, int level) {
        BlockPos b = SiloLayout.corridorPos(level);
        p.teleportTo(p.server.overworld(), b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0.0F, 0.0F);
    }
}
