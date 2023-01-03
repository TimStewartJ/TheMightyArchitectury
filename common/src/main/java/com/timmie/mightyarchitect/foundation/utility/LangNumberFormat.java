package com.timmie.mightyarchitect.foundation.utility;

import net.minecraft.client.Minecraft;

import java.text.NumberFormat;
import java.util.Locale;

public class LangNumberFormat {

	private NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
	public static LangNumberFormat numberFormat = new LangNumberFormat();

	public NumberFormat get() {
		return format;
	}

	public void update() {
		var langInfo = Minecraft.getInstance().getLanguageManager().getSelected();
		var minecraftLocale = new Locale(langInfo.getCode(), langInfo.getRegion());

		format = NumberFormat.getInstance(minecraftLocale);
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(0);
		format.setGroupingUsed(true);
	}

	public static String format(double d) {
		return numberFormat.get()
			.format(d)
			.replace("\u00A0", " ");
	}

}