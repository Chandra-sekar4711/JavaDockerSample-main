package com.Basic.Controller;


import java.nio.charset.StandardCharsets;
import com.Basic.ServiceImpl.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    @Autowired
    private S3Service s3Service;

    // Upload endpoint
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {

        try {
            String result = s3Service.uploadFile(file, folder);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    // Download endpoint (returns file)
    @GetMapping("/download/**")
    public ResponseEntity<byte[]> downloadFile(HttpServletRequest request) {
        try {

            String fullPath = request.getRequestURI();

            String fileName = fullPath.substring(fullPath.indexOf("/download/") + 10);

            String decodedFileName =
                    java.net.URLDecoder.decode(fileName, StandardCharsets.UTF_8);

            byte[] fileData = s3Service.downloadFile(decodedFileName);

            String originalFileName =
                    decodedFileName.substring(decodedFileName.lastIndexOf("/") + 1);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + originalFileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    // Delete endpoint
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam String key) {

        String result = s3Service.deleteFile(key);
        return ResponseEntity.ok(result);
    }

    // List files endpoint
    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles(
            @RequestParam(value = "folder", defaultValue = "UPLOADED FILE") String folder) {
        try {
            List<String> files = s3Service.listFiles(folder);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get pre-signed URL (temporary access)
    @GetMapping("/url/{fileName}")
    public ResponseEntity<String> getPresignedUrl(@PathVariable String fileName) {
        try {
            String url = s3Service.generatePresignedUrl(fileName);
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating URL: " + e.getMessage());
        }
    }
}
