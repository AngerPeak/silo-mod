package com.silomod.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.silomod.SiloMod;
import com.silomod.story.Journal;
import com.silomod.story.Story;
import com.silomod.world.SiloChunkGenerator;
import com.silomod.world.SiloLayout;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SiloMod.MODID)
public final class SiloEvents {
    private SiloEvents() {}

    /** Мир «Укрытие»: спавн — в жилой каюте на 20-м уровне. */
    @SubscribeEvent
    public static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.getChunkSource().getGenerator() instanceof SiloChunkGenerator) {
            event.getSettings().setSpawn(SiloLayout.startPos(), 0.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Story.ensureStarted(p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Story.onRespawn(p);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData();
        if (old.contains(Player.PERSISTED_NBT_TAG, 10)) {
            event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG,
                    old.getCompound(Player.PERSISTED_NBT_TAG).copy());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer p && p.tickCount % 20 == 0) {
            Story.secondTick(p);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Enemy && event.getSource().getEntity() instanceof ServerPlayer p) {
            Story.onKill(p);
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("silo")
                .then(Commands.literal("journal").executes(c -> {
                    Journal.open(c.getSource().getPlayerOrException(), InteractionHand.MAIN_HAND);
                    return 1;
                }))
                .then(Commands.literal("accuse")
                        .then(Commands.argument("who", IntegerArgumentType.integer(0, 3)).executes(c ->
                                Story.accuse(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "who")))))
                .then(Commands.literal("choose")
                        .then(Commands.literal("stay").executes(c -> Story.choose(c.getSource().getPlayerOrException(), "stay")))
                        .then(Commands.literal("leave").executes(c -> Story.choose(c.getSource().getPlayerOrException(), "leave"))))
                .then(Commands.literal("handin")
                        .then(Commands.argument("quest", StringArgumentType.word()).executes(c ->
                                Story.handin(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "quest")))))
                .then(Commands.literal("debug").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("stage").then(Commands.argument("n", IntegerArgumentType.integer(1, 7)).executes(c -> {
                            Story.data(c.getSource().getPlayerOrException()).putInt("stage", IntegerArgumentType.getInteger(c, "n"));
                            return 1;
                        })))
                        .then(Commands.literal("culprit").then(Commands.argument("n", IntegerArgumentType.integer(0, 3)).executes(c -> {
                            Story.data(c.getSource().getPlayerOrException()).putInt("culprit", IntegerArgumentType.getInteger(c, "n"));
                            return 1;
                        })))
                        .then(Commands.literal("level").then(Commands.argument("n", IntegerArgumentType.integer(1, 48)).executes(c -> {
                            Story.debugTeleportLevel(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"));
                            return 1;
                        }))))
        );
    }
}
