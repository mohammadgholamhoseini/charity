package com.charity.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx", "zip"
    );

    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("فایل خالی است");
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("حجم فایل نباید بیشتر از ۱۰ مگابایت باشد");
            }
            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf(".")).toLowerCase();
            }
            if (!ALLOWED_EXTENSIONS.contains(ext.replace(".", ""))) {
                throw new IllegalArgumentException("نوع فایل مجاز نیست. فرمت‌های مجاز: " + ALLOWED_EXTENSIONS);
            }
            Path root = Paths.get(uploadDir);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
            String filename = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), root.resolve(filename));
            return filename;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("خطا در ذخیره فایل: " + e.getMessage(), e);
        }
    }

    public Path resolve(String filename) {
        Path resolved = Paths.get(uploadDir).resolve(filename).normalize();
        Path root = Paths.get(uploadDir).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("دسترسی به مسیر مورد نظر مجاز نیست");
        }
        return resolved;
    }
}
