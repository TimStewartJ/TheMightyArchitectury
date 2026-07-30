package com.timmie.mightyarchitect.foundation.utility;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
//? if >=1.21.6 {
//?} else {
/*import com.mojang.brigadier.exceptions.CommandSyntaxException;
*///?}
import com.timmie.mightyarchitect.TheMightyArchitect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public class FilesHelper {

	public static void createFolderIfMissing(String name) {
		if (!Files.isDirectory(Paths.get(name))) {
			try {
				Files.createDirectory(Paths.get(name));
			} catch (IOException e) {
				TheMightyArchitect.logger.warn("Could not create Folder: " + name);
			}
		}
	}

	/** The body of a write, run against a stream that only becomes the real file once it succeeds. */
	@FunctionalInterface
	public interface StreamWriter {
		void writeTo(OutputStream out) throws IOException;
	}

	/**
	 * Writes a file without ever leaving a half-written one behind.
	 * <p>
	 * The previous pattern here was delete, open, write, close - so a crash anywhere in between
	 * destroyed the old file and produced nothing in its place. Themes and palettes are hours of
	 * user work, so instead the content goes to a temporary sibling and only replaces the target
	 * once it is complete and closed, which the filesystem does atomically.
	 *
	 * @return whether the file was written
	 */
	public static boolean writeAtomically(Path target, StreamWriter body) {
		Path directory = target.toAbsolutePath()
			.getParent();
		Path temp = target.resolveSibling(target.getFileName() + ".tmp");
		try {
			if (directory != null)
				Files.createDirectories(directory);

			try (OutputStream out = Files.newOutputStream(temp, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
				body.writeTo(out);
			}

			try {
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException unsupported) {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;

		} catch (IOException e) {
			TheMightyArchitect.logger.error("Could not write " + target, e);
			try {
				Files.deleteIfExists(temp);
			} catch (IOException cleanup) {
				TheMightyArchitect.logger.warn("Could not remove the leftover " + temp, cleanup);
			}
			return false;
		}
	}

	public static String findFirstValidFilename(String name, String folderPath, String extension) {
		int index = 0;
		String filename;
		String filepath;
		do {
			filename = slug(name) + ((index == 0) ? "" : "_" + index) + "." + extension;
			index++;
			filepath = folderPath + "/" + filename;
		} while (Files.exists(Paths.get(filepath)));
		return filename;
	}

	public static String slug(String name) {
		return name.toLowerCase()
			.replace(' ', '_');
	}

	public static boolean saveTagCompoundAsJson(CompoundTag compound, String path) {
		return saveTagCompoundAsJson(compound, path, "  ");
	}

	public static boolean saveTagCompoundAsJsonCompact(CompoundTag compound, String path) {
		return saveTagCompoundAsJson(compound, path, "");
	}

	private static boolean saveTagCompoundAsJson(CompoundTag compound, String path, String indent) {
		return writeAtomically(Paths.get(path), out -> {
			try (Writer stream = new OutputStreamWriter(out, StandardCharsets.UTF_8);
				JsonWriter writer = new JsonWriter(stream)) {
				writer.setIndent(indent);
				Streams.write(JsonParser.parseString(compound.toString()), writer);
			}
		});
	}

	public static CompoundTag loadJsonNBT(InputStream inputStream) {
		try {
			JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(inputStream)));
			reader.setLenient(true);
			JsonElement element = Streams.parse(reader);
			reader.close();
			inputStream.close();
			//? if >=1.21.6 {
			return TagParser.create(net.minecraft.nbt.NbtOps.INSTANCE).parseCompoundFully(element.toString());
		} catch (Exception e) {
			//?} else {
			/*return TagParser.parseTag(element.toString());
		} catch (IOException e) {
			*///?}
			e.printStackTrace();
		//? if >=1.21.6 {
		//?} else {
		/*} catch (CommandSyntaxException e) {
			e.printStackTrace();
		*///?}
		}
		return null;
	}

	public static CompoundTag loadJsonResourceAsNBT(String filepath) {
		return loadJsonNBT(TheMightyArchitect.class.getClassLoader()
			.getResourceAsStream(filepath));
	}

	public static CompoundTag loadJsonAsNBT(String filepath) {
		try {
			return loadJsonNBT(Files.newInputStream(Paths.get(filepath), StandardOpenOption.READ));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
