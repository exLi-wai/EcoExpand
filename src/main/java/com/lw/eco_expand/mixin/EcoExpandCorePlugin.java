package com.lw.eco_expand.mixin;

import com.lw.eco_expand.Tags;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class EcoExpandCorePlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME + " Early Mixins");
    private static final String MIXIN_CONFIG = "mixins.eco_expand_early.json";

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList(MIXIN_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(final String mixinConfig) {
        return MIXIN_CONFIG.equals(mixinConfig);
    }

    @Override
    public void onMixinConfigQueued(final String mixinConfig) {
        LOGGER.info("EcoExpand early mixin config {} queued", mixinConfig);
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(final Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
