package com.timmie.mightyarchitect.platform;

import java.util.function.Supplier;

/**
 * Physical-side check, set once by the loader entrypoint.
 * <p>
 * {@link #runOnClient(Supplier)} takes a supplier of the action rather than the action itself so
 * that client-only classes are never referenced from a method a dedicated server executes - the
 * same indirection Architectury's {@code EnvExecutor.runInEnv} used.
 */
public final class Env {

	private static boolean client;

	private Env() {
	}

	public static void setClient(boolean value) {
		client = value;
	}

	public static boolean isClient() {
		return client;
	}

	public static void runOnClient(Supplier<Runnable> action) {
		if (client)
			action.get()
				.run();
	}
}
