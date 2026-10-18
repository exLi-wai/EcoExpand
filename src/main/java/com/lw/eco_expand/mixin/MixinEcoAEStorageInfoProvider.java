package com.lw.eco_expand.mixin;

import com.lw.eco_expand.common.item.estorage.EStorageCellUniversal;
import github.kasuminova.ecoaeextension.common.tile.ecotech.estorage.EStorageCellDrive;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = github.kasuminova.ecoaeextension.common.integration.theoneprobe.EStorageInfoProvider.class, remap = false)
public abstract class MixinEcoAEStorageInfoProvider {

    @Inject(method = "addProbeInfo", at = @At("HEAD"), cancellable = true)
    private void ecoExpand$skipUniversalDrive(final ProbeMode mode, final IProbeInfo probeInfo, final EntityPlayer player, final World world, final IBlockState blockState, final IProbeHitData data, final CallbackInfo ci) {
        if (world == null || data == null) {
            return;
        }

        final TileEntity tile = world.getTileEntity(data.getPos());
        if (!(tile instanceof EStorageCellDrive)) {
            return;
        }

        final ItemStack stack = ((EStorageCellDrive) tile).getDriveInv().getStackInSlot(0);
        if (stack.getItem() instanceof EStorageCellUniversal) {
            ci.cancel();
        }
    }
}
