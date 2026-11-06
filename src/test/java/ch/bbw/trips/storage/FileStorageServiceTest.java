package ch.bbw.trips.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

	@TempDir
	Path tmp;

	@Test
	void storesPngUnderRandomNameAndLoadsItAgain() throws IOException {
		FileStorageService service = new FileStorageService(tmp.toString());
		byte[] bytes = {1, 2, 3};

		String filename = service.store(new MockMultipartFile("file", "../../evil.png", "image/png", bytes));

		assertTrue(filename.endsWith(".png"));
		assertFalse(filename.contains("evil"));
		assertArrayEquals(bytes, Files.readAllBytes(tmp.resolve(filename)));
		Resource r = service.load(filename);
		assertTrue(r.exists());
	}

	@Test
	void rejectsUnsupportedContentType() throws IOException {
		FileStorageService service = new FileStorageService(tmp.toString());
		assertThrows(IllegalArgumentException.class,
				() -> service.store(new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1})));
	}

	@Test
	void rejectsEmptyFile() throws IOException {
		FileStorageService service = new FileStorageService(tmp.toString());
		assertThrows(IllegalArgumentException.class,
				() -> service.store(new MockMultipartFile("file", "a.png", "image/png", new byte[0])));
	}

	@Test
	void rejectsPathTraversalOnLoad() throws IOException {
		FileStorageService service = new FileStorageService(tmp.toString());
		assertThrows(IllegalArgumentException.class, () -> service.load("../secret.txt"));
	}
}
