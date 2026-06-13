package com.lw.eco_expand.mixin;

import github.kasuminova.ecoaeextension.common.block.ecotech.estorage.prop.DriveStorageType;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraftforge.common.util.EnumHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DriveStorageType.class, remap = false)
public abstract class MixinDriveStorageType {

    @Unique
    private static final String[][] ECO_EXPAND$EXTRA_TYPES = {
            {"ENERGY", "energy"},
            {"MANA", "mana"},
            {"ESSENTIA", "essentia"}
    };

    @Shadow
    @Final
    @Mutable
    private static DriveStorageType[] $VALUES;

    @Shadow
    @Final
    @Mutable
    public static PropertyEnum<DriveStorageType> STORAGE_TYPE;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void ecoExpand$addExtraTypes(final CallbackInfo ci) {
        boolean addedType = false;
        for (final String[] type : ECO_EXPAND$EXTRA_TYPES) {
            if (!ecoExpand$hasType(type[0], type[1])) {
                EnumHelper.addEnum(
                        DriveStorageType.class,
                        type[0],
                        new Class[]{String.class},
                        type[1]
                );
                addedType = true;
            }
        }

        if (addedType) {
            STORAGE_TYPE = PropertyEnum.create("storage_type", DriveStorageType.class);
        }
    }

    @Unique
    private static boolean ecoExpand$hasType(final String internalName, final String serializedName) {
        for (final DriveStorageType type : $VALUES) {
            if (internalName.equals(type.name()) || serializedName.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }
}
