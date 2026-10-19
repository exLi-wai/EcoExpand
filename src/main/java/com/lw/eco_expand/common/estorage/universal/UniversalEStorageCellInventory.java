package com.lw.eco_expand.common.estorage.universal;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.implementations.items.IStorageCell;
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
        if (!canAccept(input)) {
            return input;
        }

        final UniversalStorageWorldData data = getStorageData();
        if (data == null) {
            logRejectOnce("no_data:" + type, "Universal cell reject insert: no world data for type={}, stack={}", type, input);
            return input;
        }

        synchronized (data) {
            final Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack> typedStorage = data.getStorage(uuid, type);
            repairStorageKeys(typedStorage);
            final IAEStack<?> key = input.copy();
            key.setStackSize(1);
            final UniversalStorageWorldData.StoredStack existing = findStoredStack(typedStorage, key);
            final boolean newType = existing == null;

            final UniversalStorageStats stats = cell.getStats(container, getStorageWorld());
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
                    existing.setCount(saturatedAdd(existing.count(), available));
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

        synchronized (data) {
            final Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack> typedStorage = data.getStorage(uuid, type);
            repairStorageKeys(typedStorage);
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
                    typedStorage.remove(existing.stack());
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
    }

    @Override
    public IItemList<T> getAvailableItems(final IItemList<T> out) {
        final UniversalStorageWorldData data = getStorageData();
        if (data == null) {
            return out;
        }

        synchronized (data) {
            final Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack> typedStorage = data.getStorage(uuid, type);
            repairStorageKeys(typedStorage);
            for (final UniversalStorageWorldData.StoredStack stored : typedStorage.values()) {
                if (stored.count() <= 0 || !canAccept((T) stored.stack())) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                final T stack = (T) stored.stack().copy();
                stack.setStackSize(stored.count());
                out.add(stack);
            }
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
        return stack != null
                && UniversalStorageDataManager.matchesType(stack, type)
                && !isSelfOrNonStorableCell(stack);
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
        final long usedBytes = saturatedAdd(stats.usedBytes(), typeCost);
        final long totalBytes = cell.getTotalBytesLong(container);
        if (usedBytes >= totalBytes) {
            return 0L;
        }
        final long freeBytes = totalBytes - usedBytes;
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

    private static long saturatedAdd(final long left, final long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0L && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
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

    private boolean isSelfOrNonStorableCell(final T stack) {
        if (!(stack instanceof appeng.api.storage.data.IAEItemStack)) {
            return false;
        }

        final ItemStack definition = ((appeng.api.storage.data.IAEItemStack) stack).getDefinition();
        if (definition == null || definition.isEmpty() || !(definition.getItem() instanceof IStorageCell)) {
            return false;
        }

        final IStorageCell<?> storageCell = (IStorageCell<?>) definition.getItem();
        if (!storageCell.storableInStorageCell()) {
            return true;
        }

        if (definition.getItem() instanceof EStorageCellUniversal) {
            final UUID incomingUuid = EStorageCellUniversal.getUuid(definition);
            return incomingUuid != null && uuid.equals(incomingUuid);
        }
        return false;
    }

    /**
     * Rebuild the FastUtil map so every key is a fresh copy of its stored
     * stack. Older versions could mutate a stored item NBT while enumerating
     * the inventory, leaving the key in the wrong hash bucket.
     */
    private void repairStorageKeys(final Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack> storage) {
        if (storage.isEmpty() || !needsStorageKeyRepair(storage)) {
            return;
        }

        final Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack> repaired =
                new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>(storage.size());
        for (final Object2ObjectMap.Entry<IAEStack<?>, UniversalStorageWorldData.StoredStack> entry
                : storage.object2ObjectEntrySet()) {
            final UniversalStorageWorldData.StoredStack stored = entry.getValue();
            if (stored == null || stored.stack() == null || stored.count() <= 0) {
                continue;
            }

            final IAEStack<?> key = stored.stack().copy();
            key.setStackSize(1);
            final UniversalStorageWorldData.StoredStack previous = repaired.get(key);
            if (previous == null) {
                repaired.put(key, new UniversalStorageWorldData.StoredStack(key, stored.count()));
            } else {
                previous.setCount(saturatedAdd(previous.count(), stored.count()));
            }
        }

        storage.clear();
        storage.putAll(repaired);
    }

    private boolean needsStorageKeyRepair(final Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack> storage) {
        for (final Object2ObjectMap.Entry<IAEStack<?>, UniversalStorageWorldData.StoredStack> entry
                : storage.object2ObjectEntrySet()) {
            final IAEStack<?> key = entry.getKey();
            final UniversalStorageWorldData.StoredStack stored = entry.getValue();
            if (key == null || stored == null || stored.stack() == null || stored.count() <= 0
                    || key != stored.stack() || key.getStackSize() != 1 || storage.get(key) != stored) {
                return true;
            }
        }
        return false;
    }

    private World getStorageWorld() {
        if (saveProvider instanceof TileEntity) {
            final World world = ((TileEntity) saveProvider).getWorld();
            if (world != null) {
                return world;
            }
        }
        return EStorageCellUniversal.getRememberedWorld(container);
    }

    private UniversalStorageWorldData getStorageData() {
        final World world = getStorageWorld();
        if (world != null) {
            return UniversalStorageWorldData.get(world);
        }
        return UniversalStorageWorldData.get();
    }

    private static void logRejectOnce(final String key, final String message, final Object... args) {
        if (LOGGED_REJECTIONS.add(key)) {
            ECO_Expand.LOGGER.warn(message, args);
        }
    }
}
