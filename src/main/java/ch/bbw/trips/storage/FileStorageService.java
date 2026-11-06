package ch.bbw.trips.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores employee photos on the local file system (EX-04, precursor to S3).
 * Only the generated file name is returned; the caller keeps it as reference.
 */
@Service
public class FileStorageService {

	private static final List<String> ALLOWED_TYPES = List.of("image/png", "image/jpeg", "image/webp");

	private final Path root;

	public FileStorageService(@Value("${app.storage.location}") String location) throws IOException {
		this.root = Paths.get(location).toAbsolutePath().normalize();
		Files.createDirectories(root);
	}

	public String store(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Datei ist leer");
		}
		if (!ALLOWED_TYPES.contains(file.getContentType())) {
			throw new IllegalArgumentException("Nicht unterstützter Dateityp: " + file.getContentType());
		}

		// Extension is derived from the validated content type, never from the client-supplied name.
		String filename = UUID.randomUUID() + "." + extensionFor(file.getContentType());

		Path target = root.resolve(filename).normalize();
		if (!root.equals(target.getParent())) {
			throw new IllegalArgumentException("Ungültiger Dateipfad");
		}

		Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
		return filename;
	}

	public Resource load(String filename) throws MalformedURLException {
		Path file = root.resolve(filename).normalize();
		if (!root.equals(file.getParent())) {
			throw new IllegalArgumentException("Ungültiger Dateipfad");
		}
		return new UrlResource(file.toUri());
	}

	public MediaType contentTypeOf(String filename) {
		String ext = StringUtils.getFilenameExtension(filename);
		if (ext == null) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		return switch (ext.toLowerCase(Locale.ROOT)) {
			case "png" -> MediaType.IMAGE_PNG;
			case "jpg" -> MediaType.IMAGE_JPEG;
			case "webp" -> MediaType.parseMediaType("image/webp");
			default -> MediaType.APPLICATION_OCTET_STREAM;
		};
	}

	private static String extensionFor(String contentType) {
		return switch (contentType) {
			case "image/png" -> "png";
			case "image/webp" -> "webp";
			default -> "jpg";
		};
	}
}
