package com.lw.eco_expand.common.item.estorage;

import appeng.api.storage.data.IAEStack;
import com.lw.eco_expand.Tags;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageLevel;
import github.kasuminova.ecoaeextension.common.item.estorage.EStorageCell;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public abstract class AbstractTypedEStorageCell<T extends IAEStack<T>> extends EStorageCell<T> {

    protected AbstractTypedEStorageCell(final String typeName, final DriveStorageLevel level, final int millionBytes, final int byteMultiplier) {
        super(level, millionBytes, byteMultiplier);
        final String itemName = "estorage_cell_" + typeName + "_" + millionBytes + "m";
        setRegistryName(new ResourceLocation(Tags.MOD_ID, itemName));
        setTranslationKey(Tags.MOD_ID + '.' + itemName);
    }

    @Override
    public int getBytesPerType(@Nonnull final ItemStack cellItem) {
        return byteMultiplier * 1024;
    }
}
