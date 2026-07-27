package com.timmie.mightyarchitect.gui;

//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
//? if >=1.21.10 {
import net.minecraft.client.input.KeyEvent;
//?} else {
/*
*///?}
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class TextInputPromptScreen extends AbstractSimiScreen {

	private Consumer<String> callback;
	private Consumer<String> abortCallback;

	private EditBox nameField;
	private Button confirm;
	private Button abort;

	private Component buttonTextConfirm;
	private Component buttonTextAbort;
	private Component title;

	private boolean confirmed;

	public TextInputPromptScreen(Consumer<String> callBack, Consumer<String> abortCallback) {
		super();
		this.callback = callBack;
		this.abortCallback = abortCallback;

		buttonTextConfirm = Component.literal("Confirm");
		buttonTextAbort = Component.literal("Abort");
		confirmed = false;
	}

	@Override
	public void init() {
		super.init();
		setWindowSize(ScreenResources.TEXT_INPUT.width, ScreenResources.TEXT_INPUT.height + 30);

		this.nameField =
			new EditBox(font, topLeftX + 33, topLeftY + 26, 128, 8, Component.literal(""));
		this.nameField.setTextColor(-1);
		this.nameField.setTextColorUneditable(-1);
		this.nameField.setBordered(false);
		this.nameField.setMaxLength(35);
		this.nameField.setFocused(true);

		confirm = Button.builder(buttonTextConfirm, button -> {
			callback.accept(nameField.getValue());
			confirmed = true;
			minecraft.setScreen(null);
		}).pos(topLeftX - 5, topLeftY + 50).size(100, 20).build();

		abort = Button.builder(buttonTextAbort, button -> {
			minecraft.setScreen(null);
		}).pos(topLeftX + 100, topLeftY + 50).size(100, 20).build();

		widgets.add(confirm);
		widgets.add(abort);
		widgets.add(nameField);
	}

	@Override
	//? if >=26 {
	public void renderWindow(GuiGraphicsExtractor ms, int mouseX, int mouseY, float partialTicks) {
	//?} else {
	/*public void renderWindow(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
	*///?}
		ScreenResources.TEXT_INPUT.draw(ms, topLeftX, topLeftY);
		//? if >=26 {
		ms.text(font, title, topLeftX + (sWidth / 2) - (font.width(title) / 2), topLeftY + 11,
		//?} else {
		/*ms.drawString(font, title, topLeftX + (sWidth / 2) - (font.width(title) / 2), topLeftY + 11,
		*///?}
			ScreenResources.FONT_COLOR);
	}

	@Override
	public void removed() {
		if (!confirmed)
			abortCallback.accept(nameField.getValue());
		super.removed();
	}

	public void setButtonTextConfirm(String buttonTextConfirm) {
		this.buttonTextConfirm = Component.literal(buttonTextConfirm);
	}

	public void setButtonTextAbort(String buttonTextAbort) {
		this.buttonTextAbort = Component.literal(buttonTextAbort);
	}

	public void setTitle(String title) {
		this.title = Component.literal(title);
	}

	@Override
	//? if >=1.21.10 {
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ENTER) {
			confirm.onPress(null);
	//?} else {
	/*public boolean keyPressed(int keyCode, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			confirm.onPress();
	*///?}
			return true;
		}
		//? if >=1.21.10 {
		if (event.key() == 256 && this.shouldCloseOnEsc()) {
		//?} else {
		/*if (keyCode == 256 && this.shouldCloseOnEsc()) {
		*///?}
			this.onClose();
			return true;
		}
		//? if >=1.21.10 {
		return nameField.keyPressed(event);
		//?} else {
		/*return nameField.keyPressed(keyCode, p_keyPressed_2_, p_keyPressed_3_);
		*///?}
	}

}
