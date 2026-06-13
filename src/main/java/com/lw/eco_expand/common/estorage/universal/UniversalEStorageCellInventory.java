package com.lw.eco_expand.common.estorage.universal;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import com.lw.eco_expand.ECO_Expand;
import com.lw.eco_expand.common.item.estorage.EStorageCellUniversal;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UniversalEStorageCellInventory<T extends IAEStack<T>> implements IMEInventoryHandler<T> {

    private static final Set<String> LOGGED_REJECTIONS = new HashSet<>();

    private final EStorageCellUniversal cell;
    private final ItemStack container;
    private final ISaveProvider saveProvider;
    private final IStorageChannel<T> channel;
    private final String type;
    private final UUID uuid;

    public UniversalEStorageCellInventory(final EStorageCellUniversal cell, final ItemStack container, final ISaveProvider saveProvider, final IStorageChannel<T> channel, final String type) {
        this.cell = cell;
        this.container = container;
        this.saveProvider = saveProvider;
        this.channel = channel;
        this.type = type;
        this.uuid = EStorageCellUniversal.getOrCreateUuid(container, saveProvider);
    }

    @Override
    public T injectItems(final T input, final Actionable actionable, final IActionSource src) {
        if (input == null || input.getStackSize() <= 0) {
            return null;
        }

        final UniversalStorageWorldData data = getStorageData();
        if (data == null) {
            logRejectOnce("no_data:" + type, "Universal cell reject insert: no world data for type={}, stack={}", type, input);
            return input;
        }

        final Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack> typedStorage = data.getStorage(uuid, type);
        final IAEStack<?> key = input.copy();
        key.setStackSize(1);
        final UniversalStorageWorldData.StoredStack existing = findStoredStack(typedStorage, key);
        final boolean newType = existing == null;

        final UniversalStorageStats stats = cell.getStats(container);
        if (newType && stats.types() >= cell.getTotalTypes(container)) {
            logRejectOnce("type_full:" + type + ":" + cell.getRegistryName(),
                    "Universal cell reject insert: type limit reached type={}, types={}/{}, stack={}",
                    type, stats.types(), cell.getTotalTypes(container), input);
            return input;
        }

        final long available = getAvailableToInsert(input.getStackSize(), newType, stats);
        if (available <= 0) {
            logRejectOnce("byte_full:" + type + ":" + cell.getRegistryName(),
                    "Universal cell reject insert: no bytes type={}, usedBytes={}, totalBytes={}, stack={}",
                    type, stats.usedBytes(), cell.getTotalBytesLong(container), input);
            return input;
        }

        if (actionable == Actionable.MODULATE) {
            if (existing == null) {
                typedStorage.put(key, new UniversalStorageWorldData.StoredStack(key, available));
            } else {
                existing.setCount(existing.count() + available);
            }
            data.markDirty();
            notifySaveProvider();
            ECO_Expand.LOGGER.debug("Universal cell inserted: type={}, stackClass={}, inserted={}, requested={}, uuid={}",
                    type, input.getClass().getName(), available, input.getStackSize(), uuid);
        }

        if (available >= input.getStackSize()) {
            return null;
        }

        final T remainder = input.copy();
        remainder.setStackSize(input.getStackSize() - available);
        return remainder;
    }

    @Override
    public T extractItems(final T request, final Actionable actionable, final IActionSource src) {
        if (request == null || request.getStackSize() <= 0) {
            return null;
        }

        final UniversalStorageWorldData data = getStorageData();
        if (data == null) {
            return null;
        }

        final Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack> typedStorage = data.getStorage(uuid, type);
        final IAEStack<?> key = request.copy();
        key.setStackSize(1);
        final UniversalStorageWorldData.StoredStack existing = findStoredStack(typedStorage, key);
        if (existing == null || existing.count() <= 0) {
            return null;
        }

        final long extracted = Math.min(existing.count(), request.getStackSize());
        if (actionable == Actionable.MODULATE) {
            final long remaining = existing.count() - extracted;
            if (remaining <= 0) {
                typedStorage.remove(key);
            } else {
                existing.setCount(remaining);
            }
            data.markDirty();
            notifySaveProvider();
        }

        final T result = request.copy();
        result.setStackSize(extracted);
        return result;
    }

    @Override
    public IItemList<T> getAvailableItems(final IItemList<T> out) {
        final UniversalStorageWorldData data = getStorageData();
        if (data == null) {
            return out;
        }

        for (final UniversalStorageWorldData.StoredStack stored : data.getStorage(uuid, type).values()) {
            if (stored.count() <= 0) {
                continue;
            }
            @SuppressWarnings("unchecked")
            final T stack = (T) stored.stack().copy();
            stack.setStackSize(stored.count());
            out.add(stack);
        }
        return out;
    }

    @Override
    public IStorageChannel<T> getChannel() {
        return channel;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isPrioritized(final T stack) {
        return false;
    }

    @Override
    public boolean canAccept(final T stack) {
        return stack != null && UniversalStorageDataManager.matchesType(stack, type);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public int getSlot() {
        return 0;
    }

    @Override
    public boolean validForPass(final int pass) {
        return true;
    }

    private long getAvailableToInsert(final long requested, final boolean newType, final UniversalStorageStats stats) {
        final long typeCost = newType ? cell.getBytesPerTypeLong(container) : 0L;
        final long usedBytes = stats.usedBytes() + typeCost;
        final long freeBytes = cell.getTotalBytesLong(container) - usedBytes;
        if (freeBytes <= 0) {
            return 0;
        }
        final long freeItems = multiplySaturated(multiplySaturated(freeBytes, channel.getUnitsPerByte()), cell.getByteMultiplier());
        return Math.min(requested, freeItems);
    }

    private static long multiplySaturated(final long left, final long right) {
        if (left <= 0 || right <= 0) {
            return 0;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    private UniversalStorageWorldData.StoredStack findStoredStack(final Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack> storage,
                                                                  final IAEStack<?> key) {
        final UniversalStorageWorldData.StoredStack direct = storage.get(key);
        if (direct != null) {
            return direct;
        }

        for (final UniversalStorageWorldData.StoredStack stored : storage.values()) {
            if (stored.stack().equals(key)) {
                return stored;
            }
        }
        return null;
    }

    private void notifySaveProvider() {
        if (saveProvider != null) {
            saveProvider.saveChanges(null);
        }
    }

    private UniversalStorageWorldData getStorageData() {
        if (saveProvider instanceof TileEntity) {
            final World world = ((TileEntity) saveProvider).getWorld();
            if (world != null) {
                return UniversalStorageWorldData.get(world);
            }
        }
        return UniversalStorageWorldData.get();
    }

    private static void logRejectOnce(final String key, final String message, final Object... args) {
        if (LOGGED_REJECTIONS.add(key)) {
            ECO_Expand.LOGGER.warn(message, args);
        }
    }
}
