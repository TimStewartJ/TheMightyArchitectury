package com.timmie.mightyarchitect.control.design.partials;

public class TowerFlatRoof extends TowerRoof {

	@Override
	public Design fromData(DesignData data) {
		TowerFlatRoof towerRoof = new TowerFlatRoof();
		towerRoof.applyData(data);
		towerRoof.radius = data.radius();
		towerRoof.defaultWidth = towerRoof.radius * 2 + 1;
		return towerRoof;
	}

}
