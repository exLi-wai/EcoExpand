package com.lw.eco_expand.mixin;

import github.kasuminova.ecoaeextension.common.container.data.EStorageCellData;
import github.kasuminova.ecoaeextension.client.gui.widget.SizedColumn;
import github.kasuminova.mmce.client.gui.util.TextureProperties;
import github.kasuminova.mmce.client.gui.widget.MultiLineLabel;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(value = github.kasuminova.ecoaeextension.client.gui.widget.estorage.EStorageCellInfo.class, remap = false)
public abstract class MixinEStorageCellInfo extends SizedColumn {

    @Shadow
    @Final
    protected EStorageCellData data;

    @Shadow
    protected TextureProperties cellBackground;

    @Shadow
    protected TextureProperties cellTypeBackground;

    @Shadow @Final
    public static TextureProperties CELL_BACKGROUND_L4;

    @Shadow @Final
    public static TextureProperties CELL_BACKGROUND_L6;

    @Shadow @Final
    public static TextureProperties CELL_BACKGROUND_L9;

    @Shadow @Final
    public static TextureProperties CELL_TYPE_BACKGROUND_ITEM;

    @Inject(method = "initBackground", at = @At("HEAD"), cancellable = true)
    private void ecoExpand$initUniversalBackground(final CallbackInfo ci) {
        if (!ecoExpand$isUniversal()) {
            return;
        }

        final String level = data.level().name();
        if ("A".equals(level)) {
            cellBackground = CELL_BACKGROUND_L4;
        } else if ("B".equals(level)) {
            cellBackground = CELL_BACKGROUND_L6;
        } else {
            cellBackground = CELL_BACKGROUND_L9;
        }
        cellTypeBackground = CELL_TYPE_BACKGROUND_ITEM;
        ci.cancel();
    }

    @Inject(method = "initInfo", at = @At("HEAD"), cancellable = true)
    private void ecoExpand$initUniversalInfo(final CallbackInfo ci) {
        if (!ecoExpand$isUniversal()) {
            return;
        }

        final String typeName = I18n.format(ecoExpand$getTypeTranslationKey());
        final String levelName = ecoExpand$getCapacityName();
        final int usedTypes = data.usedTypes();
        final long usedBytes = data.usedBytes();
        final int maxTypes = github.kasuminova.ecoaeextension.common.tile.ecotech.estorage.EStorageCellDrive.getMaxTypes(data);
        final long maxBytes = github.kasuminova.ecoaeextension.common.tile.ecotech.estorage.EStorageCellDrive.getMaxBytes(data);

        getWidgets().clear();
        addWidget(new MultiLineLabel(Collections.singletonList(I18n.format("gui.estorage_controller.cell_info.tip.0", typeName, levelName)))
                .setAutoWrap(false).setScale(0.6F).setHeight(7).setMargin(10, 0, 2, 0));
        addWidget(new MultiLineLabel(Collections.singletonList(
                I18n.format("gui.estorage_controller.cell_info.tip.1", usedTypes, maxTypes)))
                .setAutoWrap(false).setScale(0.6F).setHeight(7).setMargin(10, 0, 0, 0));
        addWidget(new MultiLineLabel(Collections.singletonList(
                I18n.format("gui.estorage_controller.cell_info.tip.2", ecoExpand$formatNumber(usedBytes), ecoExpand$formatNumber(maxBytes))))
                .setAutoWrap(false).setScale(0.6F).setHeight(7).setMargin(10, 0, 0, 0));
        ci.cancel();
    }

    @Unique
    private boolean ecoExpand$isUniversal() {
        return data != null
                && data.type() != null
                && data.type().name().startsWith("UNIVERSAL_");
    }

    @Unique
    private String ecoExpand$getCapacityName() {
        final String type = data.type().name();
        return type.substring("UNIVERSAL_".length());
    }

    @Unique
    private String ecoExpand$getTypeTranslationKey() {
        return "gui.eco_expand.universal_cell." + ecoExpand$getCapacityName().toLowerCase(java.util.Locale.ROOT);
    }

    @Unique
    private static String ecoExpand$formatNumber(final long value) {
        return github.kasuminova.ecoaeextension.common.crafttweaker.util.NovaEngUtils.formatNumber(value, 1);
    }
}
