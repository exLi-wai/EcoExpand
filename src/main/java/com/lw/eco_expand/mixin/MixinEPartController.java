package com.lw.eco_expand.mixin;

import github.kasuminova.ecoaeextension.common.tile.ecotech.EPartController;
import github.kasuminova.mmce.common.world.MachineComponentManager;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(value = EPartController.class, remap = false)
public abstract class MixinEPartController {

    @Redirect(method = "func_145843_s",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    private void ECO_Expand$removeOwnersCompat(final Map<?, ?> foundComponents, final BiConsumer<?, ?> originalConsumer) {
        foundComponents.forEach((key, value) -> {
            if (key instanceof TileEntity && value instanceof ProcessingComponent) {
                MachineComponentManager.INSTANCE.removeOwner((TileEntity) key, ECO_Expand$self());
                return;
            }

            if (value instanceof Map) {
                ((Map<?, ?>) value).forEach((nestedKey, nestedValue) -> {
                    if (nestedKey instanceof TileEntity && nestedValue instanceof ProcessingComponent) {
                        MachineComponentManager.INSTANCE.removeOwner((TileEntity) nestedKey, ECO_Expand$self());
                    }
                });
            }
        });
    }

    @Unique
    private TileMultiblockMachineController ECO_Expand$self() {
        return (TileMultiblockMachineController) (Object) this;
    }
}
