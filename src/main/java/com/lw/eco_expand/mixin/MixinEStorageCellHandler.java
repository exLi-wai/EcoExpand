package com.lw.eco_expand.mixin;

import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import com.lw.eco_expand.common.estorage.universal.UniversalEStorageCellHandler;
import com.lw.eco_expand.common.item.estorage.EStorageCellUniversal;
import github.kasuminova.ecoaeextension.common.estorage.EStorageCellHandler;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EStorageCellHandler.class, remap = false)
public abstract class MixinEStorageCellHandler {

    @Inject(method = "getCellInventory", at = @At("HEAD"), cancellable = true)
    private <T extends IAEStack<T>> void ECO_Expand$getUniversalCellInventory(final ItemStack is, final ISaveProvider host, final IStorageChannel<T> channel, final CallbackInfoReturnable<ICellInventoryHandler<T>> cir) {
        if (is.getItem() instanceof EStorageCellUniversal) {
            cir.setReturnValue(UniversalEStorageCellHandler.getCellInventory(is, host, channel));
        }
    }
}
