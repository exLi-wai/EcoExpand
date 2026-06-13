package com.lw.eco_expand.common.item.estorage;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.items.contents.CellConfig;
import appeng.items.contents.CellUpgrades;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageLevel;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import thaumicenergistics.api.storage.IAEEssentiaStack;
import thaumicenergistics.api.storage.IEssentiaStorageChannel;

import javax.annotation.Nonnull;

public class EStorageCellEssentia extends AbstractTypedEStorageCell<IAEEssentiaStack> {

    public static final EStorageCellEssentia LEVEL_L4 = new EStorageCellEssentia(DriveStorageLevel.A, 16, 4);
    public static final EStorageCellEssentia LEVEL_L6 = new EStorageCellEssentia(DriveStorageLevel.B, 64, 16);
    public static final EStorageCellEssentia LEVEL_L9 = new EStorageCellEssentia(DriveStorageLevel.C, 256, 64);

    public EStorageCellEssentia(final DriveStorageLevel level, final int millionBytes, final int byteMultiplier) {
        super("essentia", level, millionBytes, byteMultiplier);
    }

    @Override
    public int getTotalTypes(@Nonnull final ItemStack cellItem) {
        return 100;
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
