package com.lw.eco_expand.mixin;

import com.lw.eco_expand.Tags;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

public class EcoExpandLateMixinLoader implements ILateMixinLoader {

    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME + " Mixins");
    private static final String MIXIN_CONFIG = "mixins.eco_expand.json";

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList(MIXIN_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(final String mixinConfig) {
        final boolean queue = MIXIN_CONFIG.equals(mixinConfig)
                && Loader.isModLoaded("ecoaeextension")
                && Loader.isModLoaded("crazyae")
                && Loader.isModLoaded("thaumicenergistics");
        LOGGER.info("EcoExpand mixin config {} queue={}", mixinConfig, queue);
        return queue;
    }

    @Override
    public void onMixinConfigQueued(final String mixinConfig) {
        LOGGER.info("EcoExpand mixin config {} queued", mixinConfig);
    }
}
