package com.timmie.mightyarchitect.test.unit;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Boots enough of Minecraft for the mod's pure logic to run outside the game.
 * <p>
 * Anything touching {@code Blocks}, a {@code BlockState} or NBT block-state serialization reaches
 * {@code BuiltInRegistries}, whose static initializer throws {@code ExceptionInInitializerError}
 * until the registries are frozen. Two calls fix that, and they are the same two on every
 * Minecraft version in the matrix - {@code SharedConstants.tryDetectVersion()} and
 * {@code Bootstrap.bootStrap()} are byte-identical from 1.19.4 through 26.2 (verified with javap
 * against every node's jar), so this class needs no Stonecutter guard and can live in the
 * version-agnostic test module.
 * <p>
 * Bootstrapping costs a few seconds and is process-global, so it happens once per JVM rather than
 * once per test class.
 */
public final class MinecraftBootstrap implements BeforeAllCallback {

	/** Applies the bootstrap to a test class. */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@ExtendWith(MinecraftBootstrap.class)
	public @interface Bootstrapped {
	}

	private static boolean booted;

	@Override
	public void beforeAll(ExtensionContext context) {
		ensureBooted();
	}

	public static synchronized void ensureBooted() {
		if (booted)
			return;
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		booted = true;
	}
}
