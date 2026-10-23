package com.timmie.mightyarchitect;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Deliberately registers nothing. Any block or item in a vanilla registry makes Fabric's registry
 * sync (from 1.21.4) turn away players without the mod from every server or LAN host running it,
 * with no per-entry opt-out, so the theme exporter's tools are vanilla content instead - see
 * {@code ExportTools}. The server and client tests fail if anything in this namespace is ever
 * registered, and the join test if a player without the mod is refused.
 */
public class TheMightyArchitect {

	public static final String ID = "mightyarchitect";
	public static final String NAME = "The Mighty Architect";

	public static TheMightyArchitect instance;
	public static Logger logger = LogManager.getLogger();

	// ResourceLocation's public constructor was replaced by the fromNamespaceAndPath factory in
	// 1.21, and the class itself was renamed to Identifier in 1.21.11.
	//? if >=1.21.11 {
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}

	//?} else if >=1.21 {
	/*public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}

	*///?} else {
	/*public static ResourceLocation id(String path) {
		return new ResourceLocation(ID, path);
	}

	*///?}
}
