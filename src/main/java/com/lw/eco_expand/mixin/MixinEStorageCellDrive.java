package com.lw.eco_expand.mixin;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.tile.inventory.AppEngCellInventory;
import appeng.util.inv.InvOperation;
import com.lw.eco_expand.ECO_Expand;
import com.lw.eco_expand.common.item.estorage.EStorageCellUniversal;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageLevel;
import github.kasuminova.ecoaeextension.common.estorage.ECellDriveWatcher;
import github.kasuminova.ecoaeextension.common.estorage.EStorageCellHandler;
import github.kasuminova.ecoaeextension.common.tile.ecotech.estorage.EStorageCellDrive;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(value = EStorageCellDrive.class, remap = false)
public abstract class MixinEStorageCellDrive {

    @Unique
    private static final Set<String> LOGGED_HANDLER_RESULTS = new HashSet<>();

    @Shadow
    protected boolean isCached;

    @Shadow
    protected EStorageCellHandler cellHandler;

    @Shadow
    protected ECellDriveWatcher<?> watcher;

    @Final
    @Shadow
    protected AppEngCellInventory driveInv;

    @Final
    @Shadow
    protected Map<IStorageChannel<? extends IAEStack<?>>, ?> inventoryHandlers;

    @Shadow
    public abstract void updateDriveBlockState();

    @Shadow
    public abstract boolean isCellSupported(DriveStorageLevel level);

    @Shadow
    protected abstract void updateHandler(boolean refreshState);

    @Inject(method = "onChangeInventory", at = @At("RETURN"))
    private void ECO_Expand$postUniversalCellRemoval(final IItemHandler inv, final int slot, final InvOperation operation, final ItemStack removedStack, final ItemStack newStack, final CallbackInfo ci) {
        if (slot != 0 || removedStack.isEmpty() || !(removedStack.getItem() instanceof EStorageCellUniversal) || !newStack.isEmpty()) {
            return;
        }

        ECO_Expand$postRemovalChanges(removedStack);
        EStorageCellUniversal.releaseUuid(removedStack, (EStorageCellDrive) (Object) this);
    }

    @Inject(method = "getHandler", at = @At("HEAD"), cancellable = true)
    private <T extends IAEStack<T>> void ECO_Expand$getUniversalHandler(final IStorageChannel<T> channel,
                                                                       final CallbackInfoReturnable<IMEInventoryHandler<T>> cir) {
        final ItemStack stack = driveInv.getStackInSlot(0);
        if (!(stack.getItem() instanceof EStorageCellUniversal)) {
            return;
        }
        if (!isCellSupported(((EStorageCellUniversal) stack.getItem()).getLevel())) {
            cir.setReturnValue(null);
            return;
        }

        updateHandler(false);

        @SuppressWarnings("unchecked")
        final IMEInventoryHandler<T> handler = (IMEInventoryHandler<T>) inventoryHandlers.get(channel);
        final String key = "getHandler:" + stack.getItem().getRegistryName() + ":" + channel.getClass().getName() + ":" + (handler != null);
        if (LOGGED_HANDLER_RESULTS.add(key)) {
            ECO_Expand.LOGGER.info("Universal EStorage getHandler channel={} result={} handlers={}",
                    channel.getClass().getName(), handler != null, inventoryHandlers.size());
        }
        cir.setReturnValue(handler);
    }

    @Inject(method = "updateHandler", at = @At("HEAD"), cancellable = true)
    private void ECO_Expand$updateUniversalHandler(final boolean refreshState, final CallbackInfo ci) {
        if (isCached) {
            ci.cancel();
            return;
        }

        final ItemStack stack = driveInv.getStackInSlot(0);
        if (!(stack.getItem() instanceof EStorageCellUniversal)) {
            return;
        }
        if (!isCellSupported(((EStorageCellUniversal) stack.getItem()).getLevel())) {
            watcher = null;
            cellHandler = null;
            ECO_Expand$getInventoryHandlers().clear();
            isCached = true;
            if (refreshState) {
                updateDriveBlockState();
            }
            ci.cancel();
            return;
        }

        watcher = null;
        cellHandler = null;
        ECO_Expand$getInventoryHandlers().clear();
        isCached = true;

        if (stack.isEmpty()) {
            updateDriveBlockState();
            ci.cancel();
            return;
        }

        cellHandler = EStorageCellHandler.getHandler(stack);
        if (cellHandler == null) {
            ci.cancel();
            return;
        }

        boolean found = false;
        final List<String> attachedChannels = new ArrayList<>();
        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            final ICellInventoryHandler<?> cellInventory = ECO_Expand$getCellInventory(stack, channel);
            if (cellInventory == null) {
                continue;
            }

            found = true;
            driveInv.setHandler(0, cellInventory);
            final ECellDriveWatcher<?> channelWatcher = ECO_Expand$createWatcher(cellInventory, channel);
            if (watcher == null) {
                watcher = channelWatcher;
            }
            ECO_Expand$getInventoryHandlers().put(channel, channelWatcher);
            attachedChannels.add(channel.getClass().getName());
        }

