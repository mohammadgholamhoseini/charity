package com.charity.app.service;

import com.charity.app.common.error.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Stores uploaded images and documents on local disk.
 *
 * <p>Validation looks at the file's contents, not just its name. An earlier version trusted the
 * extension alone and took anything; the version after that swung the other way and rejected every
 * office format outright, which is no longer tenable now that centres file paperwork here.
 *
 * <p>{@code docx} and {@code xlsx} are accepted, and they are the awkward pair: both are ZIP
 * containers, so their leading bytes identify them only as "a zip" -- a JAR, an APK and a zip bomb
 * all pass a magic-byte check on {@code PK 03 04}. {@link #assertOoxmlPackage} is what actually
 * separates them, by requiring the parts a real Word or Excel package must carry and by walking the
 * archive under an entry-count and an uncompressed-byte cap. Legacy {@code doc}/{@code xls} stay
 * rejected: they are OLE compound files with no comparable cheap structural check.
 */
@Slf4j
@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 10_000;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "docx", "xlsx");

    /**
     * OOXML files are ZIP containers, so their leading bytes say nothing more than "a zip".
     * {@link #assertOoxmlPackage} looks inside for the parts every OOXML package must carry.
     */
    private static final Set<String> OOXML_EXTENSIONS = Set.of("docx", "xlsx");

    /** The manifest at the root of every OOXML package, whatever the format. */
    private static final String OOXML_MANIFEST = "[content_types].xml";

    /**
     * The part that tells the two formats apart. Without it, a spreadsheet renamed to {@code .docx}
     * -- or any package produced by a tool that only writes a manifest -- would pass.
     */
    private static final Map<String, String> OOXML_REQUIRED_PART = Map.of(
            "docx", "word/document.xml",
            "xlsx", "xl/workbook.xml");

    /**
     * Zip-bomb ceilings for the structural walk. A 10 MB upload can legitimately hold a few hundred
     * entries expanding to a few hundred megabytes; a bomb holds a handful expanding to terabytes.
     * Both caps are checked against bytes and entries actually seen -- {@code ZipEntry.getSize()} is
     * a number the uploader wrote and is never consulted.
     */
    private static final int MAX_ZIP_ENTRIES = 2_000;

    private static final long MAX_UNCOMPRESSED_BYTES = 200L * 1024 * 1024;

    /** Every OOXML file is a zip, so docx and xlsx share these four bytes. */
    private static final byte[] ZIP_MAGIC = {'P', 'K', 0x03, 0x04};

    /** Leading bytes each accepted format must actually start with. */
    private static final Map<String, byte[]> MAGIC = Map.of(
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "png", new byte[]{(byte) 0x89, 'P', 'N', 'G'},
            "webp", new byte[]{'R', 'I', 'F', 'F'},
            "pdf", new byte[]{'%', 'P', 'D', 'F', '-'},
            "docx", ZIP_MAGIC,
            "xlsx", ZIP_MAGIC);

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
            throw new ValidationException("نوع فایل مجاز نیست. فرمت‌های مجاز: JPG، PNG، WEBP، PDF، DOCX و XLSX");
        }

        // Every check below opens its own stream over the upload, and so does the copy.
        //
        // This used to be one BufferedInputStream rewound with mark/reset between the checks, and
        // that silently could not work: mark(16) sets a 16-byte read-ahead limit, and
        // BufferedInputStream drops the mark as soon as its 8 KB buffer has to be refilled past it.
        // Decoding an image reads the whole image, so reset() threw "Resetting to invalid mark" for
        // every file over ~8 KB -- which is every real photo or scan. The IOException landed in the
        // catch below and the uploader got a 500 «خطا در ذخیره فایل». Only the tiny fixtures used in
        // testing ever fit inside the buffer, which is why it looked fine. A fresh stream per pass
        // costs nothing here: the servlet container has already spooled the upload to a temp file,
        // so getInputStream() is a new reader over that file rather than a re-read of the socket.
        try {
            assertMagicBytes(file, extension);

            if (isImage) {
                assertDecodableImage(file);
            }
            if (OOXML_EXTENSIONS.contains(extension)) {
                assertOoxmlPackage(file, extension);
            }

            Path root = Paths.get(uploadDir);
            Files.createDirectories(root);

            // A generated name means an attacker cannot influence the path, and the extension is one
            // we have just verified against the content.
            String filename = UUID.randomUUID() + "." + extension;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, root.resolve(filename));
            }
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

    /**
     * Removes a stored file. Best effort: a missing file is not an error, and a failure to unlink
     * must not fail the transaction that already removed the row pointing at it -- the alternative
     * is a database row the user was told was deleted and was not.
     *
     * <p>Callers are responsible for checking that no other row still references the name; see
     * {@code DocumentService.unlinkIfUnreferenced}.
     */
    public void delete(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(filename));
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to unlink stored file {}", filename, e);
        }
    }

    private void assertMagicBytes(MultipartFile file, String extension) throws IOException {
        byte[] expected = MAGIC.get(extension);
        byte[] actual = new byte[expected.length];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(actual, 0, expected.length);
            if (read < expected.length || !java.util.Arrays.equals(expected, actual)) {
                throw new ValidationException("محتوای فایل با پسوند آن هم‌خوانی ندارد");
            }
        }
    }

    /**
     * A file that merely starts with {@code PK 03 04} is only known to be a zip -- any archive
     * renamed to {@code .docx} passes that test. Two entries have to be present for it to be the
     * thing it claims: {@code [Content_Types].xml}, which every OOXML package carries at its root,
     * and the format's own main part -- {@code word/document.xml} or {@code xl/workbook.xml} --
     * which is what stops a spreadsheet, a JAR or an APK from being filed as a Word document.
     *
     * <p>The walk is bounded twice over. {@link #MAX_ZIP_ENTRIES} caps how many entries are looked
     * at, and {@link #MAX_UNCOMPRESSED_BYTES} caps the expansion. Crucially the byte count comes
     * from data actually pulled through the stream, not from {@link ZipEntry#getSize()}: that field
     * lives in the archive and is written by whoever built it, so a bomb simply declares itself
     * small. Nothing is retained -- the bytes are read into a fixed buffer and dropped -- so the
     * peak memory cost is the buffer regardless of what was uploaded.
     */
    private void assertOoxmlPackage(MultipartFile file, String extension) throws IOException {
        String requiredPart = OOXML_REQUIRED_PART.get(extension);
        boolean manifestSeen = false;
        boolean partSeen = false;
        int entries = 0;
        long uncompressed = 0;
        byte[] buffer = new byte[8192];

        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ZIP_ENTRIES) {
                    throw new ValidationException("فایل آفیس بیش از حد پیچیده است و پذیرفته نشد");
                }
                String name = normaliseEntryName(entry.getName());
                if (OOXML_MANIFEST.equals(name)) {
                    manifestSeen = true;
                } else if (requiredPart != null && requiredPart.equals(name)) {
                    partSeen = true;
                }

                int read;
                while ((read = zip.read(buffer, 0, buffer.length)) > 0) {
                    uncompressed += read;
                    if (uncompressed > MAX_UNCOMPRESSED_BYTES) {
                        throw new ValidationException("حجم محتوای فایل آفیس بیش از حد مجاز است");
                    }
                }
            }
        }
        if (!manifestSeen || !partSeen) {
            throw new ValidationException("فایل آفیس معتبر نیست");
        }
    }

    /** Entry names are compared case-insensitively and without a leading slash some writers add. */
    private static String normaliseEntryName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    /**
     * Decoding the image rejects both polyglot files that merely start with the right bytes and
     * decompression bombs whose declared dimensions are absurd.
     */
    private void assertDecodableImage(MultipartFile file) throws IOException {
        BufferedImage image;
        try (InputStream in = file.getInputStream()) {
            image = ImageIO.read(in);
        }
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
