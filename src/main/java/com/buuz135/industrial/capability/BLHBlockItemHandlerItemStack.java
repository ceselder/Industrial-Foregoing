package com.buuz135.industrial.capability;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;

public class BLHBlockItemHandlerItemStack implements IItemHandler {

    public final ItemStack stack;
    public final int slotLimit;

    public BLHBlockItemHandlerItemStack(ItemStack stack, int slotLimit) {
        this.stack = stack;
        this.slotLimit = slotLimit;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack copied = getStack();
        copied.setCount(getAmount());
        return copied;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (isItemValid(slot, stack)) {
            int amount = getSlotLimit(slot);
            int stored = getAmount();
            int inserted = Math.min(amount - stored, stack.getCount());
            if (getVoid()) inserted = stack.getCount();
            if (!simulate){
                setStack(stack);
                setAmount(Math.min(stored + inserted, amount));
            }
            if (inserted == stack.getCount()) return ItemStack.EMPTY;
            return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - inserted);
        }
        return stack;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;
        ItemStack blStack = getStack();
        int stored = getAmount();
        if (blStack.isEmpty()) return ItemStack.EMPTY;
        if (stored <= amount) {
            ItemStack out = blStack.copy();
            int newAmount = stored;
            if (!simulate) {
                //setStack(ItemStack.EMPTY);
                setAmount(0);
            }
            out.setCount(newAmount);
            return out;
        } else {
            if (!simulate) {
                setAmount(stored - amount);

            }
            return ItemHandlerHelper.copyStackWithSize(blStack, amount);
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return slotLimit;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        ItemStack current = getStack();
        return current.isEmpty() || (current.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(current, stack));
    }

    public int getAmount(){
        CompoundNBT tag = getTag();
        if (tag != null && tag.contains("stored")){
            return tag.getInt("stored");
        }
        return 0;
    }

    public ItemStack getStack(){
        CompoundNBT tag = getTag();
        if (tag != null && tag.contains("blStack")){
            return ItemStack.read(tag.getCompound("blStack"));
        }
        return ItemStack.EMPTY;
    }

    private boolean getVoid(){
        CompoundNBT tag = getTag();
        if (tag != null && tag.contains("voidItems")){
            return tag.getBoolean("voidItems");
        }
        return true;
    }

    private void setAmount(int amount){
        CompoundNBT tag = getTag();
        if (tag == null){
            CompoundNBT compoundNBT = new CompoundNBT();
            compoundNBT.put("BlockEntityTag", new CompoundNBT());
            this.stack.setTag(compoundNBT);
        }
        this.stack.getTag().getCompound("BlockEntityTag").putInt("stored", amount);
    }

    private void setStack(ItemStack stack){
        CompoundNBT tag = getTag();
        if (tag == null){
            CompoundNBT compoundNBT = new CompoundNBT();
            compoundNBT.put("BlockEntityTag", new CompoundNBT());
            this.stack.setTag(compoundNBT);
        }
        this.stack.getTag().getCompound("BlockEntityTag").put("blStack", stack.serializeNBT());
    }

    private CompoundNBT getTag(){
        if (stack.hasTag() && stack.getTag().contains("BlockEntityTag")) return stack.getTag().getCompound("BlockEntityTag");
        return null;
    }



}