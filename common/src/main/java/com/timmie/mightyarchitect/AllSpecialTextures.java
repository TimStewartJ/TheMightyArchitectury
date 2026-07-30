package com.timmie.mightyarchitect;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else if >=1.21.6 {
/*import net.minecraft.resources.ResourceLocation;
*///?} else {
/*import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
*///?}

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

    //? if >=1.21.11 {
    private Identifier location;
    //?} else {
    /*private ResourceLocation location;
    *///?}

    private AllSpecialTextures(String filename) {
        //? if >=1.21.11 {
        location = Identifier.fromNamespaceAndPath(TheMightyArchitect.ID,
        //?} else if >=1.21 {
        /*location = ResourceLocation.fromNamespaceAndPath(TheMightyArchitect.ID,
        *///?} else {
        /*location = new ResourceLocation(TheMightyArchitect.ID,
        *///?}
                "textures/block/marker/" + filename);
    }

    //? if >=26 {
    public Identifier getLocation() {
    //?} else if >=1.21.11 {
    /*@Deprecated
    public void bind() {
	    // In 1.21.6, texture binding is handled through RenderType
	    // This method is kept for compatibility but should use getLocation() in new code
    }

    public Identifier getLocation() {
    *///?} else if >=1.21.6 {
    /*@Deprecated
    public void bind() {
	    // In 1.21.6, texture binding is handled through RenderType
	    // This method is kept for compatibility but should use getLocation() in new code
    }

    public ResourceLocation getLocation() {
    *///?} else {
    /*public void bind() {
	    RenderSystem.setShaderTexture(0, location);
    }

    public ResourceLocation getLocation() {
    *///?}
		return location;
	}

}
