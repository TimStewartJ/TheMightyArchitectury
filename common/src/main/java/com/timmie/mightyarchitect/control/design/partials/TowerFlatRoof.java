package com.timmie.mightyarchitect.control.design.partials;

import net.minecraft.nbt.CompoundTag;

public class TowerFlatRoof extends TowerRoof {

	@Override
	public Design fromNBT(CompoundTag compound) {
		TowerFlatRoof towerRoof = new TowerFlatRoof();
		towerRoof.applyNBT(compound);
		//? if >=1.21.6 {
		towerRoof.radius = compound.getInt("Radius").orElse(0);
		//?} else {
		/*towerRoof.radius = compound.getInt("Radius");
		*///?}
		towerRoof.defaultWidth = towerRoof.radius * 2 + 1;
		return towerRoof;
	}

}
