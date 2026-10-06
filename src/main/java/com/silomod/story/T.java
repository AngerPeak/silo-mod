package com.silomod.story;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/** Мелкие помощники для текста. */
final class T {
    private T() {}

    static MutableComponent t(String s, ChatFormatting... f) {
        return Component.literal(s).withStyle(f);
    }

    static MutableComponent btn(String label, String command, String hover) {
        return Component.literal("[" + label + "]").withStyle(s -> s
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }
}
