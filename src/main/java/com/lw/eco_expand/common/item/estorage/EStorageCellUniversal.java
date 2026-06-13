package com.lw.eco_expand.common.item.estorage;

import appeng.api.AEApi;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import com.lw.eco_expand.ECO_Expand;
import com.lw.eco_expand.Tags;
import com.lw.eco_expand.common.estorage.universal.UniversalStorageStats;
import com.lw.eco_expand.common.estorage.universal.UniversalStorageDataManager;
import com.lw.eco_expand.common.estorage.universal.UniversalStorageWorldData;
import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageLevel;
import github.kasuminova.ecoaeextension.common.item.estorage.EStorageCell;
import github.kasuminova.ecoaeextension.common.tile.ecotech.estorage.EStorageCellDrive;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class EStorageCellUniversal extends EStorageCell<IAEItemStack> {

    private static final String NBT_UUID = "eco_expand_universal_uuid";
    private static final String NBT_LEVEL_NAME = "eco_expand_universal_level";
    private static final Object2ObjectMap<UUID, String> UUID_OWNERS = new Object2ObjectOpenHashMap<>();
    private static final int TYPES_L4 = 315;
    private static final int TYPES_L6 = 512;
    private static final int TYPES_L9 = 768;
    private static final int TYPES_LE12 = 1024;
    private static final int TYPES_LE15 = 2048;
    private static final int TYPES_INFINITE = Integer.MAX_VALUE;

    public static final EStorageCellUniversal LEVEL_L4 = new EStorageCellUniversal("16m", "le4", DriveStorageLevel.A, 16, 4, TYPES_L4);
    public static final EStorageCellUniversal LEVEL_L6 = new EStorageCellUniversal("64m", "le6", DriveStorageLevel.B, 64, 16, TYPES_L6);
    public static final EStorageCellUniversal LEVEL_L9 = new EStorageCellUniversal("256m", "le9", DriveStorageLevel.C, 256, 64, TYPES_L9);
    public static final EStorageCellUniversal LEVEL_L12 = new EStorageCellUniversal("1024m", "le12", DriveStorageLevel.C, 1024, 256, TYPES_LE12);
    public static final EStorageCellUniversal LEVEL_L15 = new EStorageCellUniversal("2048m", "le15", DriveStorageLevel.C, 2048, 256, TYPES_LE15);
    public static final EStorageCellUniversal LEVEL_INFINITE = new EStorageCellUniversal("inf", "inf", DriveStorageLevel.C, 2048, 256, TYPES_INFINITE, Long.MAX_VALUE);

    private final String capacityName;
    private final String levelName;
    private final long totalBytesLong;
    private final int totalTypes;

    public EStorageCellUniversal(final String capacityName, final String levelName, final DriveStorageLevel level, final int millionBytes, final int byteMultiplier, final int totalTypes) {
        this(capacityName, levelName, level, millionBytes, byteMultiplier, totalTypes, millionBytes * 1000L * 1024L);
    }

    private EStorageCellUniversal(final String capacityName, final String levelName, final DriveStorageLevel level, final int millionBytes, final int byteMultiplier, final int totalTypes, final long totalBytesLong) {
        super(level, millionBytes, byteMultiplier);
        this.capacityName = capacityName;
        this.levelName = levelName;
        this.totalBytesLong = totalBytesLong;
        this.totalTypes = totalTypes;
        setRegistryName(new ResourceLocation(Tags.MOD_ID, "estorage_cell_universal_" + capacityName));
        setTranslationKey(Tags.MOD_ID + '.' + "estorage_cell_universal_" + capacityName);
    }

    public static UUID getOrCreateUuid(final ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (tag.hasUniqueId(NBT_UUID)) {
            return tag.getUniqueId(NBT_UUID);
        }

        final UUID uuid = UUID.randomUUID();
        tag.setUniqueId(NBT_UUID, uuid);
        return uuid;
    }

    public static UUID getOrCreateUuid(final ItemStack stack, final ISaveProvider saveProvider) {
        final UUID uuid = getOrCreateUuid(stack);
        final String owner = getOwnerKey(saveProvider);
        if (owner == null) {
            return uuid;
        }

        synchronized (UUID_OWNERS) {
            final String existingOwner = UUID_OWNERS.get(uuid);
            if (existingOwner == null || existingOwner.equals(owner) || !isOwnerStillHolding(existingOwner, uuid)) {
                UUID_OWNERS.put(uuid, owner);
                return uuid;
            }

            final UUID replacementUuid = UUID.randomUUID();
            setUuid(stack, replacementUuid);
            UUID_OWNERS.put(replacementUuid, owner);
            ECO_Expand.LOGGER.warn("Universal EStorage cell duplicate UUID detected. oldUuid={}, oldOwner={}, newOwner={}, replacementUuid={}",
                    uuid, existingOwner, owner, replacementUuid);
            saveProvider.saveChanges(null);
            return replacementUuid;
        }
    }

    public static void releaseUuid(final ItemStack stack, final ISaveProvider saveProvider) {
        if (stack.isEmpty() || !(stack.getItem() instanceof EStorageCellUniversal)) {
            return;
        }

        final NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasUniqueId(NBT_UUID)) {
            return;
        }

        final String owner = getOwnerKey(saveProvider);
        if (owner == null) {
            return;
        }

        final UUID uuid = tag.getUniqueId(NBT_UUID);
        synchronized (UUID_OWNERS) {
            if (owner.equals(UUID_OWNERS.get(uuid))) {
                UUID_OWNERS.remove(uuid);
            }
        }
    }

    private static void setUuid(final ItemStack stack, final UUID uuid) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setUniqueId(NBT_UUID, uuid);
    }

    @Nullable
    private static String getOwnerKey(final ISaveProvider saveProvider) {
        if (saveProvider instanceof TileEntity) {
            final TileEntity tile = (TileEntity) saveProvider;
            final World world = tile.getWorld();
            final BlockPos pos = tile.getPos();
            if (world != null && pos != null) {
                return world.provider.getDimension() + ":" + pos.toLong();
            }
        }
        return null;
    }

    private static boolean isOwnerStillHolding(final String owner, final UUID uuid) {
        final String[] parts = owner.split(":", 2);
        if (parts.length != 2) {
            return false;
        }

        try {
            final int dimension = Integer.parseInt(parts[0]);
            final long posLong = Long.parseLong(parts[1]);
            final World world = net.minecraftforge.common.DimensionManager.getWorld(dimension);
            if (world == null) {
                return false;
            }

            final TileEntity tile = world.getTileEntity(BlockPos.fromLong(posLong));
            if (!(tile instanceof EStorageCellDrive)) {
                return false;
            }

            final ItemStack stack = ((EStorageCellDrive) tile).getDriveInv().getStackInSlot(0);
            final NBTTagCompound tag = stack.getTagCompound();
            return !stack.isEmpty()
                    && stack.getItem() instanceof EStorageCellUniversal
                    && tag != null
                    && tag.hasUniqueId(NBT_UUID)
                    && uuid.equals(tag.getUniqueId(NBT_UUID));
        } catch (final RuntimeException ignored) {
            return false;
        }
    }

    public String getCapacityName() {
        return capacityName;
    }

    public String getLevelName() {
        return levelName;
    }

    @Override
    public int getTotalTypes(@Nonnull final ItemStack cellItem) {
        return totalTypes;
    }

    @Override
    public int getBytesPerType(@Nonnull final ItemStack cellItem) {
        return byteMultiplier * 1024;
    }

    @Override
    public int getBytes(@Nonnull final ItemStack cellItem) {
        return totalBytesLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalBytesLong;
    }

    public long getTotalBytesLong(final ItemStack cellItem) {
        return totalBytesLong;
    }

    public long getBytesPerTypeLong(final ItemStack cellItem) {
        return byteMultiplier * 1024L;
    }

    @Nonnull
    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
    }

    public UniversalStorageStats getStats(final ItemStack stack) {
        return getStats(stack, null);
    }

    public UniversalStorageStats getStats(final ItemStack stack, final World world) {
        final UniversalStorageWorldData data = world == null ? UniversalStorageWorldData.get() : UniversalStorageWorldData.get(world);
        if (data == null) {
            return new UniversalStorageStats(0, 0, 0);
        }

        int types = 0;
        long count = 0;
        long countBytes = 0;
        final UUID uuid = getOrCreateUuid(stack);
        final Object2ObjectMap<String, Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack>> storage = data.getStorage(uuid);
        for (final Object2ObjectMap.Entry<String, Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack>> typedEntry
                : storage.object2ObjectEntrySet()) {
            if (!UniversalStorageDataManager.isSupportedType(typedEntry.getKey())) {
                continue;
            }

            final Object2ObjectMap<IAEStack<?>, UniversalStorageWorldData.StoredStack> typedStorage = typedEntry.getValue();
            for (final UniversalStorageWorldData.StoredStack stored : typedStorage.values()) {
                if (stored.count() > 0) {
                    types++;
                    count += stored.count();
                    final long unitsPerByte = stored.stack().getChannel().getUnitsPerByte() * (long) byteMultiplier;
                    countBytes += (stored.count() + unitsPerByte - 1L) / unitsPerByte;
                }
            }
        }

        final long usedBytes = types * getBytesPerTypeLong(stack) + countBytes;
        return new UniversalStorageStats(types, count, usedBytes);
    }

    @Override
    protected void addCheckedInformation(final ItemStack stack, final World world, final List<String> lines, final ITooltipFlag advancedTooltips) {
        final UniversalStorageStats stats = getStats(stack);
        lines.add("Universal Types: " + stats.types() + " / " + totalTypes);
        lines.add("Universal Bytes: " + stats.usedBytes() + " / " + getTotalBytesLong(stack));
        final NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasUniqueId(NBT_UUID)) {
            tag.setString(NBT_LEVEL_NAME, levelName);
        }
    }
}
