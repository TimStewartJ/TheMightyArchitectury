package com.timmie.mightyarchitect.foundation.compat;

import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.10 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
//?} else {
/*
*///?}

/**
 * Synthesises GUI input against a screen, for the automated client test.
 * <p>
 * The test companion is not Stonecutter-processed, so it cannot name these signatures itself - they
 * moved in 1.21.10, where the raw arguments became {@code MouseButtonEvent} and
 * {@code CharacterEvent} records. Keeping them here means the whole mod spells the input signatures
 * in exactly one place, which is only possible because the screens no longer re-dispatch input
 * themselves.
 */
public final class ScreenInput {

	private ScreenInput() {
	}

	/** Clicks at GUI coordinates. Returns whether the screen consumed the click. */
	//? if >=1.21.10 {
	public static boolean click(Screen screen, double x, double y, int button) {
		return screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0)), false);
	}
	//?} else {
	/*public static boolean click(Screen screen, double x, double y, int button) {
		return screen.mouseClicked(x, y, button);
	}
	*///?}

	/** Types one character. Returns whether the screen consumed it. */
	//? if >=26 {
	public static boolean type(Screen screen, char character) {
		return screen.charTyped(new CharacterEvent(character));
	}
	//?} else if >=1.21.10 {
	/*public static boolean type(Screen screen, char character) {
		return screen.charTyped(new CharacterEvent(character, 0));
	}
	*///?} else {
	/*public static boolean type(Screen screen, char character) {
		return screen.charTyped(character, 0);
	}
	*///?}
}
