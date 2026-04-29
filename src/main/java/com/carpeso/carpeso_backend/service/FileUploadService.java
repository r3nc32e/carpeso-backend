package com.carpeso.carpeso_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadService {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    public String uploadFile(MultipartFile file, String subfolder) throws IOException {
        Path uploadPath = Paths.get(uploadDir, subfolder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = UUID.randomUUID().toString() + extension;

        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, file.getBytes());

        return "/" + uploadDir + "/" + subfolder + "/" + filename;
    }

    public void deleteFile(String fileUrl) {
        try {
            Path filePath = Paths.get(fileUrl.substring(1));
            Files.deleteIfExists(filePath);
        } catch (Exception ignored) {}
    }

    public String uploadImage(MultipartFile file) throws Exception {
        String filename = UUID.randomUUID() + "_" +
                file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dir = Paths.get(uploadDir, "images");
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), file.getBytes());
        return "/uploads/images/" + filename;
    }

    public List<String> uploadImages(MultipartFile[] files) throws Exception {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadImage(file));
        }
        return urls;
    }

    public String uploadVideo(MultipartFile file) throws Exception {
        String filename = UUID.randomUUID() + "_" +
                file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dir = Paths.get(uploadDir, "videos");
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), file.getBytes());
        return "/uploads/videos/" + filename;
    }
}