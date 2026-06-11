package com.lw.eco_expand.common.item.estorage;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.items.contents.CellConfig;
import appeng.items.contents.CellUpgrades;
import com.lw.eco_expand.Tags;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageLevel;
import github.kasuminova.ecoaeextension.common.item.estorage.EStorageCell;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import thaumicenergistics.api.storage.IAEEssentiaStack;
import thaumicenergistics.api.storage.IEssentiaStorageChannel;

import javax.annotation.Nonnull;

public class EStorageCellEssentia extends EStorageCell<IAEEssentiaStack> {

    public static final EStorageCellEssentia LEVEL_L4 = new EStorageCellEssentia(DriveStorageLevel.A, 16, 4);
    public static final EStorageCellEssentia LEVEL_L6 = new EStorageCellEssentia(DriveStorageLevel.B, 64, 16);
    public static final EStorageCellEssentia LEVEL_L9 = new EStorageCellEssentia(DriveStorageLevel.C, 256, 64);

    public EStorageCellEssentia(final DriveStorageLevel level, final int millionBytes, final int byteMultiplier) {
        super(level, millionBytes, byteMultiplier);
        setRegistryName(new ResourceLocation(Tags.MOD_ID, "estorage_cell_essentia_" + millionBytes + "m"));
        setTranslationKey(Tags.MOD_ID + '.' + "estorage_cell_essentia_" + millionBytes + "m");
    }

    @Override
    public int getTotalTypes(@Nonnull final ItemStack cellItem) {
        return 64;
    }

    @Override
    public int getBytesPerType(@Nonnull final ItemStack cellItem) {
        return byteMultiplier * 1024;
    }

    @Nonnull
    @Override
    public IItemHandler getUpgradesInventory(final ItemStack is) {
        return new CellUpgrades(is, 0);
    }

    @Nonnull
    @Override
    public IItemHandler getConfigInventory(final ItemStack is) {
        return new CellConfig(is);
    }

    @Nonnull
    @Override
    public IStorageChannel<IAEEssentiaStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IEssentiaStorageChannel.class);
    }
}
