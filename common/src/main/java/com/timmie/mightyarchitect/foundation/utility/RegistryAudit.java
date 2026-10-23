package com.timmie.mightyarchitect.foundation.utility;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists what a namespace has put into the game's registries.
 * <p>
 * Lives in {@code common/} so both test companions can ask the same question without naming a key
 * type: the keys are {@code ResourceLocation} up to 1.21.10 and {@code Identifier} from 1.21.11,
 * and the companions are not Stonecutter-processed. Every key prints as {@code namespace:path} on
 * every version, which is all this needs.
 */
public final class RegistryAudit {

	private RegistryAudit() {
	}

	/** Every entry in every built-in registry whose key is in this namespace, as {@code registry/key}. */
	public static List<String> entriesIn(String namespace) {
		String prefix = namespace + ":";
		List<String> found = new ArrayList<>();
		for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
			// key() rather than REGISTRY.getKey(registry): REGISTRY is wildcard-typed on older nodes.
			Object registryKey = registry.key();
			for (Object key : registry.keySet())
				if (key.toString()
					.startsWith(prefix))
					found.add(registryKey + "/" + key);
		}
		return found;
	}
}
