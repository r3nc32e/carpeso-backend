package com.carpeso.carpeso_backend.controller;

import com.carpeso.carpeso_backend.dto.response.ApiResponse;
import com.carpeso.carpeso_backend.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/upload/images")
    public ResponseEntity<?> uploadImages(
            @RequestParam("files") List<MultipartFile> files) {
        try {
            if (files.size() > 8) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Maximum 8 images allowed!"));
            }
            List<String> urls = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = fileUploadService.uploadFile(file, "images");
                urls.add(url);
            }
            return ResponseEntity.ok(
                    ApiResponse.success("Images uploaded!", urls));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/upload/video")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file) {
        try {
            String url = fileUploadService.uploadFile(file, "videos");
            return ResponseEntity.ok(
                    ApiResponse.success("Video uploaded!", url));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{folder}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String folder,
            @PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads")
                    .resolve(folder).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                String contentType = folder.equals("videos")
                        ? "video/mp4" : "image/jpeg";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}