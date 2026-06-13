package com.lw.eco_expand.common.item.estorage;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import dev.beecube31.crazyae2.core.CrazyAESidedHandler;
import dev.beecube31.crazyae2.core.api.storage.energy.IEnergyStorageChannel;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageLevel;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class EStorageCellEnergy extends AbstractTypedEStorageCell<IAEItemStack> {

    public static final EStorageCellEnergy LEVEL_L4 = new EStorageCellEnergy(DriveStorageLevel.A, 16, 4);
    public static final EStorageCellEnergy LEVEL_L6 = new EStorageCellEnergy(DriveStorageLevel.B, 64, 16);
    public static final EStorageCellEnergy LEVEL_L9 = new EStorageCellEnergy(DriveStorageLevel.C, 256, 64);

    public EStorageCellEnergy(final DriveStorageLevel level, final int millionBytes, final int byteMultiplier) {
        super("energy", level, millionBytes, byteMultiplier);
    }

    @Override
    public int getTotalTypes(@Nonnull final ItemStack cellItem) {
        return Math.max(1, CrazyAESidedHandler.availableEnergyTypes.size());
    }

    @Nonnull
    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IEnergyStorageChannel.class);
    }
}
