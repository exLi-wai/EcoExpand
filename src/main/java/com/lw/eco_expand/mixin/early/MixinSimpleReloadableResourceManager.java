package com.lw.eco_expand.mixin.early;

import com.lw.eco_expand.ECO_Expand;
import com.lw.eco_expand.Tags;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = SimpleReloadableResourceManager.class)
public abstract class MixinSimpleReloadableResourceManager {

    @Shadow
    @Final
    private Map<String, FallbackResourceManager> domainResourceManagers;

    @Inject(method = "reloadResources", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/SimpleReloadableResourceManager;notifyReloadListeners()V"))
    private void ecoExpand$prioritizeEcoAeResources(final List<IResourcePack> resourcesPacksList, final CallbackInfo ci) {
        final FallbackResourceManager fallback = domainResourceManagers.get("ecoaeextension");
        if (fallback == null) {
            return;
        }

        try {
            final List<IResourcePack> resourcePacks =
                    ((MixinFallbackResourceManagerAccessor) fallback).ecoExpand$getResourcePacks();
            final List<IResourcePack> ecoExpandPacks = new ArrayList<>();
            for (int i = resourcePacks.size() - 1; i >= 0; i--) {
                final IResourcePack pack = resourcePacks.get(i);
                if (pack.getPackName().contains(Tags.MOD_NAME)) {
                    ecoExpandPacks.add(0, resourcePacks.remove(i));
                }
            }

            if (!ecoExpandPacks.isEmpty()) {
                resourcePacks.addAll(ecoExpandPacks);
                ECO_Expand.LOGGER.info("Prioritized ECO Expand resources for ecoaeextension domain before reload listeners");
            }
        } catch (final ClassCastException e) {
            ECO_Expand.LOGGER.error("Failed to prioritize ECO AE resources during resource reload", e);
        }
    }
}
