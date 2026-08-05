package com.timmie.mightyarchitect.control.design.partials;

public class Trim extends Design {

	@Override
	public Design fromData(DesignData data) {
		Trim trim = new Trim();
		trim.applyData(data);
		return trim;
	}

}
