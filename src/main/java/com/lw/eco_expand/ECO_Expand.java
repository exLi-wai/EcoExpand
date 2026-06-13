package com.lw.eco_expand;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class ECO_Expand {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (!Loader.isModLoaded("theoneprobe")) {
            return;
        }

        try {
            Class.forName("com.lw.eco_expand.common.integration.top.IntegrationTOP")
                    .getMethod("registerProvider")
                    .invoke(null);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to register The One Probe integration.", e);
        }
    }

}
