package com.timmie.mightyarchitect.control.design.partials;

public class Facade extends Wall {

	@Override
	public Design fromData(DesignData data) {
		Facade facade = new Facade();
		facade.expandBehaviour = ExpandBehaviour.None;
		facade.applyData(data);
		return facade;
	}

}
