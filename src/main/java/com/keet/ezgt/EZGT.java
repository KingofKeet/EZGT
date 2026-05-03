package com.keet.ezgt;

import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "ezgt", version = "1.0.0", name = "EZGT", acceptedMinecraftVersions = "[1.7.10]")
public class EZGT {

    public static final String MODID = "ezgt";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.registerConfigClasses();
        MinecraftForge.EVENT_BUS.register(new EventsHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new EventsHandler());
    }
}
