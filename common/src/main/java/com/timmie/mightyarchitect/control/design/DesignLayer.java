package com.timmie.mightyarchitect.control.design;

import com.google.common.collect.ImmutableList;

import java.util.List;

public enum DesignLayer {
	
	Foundation("foundation", "Foundation"),
	Regular("regular", "Regular"),
	Open("open", "Open Arcs"),
	Special("special", "Special"),
	
	None("none", "None"),
	Roofing("roofing", "Roofing");
	
	private String filePath;
	private String displayName;
	
	private DesignLayer(String filePath, String displayName) {
		this.filePath = filePath;
		this.displayName = displayName;
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public boolean isExterior() {
		return this == Open;
	}
	
	public static List<DesignLayer> defaults() {
		return ImmutableList.of(Roofing);
	}
	
	
}
