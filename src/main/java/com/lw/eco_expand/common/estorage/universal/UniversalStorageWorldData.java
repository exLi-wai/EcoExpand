package com.lw.eco_expand.common.estorage.universal;

import appeng.api.storage.data.IAEStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.UUID;

public class UniversalStorageWorldData extends WorldSavedData {

    private static final String DATA_NAME = "eco_expand_universal_storage";

    private final Object2ObjectMap<UUID, Object2ObjectMap<String, Object2ObjectMap<IAEStack<?>, StoredStack>>> storageData =
            new Object2ObjectOpenHashMap<>();

    public UniversalStorageWorldData(final String name) {
        super(name);
    }

    @Nullable
    public static UniversalStorageWorldData get() {
        final World world = DimensionManager.getWorld(0);
        if (world == null) {
            return null;
        }
        return get(world);
    }

    public static UniversalStorageWorldData get(final World world) {
        final MapStorage storage = world.getPerWorldStorage();
        UniversalStorageWorldData data = (UniversalStorageWorldData) storage.getOrLoadData(UniversalStorageWorldData.class, DATA_NAME);
        if (data == null) {
            data = new UniversalStorageWorldData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public Object2ObjectMap<String, Object2ObjectMap<IAEStack<?>, StoredStack>> getStorage(final UUID uuid) {
        return storageData.computeIfAbsent(uuid, ignored -> new Object2ObjectOpenHashMap<>());
    }

    public Object2ObjectMap<IAEStack<?>, StoredStack> getStorage(final UUID uuid, final String type) {
        return getStorage(uuid).computeIfAbsent(type, ignored -> new Object2ObjectOpenHashMap<>());
    }

    @Override
    public void readFromNBT(final NBTTagCompound nbt) {
        storageData.clear();
        final NBTTagList cellList = nbt.getTagList("cells", Constants.NBT.TAG_COMPOUND);
        for (int cellIdx = 0; cellIdx < cellList.tagCount(); cellIdx++) {
            final NBTTagCompound cellTag = cellList.getCompoundTagAt(cellIdx);
            final UUID uuid = UUID.fromString(cellTag.getString("uuid"));
            final NBTTagList itemList = cellTag.getTagList("items", Constants.NBT.TAG_COMPOUND);
            for (int itemIdx = 0; itemIdx < itemList.tagCount(); itemIdx++) {
                final NBTTagCompound itemTag = itemList.getCompoundTagAt(itemIdx);
                final String type = itemTag.getString("type");
                if (!UniversalStorageDataManager.isSupportedType(type)) {
                    continue;
                }

                final long count = itemTag.getLong("count");
                final IAEStack<?> stack = UniversalStorageDataManager.readStack(type, itemTag.getCompoundTag("stack"));
                if (stack != null && count > 0) {
                    final IAEStack<?> key = stack.copy();
                    key.setStackSize(1);
                    getStorage(uuid, type).put(key, new StoredStack(key, count));
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound nbt) {
        final NBTTagList cellList = new NBTTagList();
        for (final Object2ObjectMap.Entry<UUID, Object2ObjectMap<String, Object2ObjectMap<IAEStack<?>, StoredStack>>> cellEntry
                : storageData.object2ObjectEntrySet()) {
            final NBTTagCompound cellTag = new NBTTagCompound();
            cellTag.setString("uuid", cellEntry.getKey().toString());

            final NBTTagList itemList = new NBTTagList();
            for (final Object2ObjectMap.Entry<String, Object2ObjectMap<IAEStack<?>, StoredStack>> typedEntry
                    : cellEntry.getValue().object2ObjectEntrySet()) {
                final String type = typedEntry.getKey();
                if (!UniversalStorageDataManager.isSupportedType(type)) {
                    continue;
                }

                for (final StoredStack stored : typedEntry.getValue().values()) {
                    if (stored.count <= 0) {
                        continue;
                    }
                    final NBTTagCompound stackTag = new NBTTagCompound();
                    if (!UniversalStorageDataManager.writeStack(type, stored.stack, stackTag)) {
                        continue;
                    }
                    final NBTTagCompound itemTag = new NBTTagCompound();
                    itemTag.setString("type", type);
                    itemTag.setLong("count", stored.count);
                    itemTag.setTag("stack", stackTag);
                    itemList.appendTag(itemTag);
                }
            }

            if (itemList.tagCount() > 0) {
                cellTag.setTag("items", itemList);
                cellList.appendTag(cellTag);
            }
        }
        nbt.setTag("cells", cellList);
        return nbt;
    }

    public static final class StoredStack {
        private final IAEStack<?> stack;
        private long count;

        public StoredStack(final IAEStack<?> stack, final long count) {
            this.stack = stack;
            this.count = count;
        }

        public IAEStack<?> stack() {
            return stack;
        }

        public long count() {
            return count;
        }

        public void setCount(final long count) {
            this.count = count;
        }
    }
}
