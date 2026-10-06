package com.silomod;

import com.silomod.registry.ModBlocks;
import com.silomod.registry.ModItems;
import com.silomod.registry.ModTabs;
import com.silomod.registry.ModWorld;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SiloMod.MODID)
public class SiloMod {
    public static final String MODID = "silo";

    public SiloMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModTabs.TABS.register(bus);
        ModWorld.CHUNK_GENERATORS.register(bus);
    }
}
