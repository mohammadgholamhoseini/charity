package com.charity.app.service;

import com.charity.app.common.error.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded images and documents on local disk.
 *
 * <p>Validation now looks at the file's contents, not just its name. The previous version trusted
 * the extension alone and accepted {@code zip}, {@code doc}, {@code docx}, {@code xls} and
 * {@code xlsx} -- none of which the product has any use for, and all of which are worth not
 * accepting from an untrusted uploader.
 */
@Slf4j
@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 10_000;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf");

    /** Leading bytes each accepted format must actually start with. */
    private static final Map<String, byte[]> MAGIC = Map.of(
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "png", new byte[]{(byte) 0x89, 'P', 'N', 'G'},
            "webp", new byte[]{'R', 'I', 'F', 'F'},
            "pdf", new byte[]{'%', 'P', 'D', 'F', '-'});

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("فایل خالی است");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("حجم فایل نباید بیشتر از ۱۰ مگابایت باشد");
        }

        String extension = extensionOf(file.getOriginalFilename());
        boolean isImage = IMAGE_EXTENSIONS.contains(extension);
        if (!isImage && !DOCUMENT_EXTENSIONS.contains(extension)) {
            throw new ValidationException("نوع فایل مجاز نیست. فرمت‌های مجاز: JPG، PNG، WEBP و PDF");
        }

        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            in.mark(16);
            assertMagicBytes(in, extension);
            in.reset();

            if (isImage) {
                assertDecodableImage(in);
                in.reset();
            }

            Path root = Paths.get(uploadDir);
            Files.createDirectories(root);

            // A generated name means an attacker cannot influence the path, and the extension is one
            // we have just verified against the content.
            String filename = UUID.randomUUID() + "." + extension;
            Files.copy(in, root.resolve(filename));
            return filename;
        } catch (IOException e) {
            log.error("Failed to store upload", e);
            throw new IllegalStateException("خطا در ذخیره فایل", e);
        }
    }

    public Path resolve(String filename) {
        Path root = Paths.get(uploadDir).normalize().toAbsolutePath();
        Path resolved = root.resolve(filename).normalize();
        if (!resolved.startsWith(root)) {
            throw new ValidationException("دسترسی به مسیر مورد نظر مجاز نیست");
        }
        return resolved;
    }

    public List<String> storeAll(List<MultipartFile> files) {
        return files.stream().map(this::store).toList();
    }

    private void assertMagicBytes(InputStream in, String extension) throws IOException {
        byte[] expected = MAGIC.get(extension);
        byte[] actual = new byte[expected.length];
        int read = in.readNBytes(actual, 0, expected.length);
        if (read < expected.length || !java.util.Arrays.equals(expected, actual)) {
            throw new ValidationException("محتوای فایل با پسوند آن هم‌خوانی ندارد");
        }
    }

    /**
     * Decoding the image rejects both polyglot files that merely start with the right bytes and
     * decompression bombs whose declared dimensions are absurd.
     */
    private void assertDecodableImage(InputStream in) throws IOException {
        BufferedImage image = ImageIO.read(in);
        if (image == null) {
            throw new ValidationException("فایل تصویری معتبر نیست");
        }
        if (image.getWidth() > MAX_IMAGE_DIMENSION || image.getHeight() > MAX_IMAGE_DIMENSION) {
            throw new ValidationException("ابعاد تصویر بیش از حد مجاز است");
        }
    }

    private static String extensionOf(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
