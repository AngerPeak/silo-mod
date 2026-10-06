package com.silomod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Универсальный «сюжетный» блок: по ПКМ вызывает действие на сервере. */
public class StoryBlock extends Block {
    @FunctionalInterface
    public interface Action {
        void run(ServerPlayer player, BlockPos pos);
    }

    private final Action action;

    public StoryBlock(Properties properties, Action action) {
        super(properties);
        this.action = action;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            action.run(sp, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
