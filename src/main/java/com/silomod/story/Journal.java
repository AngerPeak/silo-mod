package com.silomod.story;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Журнал следователя: динамическая «книга» со статусом расследования. */
public final class Journal {
    private Journal() {}

    public static void open(ServerPlayer p, InteractionHand hand) {
        if (!Story.isSiloWorld(p.server)) {
            Story.say(p, "Журнал пуст: расследование идёт только в мире «Укрытие».", ChatFormatting.GRAY);
            return;
        }
        Story.ensureStarted(p);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "Журнал следователя");
        tag.putString("author", p.getGameProfile().getName());
        ListTag pages = new ListTag();
        for (Component page : buildPages(p)) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(page)));
        }
        tag.put("pages", pages);

        // Клиент берёт книгу из руки — временно подменяем предмет и возвращаем обратно.
        ItemStack original = p.getItemInHand(hand);
        try {
            p.setItemInHand(hand, book);
            p.containerMenu.broadcastChanges();
            p.connection.send(new ClientboundOpenBookPacket(hand));
        } finally {
            p.setItemInHand(hand, original);
            p.containerMenu.broadcastChanges();
        }
    }

    private static List<Component> buildPages(ServerPlayer p) {
        CompoundTag d = Story.data(p);
        List<Component> pages = new ArrayList<>();

        int n = Story.inSilo(p) ? Story.levelOf(p) : 0;
        MutableComponent first = Component.literal("ЖУРНАЛ СЛЕДОВАТЕЛЯ\n").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED);
        first.append(Component.literal("Укрытие-17\n\n").withStyle(ChatFormatting.BLACK));
        first.append(Component.literal(n > 0 ? "Вы на уровне " + n + "\n(" + com.silomod.world.SiloLayout.department(n) + ")\n\n"
                : "Вы снаружи.\n\n").withStyle(ChatFormatting.DARK_GRAY));
        first.append(Component.literal("Дело: смерть главного инженера Ильи Роста, 48-й уровень.").withStyle(ChatFormatting.BLACK));
        pages.add(first);

        MutableComponent goal = Component.literal("ТЕКУЩАЯ ЦЕЛЬ\n\n").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_BLUE);
        goal.append(Component.literal(Story.objective(p)).withStyle(ChatFormatting.BLACK));
        pages.add(goal);

        MutableComponent clues = Component.literal("УЛИКИ\n\n").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GREEN);
        int bits = d.getInt("clues");
        int culprit = d.getInt("culprit");
        if (d.getInt("stage") < Story.ST_CLUES) {
            clues.append(Component.literal("Пока ничего. Сначала осмотрите тело Роста.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            for (int i = 0; i < 4; i++) {
                boolean has = (bits & (1 << i)) != 0;
                String line = has ? "✔ " + Texts.CLUE_SHORT[i][culprit == i ? 0 : 1] : "✘ " + Texts.SUSPECT_SHORT[i] + " — не найдено";
                clues.append(Component.literal(line + "\n\n").withStyle(has ? ChatFormatting.BLACK : ChatFormatting.DARK_GRAY));
            }
        }
        pages.add(clues);

        MutableComponent sus = Component.literal("ПОДОЗРЕВАЕМЫЕ\n\n").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE);
        for (int i = 0; i < 4; i++) {
            sus.append(Component.literal("• " + Texts.SUSPECT_FULL[i] + "\n").withStyle(ChatFormatting.BLACK));
        }
        if (d.getInt("stage") >= Story.ST_ACCUSE) {
            sus.append(Component.literal("\nВремя смерти — около 03:10.").withStyle(ChatFormatting.DARK_GRAY));
        }
        pages.add(sus);

        MutableComponent side = Component.literal("ЗАДАНИЯ ОТДЕЛОВ\n\n").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
        side.append(line("Нормы выработки (ур. 41–47)", d.getBoolean("side_iron")));
        side.append(line("Урожай для уровня (31–40)", d.getBoolean("side_farm")));
        side.append(line("Аптечные запасы (25–30)", d.getBoolean("side_medic")));
        side.append(line("Крысы в вентиляции (3–6): " + Math.min(12, d.getInt("kills")) + "/12", d.getBoolean("side_rats")));
        pages.add(side);

        MutableComponent tips = Component.literal("ВЫЖИВАНИЕ\n\n").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED);
        tips.append(Component.literal("• Ящики (ПКМ) выдают паёк раз в 5 минут.\n• Терминал — на каждом уровне у лестницы.\n"
                + "• Руды — в породе за стеной.\n• Уровни 45–47 заброшены: там темно и опасно.\n• Снаружи — яд.").withStyle(ChatFormatting.BLACK));
        pages.add(tips);
        return pages;
    }

    private static Component line(String s, boolean done) {
        return Component.literal((done ? "✔ " : "☐ ") + s + "\n\n").withStyle(done ? ChatFormatting.DARK_GREEN : ChatFormatting.BLACK);
    }
}
