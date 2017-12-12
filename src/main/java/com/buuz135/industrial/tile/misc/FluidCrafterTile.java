package com.buuz135.industrial.tile.misc;

import com.buuz135.industrial.tile.CustomSidedTileEntity;
import com.buuz135.industrial.tile.block.FluidCrafterBlock;
import com.buuz135.industrial.utils.CraftingUtils;
import com.buuz135.industrial.utils.Reference;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.ndrei.teslacorelib.gui.BasicRenderedGuiPiece;
import net.ndrei.teslacorelib.gui.BasicTeslaGuiContainer;
import net.ndrei.teslacorelib.gui.IGuiContainerPiece;
import net.ndrei.teslacorelib.gui.LockedInventoryTogglePiece;
import net.ndrei.teslacorelib.inventory.BoundingRectangle;
import net.ndrei.teslacorelib.inventory.FluidTankType;
import net.ndrei.teslacorelib.inventory.LockableItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FluidCrafterTile extends CustomSidedTileEntity {

    private IFluidTank tank;
    private LockableItemHandler crafting;
    private ItemStackHandler output;
    private int tick;

    public FluidCrafterTile() {
        super(FluidCrafterTile.class.getName().hashCode());
    }

    @Override
    protected void initializeInventories() {
        super.initializeInventories();
        tank = this.addSimpleFluidTank(8000, "tank", EnumDyeColor.BLUE, 18, 25, FluidTankType.BOTH, fluidStack -> true, fluidStack -> true);
        crafting = (LockableItemHandler) this.addSimpleInventory(9, "crafting", EnumDyeColor.GREEN, "crafting", new BoundingRectangle( 58,25,18*3,18*3), (stack, integer) -> true, (stack, integer) -> false, true, null);
        output = (ItemStackHandler) this.addSimpleInventory(1, "output", EnumDyeColor.ORANGE, "output", new BoundingRectangle( 58+18*5,25+18,18,18), (stack, integer) -> false, (stack, integer) -> true, false, null);
        tick = 0;
    }

    @Override
    protected void innerUpdate() {
        if (this.world.isRemote) return;
        ++tick;
        if (crafting.getLocked() && tick >= 40 && hasOnlyOneFluid()){
            Fluid fluid = getRecipeFluid();
            if (fluid == null) return;
            int bucketAmount = getFluidAmount(fluid);
            FluidStack stack = tank.drain(bucketAmount*1000, false);
            if (stack != null && stack.getFluid().equals(fluid) && stack.amount == bucketAmount*1000){ //HAS ALL THE FLUIDS
                ItemStack output = CraftingUtils.findOutput(this.world, crafting.getFilter());
                if (output.isEmpty()) return;
                if (ItemHandlerHelper.insertItem(this.output, output, true).isEmpty() && areAllSolidsPresent()){
                    for (int i = 0; i < crafting.getSlots(); ++i){
                        if (crafting.getFilter()[i].hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) continue;
                        crafting.getStackInSlot(i).shrink(1);
                    }
                    tank.drain(bucketAmount*1000, true);
                    ItemHandlerHelper.insertItem(this.output, output, false);
                }
            }
            tick = 0;
        }
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound compound) {
        tick = compound.getInteger("Tick");
        super.readFromNBT(compound);
    }

    @NotNull
    @Override
    public NBTTagCompound writeToNBT(@NotNull NBTTagCompound compound) {
        NBTTagCompound compound1 = super.writeToNBT(compound);
        compound1.setInteger("Tick", tick);
        return compound1;
    }

    @NotNull
    @Override
    public List<IGuiContainerPiece> getGuiContainerPieces(BasicTeslaGuiContainer<?> container) {
        List<IGuiContainerPiece> pieces = super.getGuiContainerPieces(container);
        pieces.add(new BasicRenderedGuiPiece(118, 25 + 20, 25, 18, new ResourceLocation(Reference.MOD_ID, "textures/gui/jei.png"), 24, 5));
        pieces.add(new LockedInventoryTogglePiece(18 * 6+8, 25+39, this, EnumDyeColor.GREEN));
        return pieces;
    }

    public Fluid getRecipeFluid(){
        for (ItemStack stack : crafting.getFilter()){
            if (stack.isEmpty()) continue;
            if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)){
                IFluidHandlerItem fluidHandlerItem = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
                return fluidHandlerItem.drain(Integer.MAX_VALUE , false).getFluid();
            }
        }
        return null;
    }

    public int getFluidAmount(Fluid fluid){
        int i = 0;
        for (ItemStack stack : crafting.getFilter()){
            if (stack.isEmpty()) continue;
            if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)){
                IFluidHandlerItem fluidHandlerItem = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
                if  (fluidHandlerItem.drain(Integer.MAX_VALUE , false).getFluid().equals(fluid)) ++i;
            }
        }
        return i;
    }

    public boolean areAllSolidsPresent(){
        for (int i = 0; i < crafting.getSlots(); ++i){
            if (crafting.getFilter()[i].hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null) || crafting.getFilter()[i].isEmpty()) continue;
            if (crafting.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    protected boolean supportsAddons() {
        return false;
    }

    public boolean hasOnlyOneFluid(){
        List<Fluid> fluids = new ArrayList<>();
        for (ItemStack stack : crafting.getFilter()){
            if (stack.isEmpty()) continue;
            if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)){
                IFluidHandlerItem fluidHandlerItem = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
                Fluid fluid = fluidHandlerItem.drain(Integer.MAX_VALUE , false).getFluid();
                if (!fluids.contains(fluid)) fluids.add(fluid);
            }
        }
        return fluids.size() == 1;
    }
}
