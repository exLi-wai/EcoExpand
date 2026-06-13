package com.lw.eco_expand.common.item.estorage;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import dev.beecube31.crazyae2.core.CrazyAE;
import dev.beecube31.crazyae2.core.api.storage.IManaStorageChannel;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageLevel;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class EStorageCellMana extends AbstractTypedEStorageCell<IAEItemStack> {

    public static final EStorageCellMana LEVEL_L4 = new EStorageCellMana(DriveStorageLevel.A, 16, 4);
    public static final EStorageCellMana LEVEL_L6 = new EStorageCellMana(DriveStorageLevel.B, 64, 16);
    public static final EStorageCellMana LEVEL_L9 = new EStorageCellMana(DriveStorageLevel.C, 256, 64);

    public EStorageCellMana(final DriveStorageLevel level, final int millionBytes, final int byteMultiplier) {
        super("mana", level, millionBytes, byteMultiplier);
    }

    @Override
    public int getTotalTypes(@Nonnull final ItemStack cellItem) {
        return 1;
    }

    @Override
    public boolean isBlackListed(@Nonnull final ItemStack cellItem, @Nonnull final IAEItemStack requestedAddition) {
        return !CrazyAE.definitions().items().manaAsAEStack().isSameAs(requestedAddition.asItemStackRepresentation());
    }

    @Nonnull
    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IManaStorageChannel.class);
    }
}
