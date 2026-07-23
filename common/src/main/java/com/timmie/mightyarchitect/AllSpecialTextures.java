package com.timmie.mightyarchitect;

import net.minecraft.resources.Identifier;

public enum AllSpecialTextures {

	BLANK("blank.png"),
	CHECKERED("checkerboard.png"),
	THIN_CHECKERED("thin_checkerboard.png"),
	HIGHLIGHT_CHECKERED("highlighted_checkerboard.png"),
	SELECTION("selection.png"),
	
	FOUNDATION("foundation.png"),
	NORMAL("normal.png"),
	TOWER_FOUNDATION("tower_foundation.png"),
	TOWER_NORMAL("tower_normal.png"),
	
    Room("inner.png"),
    RoomTransparent("inner_transparent.png"),
    SelectedRoom("inner_selected.png"),
    SuperSelectedRoom("inner_super_selected.png"),
    Selection("select.png"),
    Exporter("exporter.png"),
    
    PaletteUnchanged("palette_unchanged.png"),
    PaletteChanged("palette_changed.png"),
    
    Trim("trim.png");

    private Identifier location;

    private AllSpecialTextures(String filename) {
        location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID,
                "textures/block/marker/" + filename);
    }

    public Identifier getLocation() {
		return location;
	}

}