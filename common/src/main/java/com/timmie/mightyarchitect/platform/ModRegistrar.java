package com.timmie.mightyarchitect.platform;

import java.util.function.Supplier;

/**
 * Loader-agnostic content registration. Fabric registers straight into the vanilla registry,
 * NeoForge defers to its own {@code DeferredRegister}; either way the shared code just hands over a
 * name and a factory.
 */
@FunctionalInterface
public interface ModRegistrar<T> {

	void register(String name, Supplier<T> factory);
}
