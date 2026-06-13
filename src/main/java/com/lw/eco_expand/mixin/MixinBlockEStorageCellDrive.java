package com.lw.eco_expand.mixin;

import appeng.api.AEApi;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import com.lw.eco_expand.common.item.estorage.EStorageCellEnergy;
import com.lw.eco_expand.common.item.estorage.EStorageCellEssentia;
import com.lw.eco_expand.common.item.estorage.EStorageCellMana;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.BlockEStorageCellDrive;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStatus;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageCapacity;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageLevel;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageType;
import github.kasuminova.ecoaeextension.common.estorage.EStorageCellHandler;
import github.kasuminova.ecoaeextension.common.item.estorage.EStorageCell;
import github.kasuminova.ecoaeextension.common.tile.ecotech.estorage.EStorageCellDrive;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockEStorageCellDrive.class, remap = false)
public abstract class MixinBlockEStorageCellDrive {

    @Unique
    private static final String ECO_EXPAND$ENERGY_STORAGE_TYPE = "ENERGY";

    @Unique
    private static final String ECO_EXPAND$MANA_STORAGE_TYPE = "MANA";

    @Unique
    private static final String ECO_EXPAND$ESSENTIA_STORAGE_TYPE = "ESSENTIA";

    @Inject(method = "func_176221_a", at = @At("HEAD"), cancellable = true, require = 1)
    private void ecoExpand$getActualStateSrg(final IBlockState state,
                                             final IBlockAccess world,
                                             final BlockPos pos,
                                             final CallbackInfoReturnable<IBlockState> cir) {
        final TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof EStorageCellDrive)) {
            return;
        }

        final EStorageCellDrive drive = (EStorageCellDrive) te;
        final ItemStack stack = drive.getDriveInv().getStackInSlot(0);
        final Item item = stack.getItem();
        if (!(item instanceof EStorageCellEnergy)
                && !(item instanceof EStorageCellMana)
                && !(item instanceof EStorageCellEssentia)) {
            return;
        }

        final ICellInventoryHandler<?> cellInventory = ecoExpand$getCellInventory(stack, drive);
        if (cellInventory == null) {
            return;
        }

        final EStorageCell<?> cell = (EStorageCell<?>) item;
        final DriveStorageType displayType = ecoExpand$getDisplayType(item);
        final DriveStorageCapacity capacity = EStorageCellDrive.getCapacity(cellInventory);
        final IBlockState renderedState = state.withProperty(DriveStorageLevel.STORAGE_LEVEL, cell.getLevel())
                .withProperty(DriveStorageType.STORAGE_TYPE, displayType)
                .withProperty(DriveStatus.STATUS, drive.isWriting() ? DriveStatus.RUN : DriveStatus.IDLE)
                .withProperty(DriveStorageCapacity.STORAGE_CAPACITY, capacity);
        cir.setReturnValue(renderedState);
    }

    private static ICellInventoryHandler<?> ecoExpand$getCellInventory(final ItemStack stack, final EStorageCellDrive drive) {
        final EStorageCellHandler handler = EStorageCellHandler.getHandler(stack);
        if (handler == null) {
            return null;
        }

        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            final ICellInventoryHandler<?> cellInventory = ecoExpand$getCellInventory(handler, stack, drive, channel);
            if (cellInventory != null) {
                return cellInventory;
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ICellInventoryHandler<?> ecoExpand$getCellInventory(final EStorageCellHandler handler,
                                                                       final ItemStack stack,
                                                                       final EStorageCellDrive drive,
                                                                       final IStorageChannel<? extends IAEStack<?>> channel) {
        return handler.getCellInventory(stack, drive, (IStorageChannel) channel);
    }

    private static DriveStorageType ecoExpand$getDisplayType(final Item item) {
        if (item instanceof EStorageCellEssentia) {
            return DriveStorageType.valueOf(ECO_EXPAND$ESSENTIA_STORAGE_TYPE);
        }
        if (item instanceof EStorageCellEnergy) {
            return DriveStorageType.valueOf(ECO_EXPAND$ENERGY_STORAGE_TYPE);
        }
        if (item instanceof EStorageCellMana) {
            return DriveStorageType.valueOf(ECO_EXPAND$MANA_STORAGE_TYPE);
        }
        return DriveStorageType.ITEM;
    }
}
