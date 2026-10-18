package com.lw.eco_expand.mixin;

import com.lw.eco_expand.common.estorage.universal.UniversalStorageStats;
import com.lw.eco_expand.common.item.estorage.EStorageCellUniversal;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageType;
import github.kasuminova.ecoaeextension.common.container.data.EStorageCellData;
import github.kasuminova.ecoaeextension.common.tile.ecotech.estorage.EStorageCellDrive;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EStorageCellData.class, remap = false)
public abstract class MixinEStorageCellData {

    @Inject(method = "from", at = @At("HEAD"), cancellable = true)
    private static void ecoExpand$readUniversalCell(final EStorageCellDrive drive, final CallbackInfoReturnable<EStorageCellData> cir) {
        if (drive == null) {
            return;
        }

        final ItemStack stack = drive.getDriveInv().getStackInSlot(0);
        if (!(stack.getItem() instanceof EStorageCellUniversal)) {
            return;
        }

        final EStorageCellUniversal cell = (EStorageCellUniversal) stack.getItem();
        final UniversalStorageStats stats = cell.getStats(stack, drive.getWorld());
        cir.setReturnValue(new EStorageCellData(ecoExpand$getStorageType(cell), cell.getLevel(), stats.types(), stats.usedBytes()
        ));
    }

    @Unique
    private static DriveStorageType ecoExpand$getStorageType(final EStorageCellUniversal cell) {
        final String capacity = cell.getCapacityName();
        if ("16m".equals(capacity)) {
            return DriveStorageType.valueOf("UNIVERSAL_16M");
        }
        if ("64m".equals(capacity)) {
            return DriveStorageType.valueOf("UNIVERSAL_64M");
        }
        if ("256m".equals(capacity)) {
            return DriveStorageType.valueOf("UNIVERSAL_256M");
        }
        if ("1024m".equals(capacity)) {
            return DriveStorageType.valueOf("UNIVERSAL_1024M");
        }
        if ("4096m".equals(capacity)) {
            return DriveStorageType.valueOf("UNIVERSAL_4096M");
        }
        return DriveStorageType.valueOf("UNIVERSAL_INF");
    }
}
