package com.lw.eco_expand.client;

import com.lw.eco_expand.Tags;
import com.lw.eco_expand.common.registry.RegistryItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ClientRegistry {

    @SubscribeEvent
    public static void registerModels(final ModelRegistryEvent event) {
        for (final Item item : RegistryItems.STORAGE_CELLS) {
            final ResourceLocation registryName = Objects.requireNonNull(item.getRegistryName());
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(registryName, "inventory"));
        }
    }
}
