package com.lw.eco_expand.common.item.estorage;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import com.lw.eco_expand.Tags;
import dev.beecube31.crazyae2.core.CrazyAESidedHandler;
import dev.beecube31.crazyae2.core.api.storage.energy.IEnergyStorageChannel;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageLevel;
import github.kasuminova.ecoaeextension.common.item.estorage.EStorageCell;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class EStorageCellEnergy extends EStorageCell<IAEItemStack> {

    public static final EStorageCellEnergy LEVEL_L4 = new EStorageCellEnergy(DriveStorageLevel.A, 16, 4);
    public static final EStorageCellEnergy LEVEL_L6 = new EStorageCellEnergy(DriveStorageLevel.B, 64, 16);
    public static final EStorageCellEnergy LEVEL_L9= new EStorageCellEnergy(DriveStorageLevel.C, 256, 64);

    public EStorageCellEnergy(final DriveStorageLevel level, final int millionBytes, final int byteMultiplier) {
        super(level, millionBytes, byteMultiplier);
        setRegistryName(new ResourceLocation(Tags.MOD_ID, "estorage_cell_energy_" + millionBytes + "m"));
        setTranslationKey(Tags.MOD_ID + '.' + "estorage_cell_energy_" + millionBytes + "m");
    }

    @Override
    public int getTotalTypes(@Nonnull final ItemStack cellItem) {
        return Math.max(1, CrazyAESidedHandler.availableEnergyTypes.size());
    }

    @Override
    public int getBytesPerType(@Nonnull final ItemStack cellItem) {
        return byteMultiplier * 1024;
    }

    @Nonnull
    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IEnergyStorageChannel.class);
    }
}
