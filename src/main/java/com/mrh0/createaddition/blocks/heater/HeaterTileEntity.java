package com.mrh0.createaddition.blocks.heater;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.compat.immersive_engineering.IEHeaterOptional;
import com.mrh0.createaddition.config.Config;
import com.mrh0.createaddition.energy.BaseElectricTileEntity;
import com.mrh0.createaddition.mixin.AbstractFurnaceMixin;
import com.simibubi.create.AllBlocks;

import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.energy.IEnergyStorage;

public class HeaterTileEntity extends BaseElectricTileEntity {
	
	public AbstractFurnaceTileEntity  cache;
	private boolean isFurnaceEngine = false;
	public static final int CONSUMPTION = Config.HEATER_NORMAL_CONSUMPTION.get(),
			CONSUMPTION_ENGINE = Config.HEATER_FURNACE_ENGINE_CONSUMPTION.get();
	public static final boolean ALLOW_ENGINE = Config.HEATER_FURNACE_ENGINE_ENABLED.get();
	private boolean litState = false;
	
	private static final int 
	MAX_IN = Config.HEATER_MAX_INPUT.get(),
	MAX_OUT = 0,
	CAPACITY = Config.HEATER_CAPACITY.get();
	
	public HeaterTileEntity(TileEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn, CAPACITY, MAX_IN, MAX_OUT);
	}

	@Override
	public boolean isEnergyInput(Direction side) {
		return side == getBlockState().get(HeaterBlock.FACING).getOpposite();
	}

	@Override
	public boolean isEnergyOutput(Direction side) {
		return false;
	}
	
	@Override
	public void tick() {
		super.tick();
		if(world.isRemote())
			return;
		if(cache == null)
			return;
		if(cache.isRemoved())
			cache = null;
		if(cache == null)
			return;
		
		/*if(CreateAddition.IE_ACTIVE) {
			litState =  IEHeaterOptional.externalHeater(cache, energy);
		}*/
		
		IIntArray data = ((AbstractFurnaceMixin)cache).getFurnaceData();
		
		if(hasEnoughEnergy()) {
			data.set(0, Math.min(200, data.get(0)+2));
			if(!litState)
				updateState(true);
			litState = true;
		}
		else if(litState && data.get(0) < 1) {
			updateState(false);
			litState = false;
		}
	}
	
	public void refreshCache() {
		Direction d = getBlockState().get(HeaterBlock.FACING);
		TileEntity te = world.getTileEntity(pos.offset(d));
		if(te instanceof AbstractFurnaceTileEntity)
			cache = (AbstractFurnaceTileEntity) te;
		else
			cache = null;
		isFurnaceEngine = hasFurnaceEngine();
	}
	
	public boolean hasEnoughEnergy() {
		if(!ALLOW_ENGINE && isFurnaceEngine)
			return false;
		return energy.getEnergyStored() > (isFurnaceEngine ? CONSUMPTION_ENGINE * 20 : CONSUMPTION * 20);
	}
	
	public boolean hasFurnaceEngine() {
		Direction dir = getBlockState().get(HeaterBlock.FACING);
		BlockPos origin = pos.offset(dir);
		for(Direction d : Direction.values())
			if(world.getBlockState(origin.offset(d)).getBlock() == AllBlocks.FURNACE_ENGINE.get())
				return true;
		return false;
	}
	
	@Override
	public void lazyTick() {
		super.lazyTick();
		if(hasEnoughEnergy())
			energy.internalConsumeEnergy(isFurnaceEngine ? CONSUMPTION_ENGINE * 20 : CONSUMPTION * 20);
		isFurnaceEngine = hasFurnaceEngine();
	}
	
	public void updateState(boolean lit) {
		Direction d = getBlockState().get(HeaterBlock.FACING);
		BlockState state = world.getBlockState(pos.offset(d));
		if(state.getBlock() instanceof AbstractFurnaceBlock) {
			if(state.get(AbstractFurnaceBlock.LIT) != lit)
				world.setBlockState(pos.offset(d), state.with(AbstractFurnaceBlock.LIT, lit));
		}
	}
	
	@Override
	public void firstTick() {
		super.firstTick();
		refreshCache();
	}

	@Override
	public void setCache(Direction side, IEnergyStorage storage) {
	}

	@Override
	public IEnergyStorage getCachedEnergy(Direction side) {
		return null;
	}
}
