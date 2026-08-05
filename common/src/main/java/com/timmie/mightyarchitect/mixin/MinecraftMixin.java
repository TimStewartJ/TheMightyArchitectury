package com.timmie.mightyarchitect.mixin;

import com.timmie.mightyarchitect.control.storage.ArchitectStorage;
import com.timmie.mightyarchitect.platform.ClientHooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Client tick hook. Minecraft#tick has a single exit, so HEAD/TAIL bracket the whole tick.
 * <p>
 * Also where a resource reload reaches the mod's own content. Doing it here rather than by
 * registering a reload listener is deliberate: each loader spells that registration differently and
 * has changed the spelling twice across this matrix, whereas
 * {@code Minecraft#reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;} is byte-identical
 * on all thirteen versions (javap'd), and this mixin config already exists so no new one has to
 * name a refmap.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "tick", at = @At("HEAD"))
	private void mightyarchitect$tickPre(CallbackInfo ci) {
		ClientHooks.clientTickPre((Minecraft) (Object) this);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void mightyarchitect$tickPost(CallbackInfo ci) {
		ClientHooks.clientTickPost((Minecraft) (Object) this);
	}

	@Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
	private void mightyarchitect$onReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		// Dropped rather than reloaded: themes and palettes are read lazily, so the next request
		// picks up whatever the new pack stack provides without doing any work on the reload
		// thread.
		ArchitectStorage.onResourceReload();
	}
}

