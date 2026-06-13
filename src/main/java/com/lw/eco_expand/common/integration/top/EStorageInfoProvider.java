package com.lw.eco_expand.common.integration.top;

import appeng.api.AEApi;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import com.lw.eco_expand.Tags;
import com.lw.eco_expand.common.estorage.universal.UniversalStorageStats;
import com.lw.eco_expand.common.item.estorage.EStorageCellEnergy;
import com.lw.eco_expand.common.item.estorage.EStorageCellEssentia;
import com.lw.eco_expand.common.item.estorage.EStorageCellMana;
import com.lw.eco_expand.common.item.estorage.EStorageCellUniversal;
import github.kasuminova.ecoaeextension.common.estorage.EStorageCellHandler;
import github.kasuminova.ecoaeextension.common.item.estorage.EStorageCell;
import github.kasuminova.ecoaeextension.common.tile.ecotech.estorage.EStorageCellDrive;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class EStorageInfoProvider implements IProbeInfoProvider {

    public static final EStorageInfoProvider INSTANCE = new EStorageInfoProvider();

    private EStorageInfoProvider() {
    }

    @Override
    public String getID() {
        return Tags.MOD_ID + ":estorage_info_provider";
    }

    @Override
    public void addProbeInfo(final ProbeMode mode, final IProbeInfo probeInfo, final EntityPlayer player, final World world, final IBlockState blockState, final IProbeHitData data) {
        final BlockPos pos = data.getPos();
        final TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof EStorageCellDrive)) {
            return;
        }

        final EStorageCellDrive drive = (EStorageCellDrive) te;
        final ItemStack stack = drive.getDriveInv().getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }

        final Item item = stack.getItem();
        if (!(item instanceof EStorageCellUniversal)
                && !(item instanceof EStorageCellMana)
                && !(item instanceof EStorageCellEnergy)
                && !(item instanceof EStorageCellEssentia)) {
            return;
        }

        if (item instanceof EStorageCellUniversal) {
            processUniversalCellInfo(probeInfo, drive, stack, (EStorageCellUniversal) item);
            return;
        }

        processSingleChannelCellInfo(probeInfo, drive, stack, (EStorageCell<?>) item);
    }

    private static void processUniversalCellInfo(final IProbeInfo probeInfo,
                                                 final EStorageCellDrive drive,
                                                 final ItemStack stack,
                                                 final EStorageCellUniversal cell) {
        final IProbeInfo box = newBox(probeInfo);
        final IProbeInfo labels = newVertical(box);
        final IProbeInfo values = newVertical(box);

        addStatus(labels, values, drive);
        labels.text("{*top.estorage.drive.cell*}");
        values.text(stack.getDisplayName());

        final UniversalStorageStats stats = cell.getStats(stack, drive.getWorld());
        addLongPair(labels, values, "{*top.estorage.drive.cell.bytes*}", stats.usedBytes(), cell.getBytes(stack));
        addLongPair(labels, values, "{*top.estorage.drive.cell.types*}", stats.types(), cell.getTotalTypes(stack));
    }

    private static void processSingleChannelCellInfo(final IProbeInfo probeInfo,
                                                     final EStorageCellDrive drive,
                                                     final ItemStack stack,
                                                     final EStorageCell<?> cell) {
        final CellStats stats = getCellStats(stack, drive);
        final IProbeInfo box = newBox(probeInfo);
        final IProbeInfo labels = newVertical(box);
        final IProbeInfo values = newVertical(box);

        addStatus(labels, values, drive);
        labels.text("{*top.estorage.drive.cell*}");
        values.text(stack.getDisplayName());

        if (stats == null) {
            values.text(TextFormatting.RED + "Unavailable");
            return;
        }

        addLongPair(labels, values, "{*top.estorage.drive.cell.bytes*}", stats.usedBytes, stats.totalBytes);
        addLongPair(labels, values, "{*top.estorage.drive.cell.types*}", stats.usedTypes, stats.totalTypes);
    }

    private static void addStatus(final IProbeInfo labels, final IProbeInfo values, final EStorageCellDrive drive) {
        labels.text("{*top.estorage.drive.status*}");
        values.text(drive.getController() == null
                ? "{*top.estorage.drive.status.offline*}"
                : "{*top.estorage.drive.status.online*}");
    }

    private static void addLongPair(final IProbeInfo labels, final IProbeInfo values, final String label, final long used, final long total) {
        labels.text(label);
        if (total <= 0) {
            values.text(TextFormatting.YELLOW + String.valueOf(used));
            return;
        }
        values.text(TextFormatting.YELLOW + String.valueOf(used)
                + TextFormatting.BLUE + " / "
                + TextFormatting.GOLD + total);
    }

    @Nullable
    private static CellStats getCellStats(final ItemStack stack, final EStorageCellDrive drive) {
        final EStorageCellHandler handler = EStorageCellHandler.getHandler(stack);
        if (handler == null) {
            return null;
        }

        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            final ICellInventoryHandler<?> cellInventory = getCellInventory(handler, stack, drive, channel);
            if (cellInventory == null || cellInventory.getCellInv() == null) {
                continue;
            }

            final ICellInventory<?> inventory = cellInventory.getCellInv();
            return new CellStats(inventory.getUsedBytes(),
                    inventory.getTotalBytes(),
                    inventory.getStoredItemTypes(),
                    inventory.getTotalItemTypes());
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    private static ICellInventoryHandler<?> getCellInventory(final EStorageCellHandler handler, final ItemStack stack, final EStorageCellDrive drive, final IStorageChannel<? extends IAEStack<?>> channel) {
        return handler.getCellInventory(stack, drive, (IStorageChannel) channel);
    }

    private static IProbeInfo newVertical(final IProbeInfo probeInfo) {
        return probeInfo.vertical(probeInfo.defaultLayoutStyle().spacing(0));
    }

    private static IProbeInfo newBox(final IProbeInfo probeInfo) {
        return probeInfo.horizontal(probeInfo.defaultLayoutStyle().borderColor(0x802020ff));
    }

    private static final class CellStats {
        private final long usedBytes;
        private final long totalBytes;
        private final long usedTypes;
        private final long totalTypes;

        private CellStats(final long usedBytes, final long totalBytes, final long usedTypes, final long totalTypes) {
            this.usedBytes = usedBytes;
            this.totalBytes = totalBytes;
            this.usedTypes = usedTypes;
            this.totalTypes = totalTypes;
        }
    }
}
