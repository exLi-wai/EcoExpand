package com.lw.eco_expand.mixin.early;

import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = FallbackResourceManager.class)
public interface MixinFallbackResourceManagerAccessor {

    @Accessor("resourcePacks")
    List<IResourcePack> ecoExpand$getResourcePacks();
}