        ECO_Expand.LOGGER.info("Universal EStorage cell {} attached {} channel handlers: {}",
                stack.getItem().getRegistryName(), attachedChannels.size(), attachedChannels);

        if (found && refreshState) {
            updateDriveBlockState();
        }

        ci.cancel();
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<IStorageChannel<? extends IAEStack<?>>, ECellDriveWatcher<?>> ECO_Expand$getInventoryHandlers() {
        return (Map) inventoryHandlers;
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private ICellInventoryHandler<?> ECO_Expand$getCellInventory(final ItemStack stack,
                                                               final IStorageChannel<? extends IAEStack<?>> channel) {
        return cellHandler.getCellInventory(stack, (EStorageCellDrive) (Object) this, (IStorageChannel) channel);
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private ECellDriveWatcher<?> ECO_Expand$createWatcher(final ICellInventoryHandler<?> cellInventory,
                                                        final IStorageChannel<? extends IAEStack<?>> channel) {
        return new ECellDriveWatcher(cellInventory, (IStorageChannel) channel, (EStorageCellDrive) (Object) this);
    }

    @Unique
    private void ECO_Expand$postRemovalChanges(final ItemStack removedStack) {
        final EStorageCellHandler handler = EStorageCellHandler.getHandler(removedStack);
        if (handler == null) {
            return;
        }

        try {
            final Object controller = ((EStorageCellDrive) (Object) this).getController();
            if (controller == null) {
                return;
            }

            final Object channelPart = ECO_Expand$invoke(controller, "getChannel");
            if (channelPart == null) {
                return;
            }

            final Object proxy = ECO_Expand$invoke(channelPart, "getProxy");
            if (proxy == null || !((Boolean) ECO_Expand$invoke(proxy, "isActive"))) {
                return;
            }

            final IStorageGrid storageGrid = (IStorageGrid) ECO_Expand$invoke(proxy, "getStorage");
            final IActionSource source = (IActionSource) ECO_Expand$invoke(channelPart, "getSource");
            for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
                ECO_Expand$postRemovalChanges(storageGrid, source, handler, removedStack, channel);
            }
            ECO_Expand$postCellArrayUpdate(proxy);
        } catch (final ReflectiveOperationException e) {
            ECO_Expand.LOGGER.warn("Failed to post universal EStorage cell removal changes.", e);
        }
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void ECO_Expand$postRemovalChanges(final IStorageGrid storageGrid,
                                               final IActionSource source,
                                               final EStorageCellHandler handler,
                                               final ItemStack removedStack,
                                               final IStorageChannel<? extends IAEStack<?>> channel) {
        final ICellInventoryHandler cellInventory = handler.getCellInventory(removedStack, (EStorageCellDrive) (Object) this, (IStorageChannel) channel);
        if (cellInventory == null) {
            return;
        }

        final IItemList changes = ((IStorageChannel) channel).createList();
        cellInventory.getAvailableItems(changes);
        for (final Object object : changes) {
            final IAEStack stack = (IAEStack) object;
            stack.setStackSize(-stack.getStackSize());
        }
        storageGrid.postAlterationOfStoredItems(channel, changes, source);
    }

    @Unique
    private void ECO_Expand$postCellArrayUpdate(final Object proxy) throws ReflectiveOperationException {
        final IGrid grid = (IGrid) ECO_Expand$invoke(proxy, "getGrid");
        grid.postEvent(new MENetworkCellArrayUpdate());
    }

    @Unique
    private static Object ECO_Expand$invoke(final Object target, final String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }
}
