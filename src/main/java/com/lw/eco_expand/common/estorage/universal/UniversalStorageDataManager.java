package com.lw.eco_expand.common.estorage.universal;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.fluids.util.AEFluidStack;
import appeng.util.item.AEItemStack;
import com.mekeng.github.common.ItemAndBlocks;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import dev.beecube31.crazyae2.core.CrazyAE;
import dev.beecube31.crazyae2.core.CrazyAESidedHandler;
import dev.beecube31.crazyae2.core.api.storage.IManaStorageChannel;
import net.minecraft.nbt.NBTTagCompound;
import thaumicenergistics.api.storage.IAEEssentiaStack;
import thaumicenergistics.api.storage.IEssentiaStorageChannel;
import thaumicenergistics.integration.appeng.AEEssentiaStack;

import javax.annotation.Nullable;

public final class UniversalStorageDataManager {

    public static final String TYPE_ITEM = "item";
    public static final String TYPE_FLUID = "fluid";
    public static final String TYPE_GAS = "gas";
    public static final String TYPE_MANA = "mana";
    public static final String TYPE_ESSENTIA = "essentia";

    @Nullable
    public static String getTypeForChannel(final IStorageChannel<?> channel) {
        if (channel instanceof IItemStorageChannel
                || channel == AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)) {
            return TYPE_ITEM;
        }
        if (channel instanceof IFluidStorageChannel
                || channel == AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class)) {
            return TYPE_FLUID;
        }
        if (channel instanceof IGasStorageChannel
                || channel == AEApi.instance().storage().getStorageChannel(IGasStorageChannel.class)) {
            return TYPE_GAS;
        }
        if (channel instanceof IManaStorageChannel
                || channel == AEApi.instance().storage().getStorageChannel(IManaStorageChannel.class)) {
            return TYPE_MANA;
        }
        if (channel instanceof IEssentiaStorageChannel
                || channel == AEApi.instance().storage().getStorageChannel(IEssentiaStorageChannel.class)) {
            return TYPE_ESSENTIA;
        }
        return null;
    }

    public static boolean canStore(final IStorageChannel<?> channel) {
        return getTypeForChannel(channel) != null;
    }

    public static boolean matchesType(final IAEStack<?> stack, final String type) {
        if (TYPE_ITEM.equals(type)) {
            return stack instanceof IAEItemStack && !isCrazyAEInternalStack((IAEItemStack) stack);
        }
        if (TYPE_MANA.equals(type)) {
            return stack instanceof IAEItemStack && isManaStack((IAEItemStack) stack);
        }
        if (TYPE_FLUID.equals(type)) {
            return stack instanceof IAEFluidStack;
        }
        if (TYPE_GAS.equals(type)) {
            return stack instanceof IAEGasStack;
        }
        if (TYPE_ESSENTIA.equals(type)) {
            return stack instanceof IAEEssentiaStack;
        }
        return false;
    }

    @Nullable
    public static IAEStack<?> readStack(final String type, final NBTTagCompound tag) {
        if (TYPE_ITEM.equals(type) || TYPE_MANA.equals(type)) {
            return AEItemStack.fromNBT(tag);
        }
        if (TYPE_FLUID.equals(type)) {
            return AEFluidStack.fromNBT(tag);
        }
        if (TYPE_GAS.equals(type)) {
            return AEGasStack.of(tag);
        }
        if (TYPE_ESSENTIA.equals(type)) {
            return AEEssentiaStack.fromNBT(tag);
        }
        return null;
    }

    public static boolean writeStack(final String type, final IAEStack<?> stack, final NBTTagCompound tag) {
        if (!matchesType(stack, type)) {
            return false;
        }
        stack.writeToNBT(tag);
        return true;
    }

    public static boolean isSupportedType(final String type) {
        return TYPE_ITEM.equals(type)
                || TYPE_FLUID.equals(type)
                || TYPE_GAS.equals(type)
                || TYPE_MANA.equals(type)
                || TYPE_ESSENTIA.equals(type);
    }

    private static boolean isCrazyAEInternalStack(final IAEItemStack stack) {
        return isManaStack(stack) || isEnergyStack(stack) || isMekEngGasStack(stack);
    }

    private static boolean isManaStack(final IAEItemStack stack) {
        return isSameAs(CrazyAE.definitions().items().manaAsAEStack(), stack);
    }

    private static boolean isEnergyStack(final IAEItemStack stack) {
        for (final IItemDefinition definition : CrazyAESidedHandler.availableEnergyTypes) {
            if (isSameAs(definition, stack)) {
                return true;
            }
        }

        for (final IItemDefinition definition : CrazyAE.definitions().items().energyItemsList()) {
            if (isSameAs(definition, stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMekEngGasStack(final IAEItemStack stack) {
        return ItemAndBlocks.DUMMY_GAS != null
                && stack.asItemStackRepresentation().getItem() == ItemAndBlocks.DUMMY_GAS;
    }

    private static boolean isSameAs(final IItemDefinition definition, final IAEItemStack stack) {
        return definition != null && definition.isSameAs(stack.asItemStackRepresentation());
    }
}
