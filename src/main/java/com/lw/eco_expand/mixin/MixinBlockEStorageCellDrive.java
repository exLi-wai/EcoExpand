package com.lw.eco_expand.mixin;

import appeng.api.AEApi;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import com.lw.eco_expand.ECO_Expand;
import com.lw.eco_expand.common.item.estorage.EStorageCellEnergy;
import com.lw.eco_expand.common.item.estorage.EStorageCellEssentia;
import com.lw.eco_expand.common.item.estorage.EStorageCellMana;
import com.lw.eco_expand.common.item.estorage.EStorageCellUniversal;
import com.lw.eco_expand.common.estorage.universal.UniversalStorageStats;
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

import java.util.HashSet;
import java.util.Set;

@Mixin(value = BlockEStorageCellDrive.class, remap = false)
public abstract class MixinBlockEStorageCellDrive {

    @Unique
    private static final Set<String> LOGGED_RENDER_STATES = new HashSet<>();

    @Inject(method = "func_176221_a", at = @At("HEAD"), cancellable = true, require = 1)
    private void ecoExpand$getActualStateSrg(final IBlockState state, final IBlockAccess world, final BlockPos pos, final CallbackInfoReturnable<IBlockState> cir) {
        final TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof EStorageCellDrive)) {
            return;
        }

        final EStorageCellDrive drive = (EStorageCellDrive) te;
        final ItemStack stack = drive.getDriveInv().getStackInSlot(0);
        final Item item = stack.getItem();
        if (!(item instanceof EStorageCellEnergy)
                && !(item instanceof EStorageCellMana)
                && !(item instanceof EStorageCellEssentia)
                && !(item instanceof EStorageCellUniversal)) {
            return;
        }

        final EStorageCell<?> cell = (EStorageCell<?>) item;
        final DriveStorageType displayType = ECO_Expand$getDisplayType(item);
        final DriveStorageCapacity capacity = ECO_Expand$getCapacity(stack, drive, item);
        final IBlockState renderedState = state.withProperty(DriveStorageLevel.STORAGE_LEVEL, cell.getLevel())
                .withProperty(DriveStorageType.STORAGE_TYPE, displayType)
                .withProperty(DriveStatus.STATUS, drive.isWriting() ? DriveStatus.RUN : DriveStatus.IDLE)
                .withProperty(DriveStorageCapacity.STORAGE_CAPACITY, capacity);
        ECO_Expand$logOnce("render:" + item.getRegistryName() + ":" + cell.getLevel() + ":" + displayType + ":" + capacity,
                "EStorage drive render: custom cell {} mapped to level={}, type={}, capacity={}, pos={}",
                item.getRegistryName(), cell.getLevel(), displayType, capacity, pos);
        cir.setReturnValue(renderedState);
    }

    @Unique
    private static ICellInventoryHandler<?> ECO_Expand$getCellInventory(final ItemStack stack, final EStorageCellDrive drive) {
        final EStorageCellHandler handler = EStorageCellHandler.getHandler(stack);
        if (handler == null) {
            return null;
        }

        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            final ICellInventoryHandler<?> cellInventory = ECO_Expand$getCellInventory(handler, stack, drive, channel);
            if (cellInventory != null) {
                return cellInventory;
            }
        }
        return null;
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ICellInventoryHandler<?> ECO_Expand$getCellInventory(final EStorageCellHandler handler,
                                                                       final ItemStack stack,
                                                                       final EStorageCellDrive drive,
                                                                       final IStorageChannel<? extends IAEStack<?>> channel) {
        return handler.getCellInventory(stack, drive, (IStorageChannel) channel);
    }

    @Unique
    private static DriveStorageType ECO_Expand$getDisplayType(final Item item) {
        if (item instanceof EStorageCellEssentia) {
            return DriveStorageType.FLUID;
        }
        return DriveStorageType.ITEM;
    }

    @Unique
    private static DriveStorageCapacity ECO_Expand$getCapacity(final ItemStack stack,
                                                              final EStorageCellDrive drive,
                                                              final Item item) {
        if (item instanceof EStorageCellUniversal) {
            final EStorageCellUniversal universal = (EStorageCellUniversal) item;
            final UniversalStorageStats stats = universal.getStats(stack, drive.getWorld());
            if (stats.types() <= 0) {
                return DriveStorageCapacity.EMPTY;
            }
            if (stats.usedBytes() >= universal.getBytes(stack)) {
                return DriveStorageCapacity.FULL;
            }
            if (stats.types() >= universal.getTotalTypes(stack)) {
                return DriveStorageCapacity.TYPE_MAX;
            }
            return DriveStorageCapacity.EMPTY;
        }

        final ICellInventoryHandler<?> cellInventory = ECO_Expand$getCellInventory(stack, drive);
        if (cellInventory == null) {
            ECO_Expand$logOnce("no_inventory:" + item.getRegistryName(),
                    "EStorage drive render: custom cell {} has no cell inventory. stack={}",
                    item.getRegistryName(), stack);
            return DriveStorageCapacity.EMPTY;
        }
        return EStorageCellDrive.getCapacity(cellInventory);
    }

    @Unique
    private static void ECO_Expand$logOnce(final String key, final String message, final Object... args) {
        if (LOGGED_RENDER_STATES.add(key)) {
            ECO_Expand.LOGGER.info(message, args);
        }
    }
}
