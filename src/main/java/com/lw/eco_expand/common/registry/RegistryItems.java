package com.lw.eco_expand.common.registry;

import com.lw.eco_expand.ECO_Expand;
import com.lw.eco_expand.common.item.estorage.EStorageCellEnergy;
import com.lw.eco_expand.common.item.estorage.EStorageCellEssentia;
import com.lw.eco_expand.common.item.estorage.EStorageCellMana;
import com.lw.eco_expand.common.item.estorage.EStorageCellUniversal;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public final class RegistryItems {

    public static final Item[] STORAGE_CELLS = {
            EStorageCellEnergy.LEVEL_L4,
            EStorageCellEnergy.LEVEL_L6,
            EStorageCellEnergy.LEVEL_L9,
            EStorageCellMana.LEVEL_L4,
            EStorageCellMana.LEVEL_L6,
            EStorageCellMana.LEVEL_L9,
            EStorageCellEssentia.LEVEL_L4,
            EStorageCellEssentia.LEVEL_L6,
            EStorageCellEssentia.LEVEL_L9,
            EStorageCellUniversal.LEVEL_L4,
            EStorageCellUniversal.LEVEL_L6,
            EStorageCellUniversal.LEVEL_L9,
            EStorageCellUniversal.LEVEL_L12,
            EStorageCellUniversal.LEVEL_L15,
            EStorageCellUniversal.LEVEL_INFINITE
    };

    @SubscribeEvent
    public static void registerItems(final RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(STORAGE_CELLS);
        for (final Item item : STORAGE_CELLS) {
            ECO_Expand.LOGGER.info("Registered EStorage cell item: registry={}, translationKey={}",
                    item.getRegistryName(), item.getTranslationKey());
        }
    }
}
