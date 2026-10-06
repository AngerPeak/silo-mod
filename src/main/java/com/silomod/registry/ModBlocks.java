package com.silomod.registry;

import com.silomod.SiloMod;
import com.silomod.block.StoryBlock;
import com.silomod.block.TerminalBlock;
import com.silomod.story.Story;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SiloMod.MODID);

    private static BlockBehaviour.Properties unbreakable(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(-1.0F, 3600000.0F).sound(sound);
    }

    public static final RegistryObject<Block> TERMINAL = BLOCKS.register("terminal",
            () -> new TerminalBlock(unbreakable(MapColor.COLOR_GRAY, SoundType.METAL).lightLevel(s -> 7)));

    public static final RegistryObject<Block> SUPPLY_CRATE = BLOCKS.register("supply_crate",
            () -> new StoryBlock(unbreakable(MapColor.COLOR_BROWN, SoundType.WOOD), Story::openCrate));

    public static final RegistryObject<Block> EVIDENCE = BLOCKS.register("evidence",
            () -> new StoryBlock(unbreakable(MapColor.COLOR_YELLOW, SoundType.METAL).lightLevel(s -> 4), Story::examine));

    public static final RegistryObject<Block> AIRLOCK = BLOCKS.register("airlock",
            () -> new StoryBlock(unbreakable(MapColor.METAL, SoundType.IRON), Story::useAirlock));

    public static final RegistryObject<Block> MACHINE = BLOCKS.register("machine",
            () -> new StoryBlock(unbreakable(MapColor.COLOR_ORANGE, SoundType.COPPER), Story::useMachine));

    private ModBlocks() {}
}
