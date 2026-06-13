package com.lw.eco_expand.client;

import com.lw.eco_expand.Tags;
import com.lw.eco_expand.ECO_Expand;
import com.lw.eco_expand.common.registry.RegistryItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ClientRegistry {

    private static boolean registeredReloadListener = false;

    @SubscribeEvent
    public static void registerModels(final ModelRegistryEvent event) {
        registerEcoAeResourcePriorityReloadListener();
        prioritizeEcoExpandEcoAeResources();
        for (final Item item : RegistryItems.STORAGE_CELLS) {
            final ResourceLocation registryName = Objects.requireNonNull(item.getRegistryName());
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(registryName, "inventory"));
        }
    }

    @SubscribeEvent
    public static void onTextureStitchPre(final TextureStitchEvent.Pre event) {
        prioritizeEcoExpandEcoAeResources();
    }

    private static void registerEcoAeResourcePriorityReloadListener() {
        if (registeredReloadListener) {
            return;
        }

        final IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if (!(resourceManager instanceof IReloadableResourceManager)) {
            ECO_Expand.LOGGER.warn("Cannot register ECO AE resource priority listener: unexpected resource manager {}",
                    resourceManager.getClass().getName());
            return;
        }

        ((IReloadableResourceManager) resourceManager).registerReloadListener(manager -> prioritizeEcoExpandEcoAeResources());
        registeredReloadListener = true;
    }

    @SubscribeEvent
    public static void onModelBake(final ModelBakeEvent event) {
        prioritizeEcoExpandEcoAeResources();
    }

    @SuppressWarnings("unchecked")
    private static void prioritizeEcoExpandEcoAeResources() {
        final IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if (!(resourceManager instanceof SimpleReloadableResourceManager)) {
            ECO_Expand.LOGGER.warn("Cannot prioritize ECO AE resources: unexpected resource manager {}",
                    resourceManager.getClass().getName());
            return;
        }

        try {
            final Field domainManagersField = SimpleReloadableResourceManager.class
                    .getDeclaredField("domainResourceManagers");
            domainManagersField.setAccessible(true);
            final Map<String, FallbackResourceManager> domainManagers =
                    (Map<String, FallbackResourceManager>) domainManagersField.get(resourceManager);
            final FallbackResourceManager fallback = domainManagers.get("ecoaeextension");
            if (fallback == null) {
                ECO_Expand.LOGGER.warn("Cannot prioritize ECO AE resources: ecoaeextension domain is not loaded yet");
                return;
            }

            final Field resourcePacksField = FallbackResourceManager.class.getDeclaredField("resourcePacks");
            resourcePacksField.setAccessible(true);
            final List<IResourcePack> resourcePacks = (List<IResourcePack>) resourcePacksField.get(fallback);
            final List<IResourcePack> ecoExpandPacks = new ArrayList<>();
            for (int i = resourcePacks.size() - 1; i >= 0; i--) {
                final IResourcePack pack = resourcePacks.get(i);
                if (pack.getPackName().contains(Tags.MOD_NAME)) {
                    ecoExpandPacks.add(0, resourcePacks.remove(i));
                }
            }

            if (ecoExpandPacks.isEmpty()) {
                ECO_Expand.LOGGER.warn("Cannot prioritize ECO AE resources: {} resource pack was not found", Tags.MOD_NAME);
                return;
            }

            resourcePacks.addAll(ecoExpandPacks);
            ECO_Expand.LOGGER.info("Prioritized ECO Expand resources for ecoaeextension domain");
        } catch (final ReflectiveOperationException | ClassCastException e) {
            ECO_Expand.LOGGER.error("Failed to prioritize ECO AE resources", e);
        }
    }
}
