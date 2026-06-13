package com.lw.eco_expand.common.estorage.universal;

import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.me.storage.BasicCellInventoryHandler;
import com.lw.eco_expand.common.item.estorage.EStorageCellUniversal;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public final class UniversalEStorageCellHandler {

    @Nullable
    public static <T extends IAEStack<T>> ICellInventoryHandler<T> getCellInventory(final ItemStack stack, final ISaveProvider saveProvider, final IStorageChannel<T> channel) {
        if (!(stack.getItem() instanceof EStorageCellUniversal)) {
            return null;
        }

        final String type = UniversalStorageDataManager.getTypeForChannel(channel);
        if (type == null) {
            return null;
        }

        final EStorageCellUniversal cell = (EStorageCellUniversal) stack.getItem();
        final UniversalEStorageCellInventory<T> inventory = new UniversalEStorageCellInventory<>(cell, stack, saveProvider, channel, type);
        return new BasicCellInventoryHandler<>(inventory, channel);
    }
}
