package com.lw.eco_expand.common.integration.theoneprobe;

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

    private static void processUniversalCellInfo(final IProbeInfo probeInfo, final EStorageCellDrive drive, final ItemStack stack, final EStorageCellUniversal cell) {
        final ProbeColumns columns = newColumns(probeInfo);

        addStatus(columns, drive);
        columns.labels.text("{*top.estorage.drive.cell*}");
        columns.values.text(stack.getDisplayName());

        final UniversalStorageStats stats = cell.getStats(stack, drive.getWorld());
        addLongPair(columns, "{*top.estorage.drive.cell.bytes*}", stats.usedBytes(), cell.getTotalBytesLong(stack));
        addLongPair(columns, "{*top.estorage.drive.cell.types*}", stats.types(), cell.getTotalTypes(stack));
    }

    private static void processSingleChannelCellInfo(final IProbeInfo probeInfo, final EStorageCellDrive drive, final ItemStack stack, final EStorageCell<?> cell) {
        final CellStats stats = getCellStats(stack, drive);
        final ProbeColumns columns = newColumns(probeInfo);

        addStatus(columns, drive);
        columns.labels.text("{*top.estorage.drive.cell*}");
        columns.values.text(stack.getDisplayName());

        if (stats == null) {
            columns.values.text(TextFormatting.RED + "Unavailable");
            return;
        }

        addLongPair(columns, "{*top.estorage.drive.cell.bytes*}", stats.usedBytes, stats.totalBytes);
        addLongPair(columns, "{*top.estorage.drive.cell.types*}", stats.usedTypes, stats.totalTypes);
    }

    private static void addStatus(final ProbeColumns columns, final EStorageCellDrive drive) {
        columns.labels.text("{*top.estorage.drive.status*}");
        columns.values.text(drive.getController() == null
                ? "{*top.estorage.drive.status.offline*}"
                : "{*top.estorage.drive.status.online*}");
    }

    private static void addLongPair(final ProbeColumns columns, final String label, final long used, final long total) {
        columns.labels.text(label);
        if (total <= 0) {
            columns.values.text(TextFormatting.YELLOW + String.valueOf(used));
            return;
        }
        columns.values.text(TextFormatting.YELLOW + String.valueOf(used)
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

    private static ProbeColumns newColumns(final IProbeInfo probeInfo) {
        final IProbeInfo box = probeInfo.horizontal(probeInfo.defaultLayoutStyle().borderColor(0x802020ff));
        return new ProbeColumns(newVertical(box), newVertical(box));
    }

    private static IProbeInfo newVertical(final IProbeInfo probeInfo) {
        return probeInfo.vertical(probeInfo.defaultLayoutStyle().spacing(0));
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

    private static final class ProbeColumns {
        private final IProbeInfo labels;
        private final IProbeInfo values;

        private ProbeColumns(final IProbeInfo labels, final IProbeInfo values) {
            this.labels = labels;
            this.values = values;
        }
    }
}
