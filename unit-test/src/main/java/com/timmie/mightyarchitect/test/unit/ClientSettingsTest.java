package com.timmie.mightyarchitect.test.unit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.timmie.mightyarchitect.control.storage.ArchitectPaths;
import com.timmie.mightyarchitect.control.storage.ClientSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The blueprint view preference survives a restart, and a file that is missing or broken falls
 * back to the effect being on. The literal key is pinned because a renamed key would silently
 * switch the effect back on for everyone who turned it off.
 */
@DisplayName("ClientSettings")
class ClientSettingsTest {

	@AfterEach
	void restore() {
		ArchitectPaths.setRootsForTesting(null, null);
		ClientSettings.reload();
	}

	private static void useRoot(Path root) {
		ArchitectPaths.setRootsForTesting(root, root);
		ClientSettings.reload();
	}

	@Test
	@DisplayName("blueprint view is on when there is no settings file")
	void onByDefault(@TempDir Path root) {
		useRoot(root);

		assertTrue(ClientSettings.blueprintView());
		assertFalse(Files.exists(root.resolve("settings.json")));
	}

	@Test
	@DisplayName("turning blueprint view off is written as BlueprintView and read back")
	void offPersists(@TempDir Path root) throws IOException {
		useRoot(root);

		ClientSettings.setBlueprintView(false);

		JsonObject written = JsonParser.parseString(Files.readString(root.resolve("settings.json")))
			.getAsJsonObject();
		assertFalse(written.get("BlueprintView")
			.getAsBoolean());

		ClientSettings.reload();
		assertFalse(ClientSettings.blueprintView());

		ClientSettings.setBlueprintView(true);
		ClientSettings.reload();
		assertTrue(ClientSettings.blueprintView());
	}

	@Test
	@DisplayName("a hand-written file is honoured")
	void handWrittenFile(@TempDir Path root) throws IOException {
		Files.writeString(root.resolve("settings.json"), "{\"BlueprintView\": false}");
		useRoot(root);

		assertFalse(ClientSettings.blueprintView());
	}

	@Test
	@DisplayName("a broken or keyless file means blueprint view on")
	void brokenFileMeansDefault(@TempDir Path root) throws IOException {
		useRoot(root);

		Files.writeString(root.resolve("settings.json"), "{not json");
		ClientSettings.reload();
		assertTrue(ClientSettings.blueprintView());

		Files.writeString(root.resolve("settings.json"), "{}");
		ClientSettings.reload();
		assertTrue(ClientSettings.blueprintView());

		Files.writeString(root.resolve("settings.json"), "{\"BlueprintView\": \"nope\"}");
		ClientSettings.reload();
		assertTrue(ClientSettings.blueprintView());
	}
}
