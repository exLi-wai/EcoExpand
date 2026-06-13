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
    private static final String ECO_EXPAND$ENERGY_STORAGE_TYPE = "ENERGY";

    @Unique
    private static final String ECO_EXPAND$MANA_STORAGE_TYPE = "MANA";

    @Unique
    private static final String ECO_EXPAND$ESSENTIA_STORAGE_TYPE = "ESSENTIA";

    @Unique
    private static final String ECO_EXPAND$UNIVERSAL_16M_STORAGE_TYPE = "UNIVERSAL_16M";

    @Unique
    private static final String ECO_EXPAND$UNIVERSAL_64M_STORAGE_TYPE = "UNIVERSAL_64M";

    @Unique
    private static final String ECO_EXPAND$UNIVERSAL_256M_STORAGE_TYPE = "UNIVERSAL_256M";

    @Unique
    private static final String ECO_EXPAND$UNIVERSAL_1024M_STORAGE_TYPE = "UNIVERSAL_1024M";

    @Unique
    private static final String ECO_EXPAND$UNIVERSAL_2048M_STORAGE_TYPE = "UNIVERSAL_2048M";

    @Unique
    private static final String ECO_EXPAND$UNIVERSAL_INF_STORAGE_TYPE = "UNIVERSAL_INF";

    @Unique
    private static final Set<String> ECO_EXPAND$LOGGED_RENDER_STATES = new HashSet<>();

    @Inject(method = "func_176221_a", at = @At("RETURN"), cancellable = true, require = 1)
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
        final IBlockState baseState = cir.getReturnValue() == null ? state : cir.getReturnValue();
        final IBlockState renderedState = baseState.withProperty(DriveStorageLevel.STORAGE_LEVEL, cell.getLevel())
                .withProperty(DriveStorageType.STORAGE_TYPE, displayType)
                .withProperty(DriveStatus.STATUS, drive.isWriting() ? DriveStatus.RUN : DriveStatus.IDLE)
                .withProperty(DriveStorageCapacity.STORAGE_CAPACITY, capacity);
        ECO_Expand$logRenderState(item, cell, displayType, capacity, pos);
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
            return DriveStorageType.valueOf(ECO_EXPAND$ESSENTIA_STORAGE_TYPE);
        }
        if (item instanceof EStorageCellEnergy) {
            return DriveStorageType.valueOf(ECO_EXPAND$ENERGY_STORAGE_TYPE);
        }
        if (item instanceof EStorageCellMana) {
            return DriveStorageType.valueOf(ECO_EXPAND$MANA_STORAGE_TYPE);
        }
        if (item instanceof EStorageCellUniversal) {
            final EStorageCellUniversal universal = (EStorageCellUniversal) item;
            if ("16m".equals(universal.getCapacityName())) {
                return DriveStorageType.valueOf(ECO_EXPAND$UNIVERSAL_16M_STORAGE_TYPE);
            }
            if ("64m".equals(universal.getCapacityName())) {
                return DriveStorageType.valueOf(ECO_EXPAND$UNIVERSAL_64M_STORAGE_TYPE);
            }
            if ("256m".equals(universal.getCapacityName())) {
                return DriveStorageType.valueOf(ECO_EXPAND$UNIVERSAL_256M_STORAGE_TYPE);
            }
            if ("1024m".equals(universal.getCapacityName())) {
                return DriveStorageType.valueOf(ECO_EXPAND$UNIVERSAL_1024M_STORAGE_TYPE);
            }
            if ("2048m".equals(universal.getCapacityName())) {
                return DriveStorageType.valueOf(ECO_EXPAND$UNIVERSAL_2048M_STORAGE_TYPE);
            }
            if ("inf".equals(universal.getCapacityName())) {
                return DriveStorageType.valueOf(ECO_EXPAND$UNIVERSAL_INF_STORAGE_TYPE);
            }
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
            if (stats.usedBytes() >= universal.getTotalBytesLong(stack)) {
                return DriveStorageCapacity.FULL;
            }
            if (stats.types() >= universal.getTotalTypes(stack)) {
                return DriveStorageCapacity.TYPE_MAX;
            }
            return DriveStorageCapacity.EMPTY;
        }

        final ICellInventoryHandler<?> cellInventory = ECO_Expand$getCellInventory(stack, drive);
        if (cellInventory == null) {
            return DriveStorageCapacity.EMPTY;
        }
        return EStorageCellDrive.getCapacity(cellInventory);
    }

    @Unique
    private static void ECO_Expand$logRenderState(final Item item,
                                                 final EStorageCell<?> cell,
                                                 final DriveStorageType displayType,
                                                 final DriveStorageCapacity capacity,
                                                 final BlockPos pos) {
        final String key = item.getRegistryName() + ":" + cell.getLevel() + ":" + displayType + ":" + capacity;
        if (ECO_EXPAND$LOGGED_RENDER_STATES.add(key)) {
            ECO_Expand.LOGGER.info("EStorage drive render mapped custom cell {} to level={}, type={}, capacity={}, pos={}",
                    item.getRegistryName(), cell.getLevel(), displayType, capacity, pos);
        }
    }
}
