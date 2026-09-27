package com.lw.eco_expand.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import com.lw.eco_expand.ECO_Expand;
import github.kasuminova.ecoaeextension.common.estorage.ECellDriveWatcher;
import github.kasuminova.ecoaeextension.common.tile.ecotech.estorage.EStorageCellDrive;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicenergistics.api.storage.IEssentiaStorageChannel;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

import net.minecraft.tileentity.TileEntity;

@Mixin(value = ECellDriveWatcher.class, remap = false)
public abstract class MixinECellDriveWatcher {

    @Unique
    private static final Map<EStorageCellDrive, Long> LAST_ESSENTIA_REFRESH_TICKS = new WeakHashMap<>();

    @Shadow
    @Final
    protected EStorageCellDrive drive;

    @Inject(method = "injectItems", at = @At("RETURN"))
    @Unique
    private void ecoExpand$refreshEssentiaAfterInject(final IAEStack<?> input, final Actionable type, final IActionSource src, final CallbackInfoReturnable<IAEStack<?>> cir) {
        if (type != Actionable.MODULATE || input == null) {
            return;
        }

        final IAEStack<?> remainder = cir.getReturnValue();
        if (remainder != null && remainder.getStackSize() >= input.getStackSize()) {
            return;
        }
        ecoExpand$refreshEssentiaCellArray();
    }

    @Inject(method = "extractItems", at = @At("RETURN"))
    @Unique
    private void ecoExpand$refreshEssentiaAfterExtract(final IAEStack<?> request, final Actionable type, final IActionSource src, final CallbackInfoReturnable<IAEStack<?>> cir) {
        final IAEStack<?> extracted = cir.getReturnValue();
        if (type != Actionable.MODULATE
                || extracted == null
                || extracted.getStackSize() <= 0) {
            return;
        }
        ecoExpand$refreshEssentiaCellArray();
    }

    @Redirect(
            method = {"injectItems", "extractItems"},
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/api/networking/storage/IStorageGrid;postAlterationOfStoredItems(Lappeng/api/storage/IStorageChannel;Ljava/lang/Iterable;Lappeng/api/networking/security/IActionSource;)V"
            )
    )
    @Unique
    private void ecoExpand$guardEmptyNetworkChange(final IStorageGrid storageGrid, final IStorageChannel<?> type, final Iterable<? extends IAEStack<?>> changes,final IActionSource src) {
        try {
            storageGrid.postAlterationOfStoredItems(type, changes, src);
        } catch (final NoSuchElementException exception) {
            ECO_Expand.LOGGER.debug("Skipped an empty AE storage change during a reentrant transfer.", exception);
        }
    }

    @Unique
    private void ecoExpand$refreshEssentiaCellArray() {
        final IStorageChannel<?> channel = ((IMEInventoryHandler<?>) this).getChannel();
        if (!(channel instanceof IEssentiaStorageChannel)) {
            return;
        }

        try {
            final TileEntity driveTile = (TileEntity) (Object) drive;
            if (!driveTile.hasWorld() || driveTile.getWorld().isRemote) {
                return;
            }

            final long currentTick = driveTile.getWorld().getTotalWorldTime();
            synchronized (LAST_ESSENTIA_REFRESH_TICKS) {
                final Long lastTick = LAST_ESSENTIA_REFRESH_TICKS.get(drive);
                if (lastTick != null && lastTick == currentTick) {
                    return;
                }
                LAST_ESSENTIA_REFRESH_TICKS.put(drive, currentTick);
            }

            final Object controller = drive.getController();
            if (controller == null) {
                return;
            }

            final Object channelPart = ecoExpand$invoke(controller, "getChannel");
            final Object proxy = ecoExpand$invoke(channelPart, "getProxy");
            if (proxy != null && Boolean.TRUE.equals(ecoExpand$invoke(proxy, "isActive"))) {
                ((IGrid) ecoExpand$invoke(proxy, "getGrid")).postEvent(new MENetworkCellArrayUpdate());
            }
        } catch (final ReflectiveOperationException | RuntimeException exception) {
            ECO_Expand.LOGGER.debug("Unable to refresh Essentia cell array after storage change.", exception);
        }
    }

    @Unique
    private static Object ecoExpand$invoke(final Object target, final String method)
            throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (final InvocationTargetException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof ReflectiveOperationException) {
                throw (ReflectiveOperationException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw exception;
        }
    }
}
