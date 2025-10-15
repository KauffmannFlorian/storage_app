package com.teletronics.storage.controller;

import com.teletronics.storage.model.StoredFile;
import com.teletronics.storage.model.Visibility;
import com.teletronics.storage.repository.FileRepository;
import com.teletronics.storage.service.StorageService;
import com.mongodb.client.gridfs.model.GridFSFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/files")
@Tag(name = "Download", description = "Endpoint for file download management")
public class DownloadController {

    private final StorageService storageService;
    private final FileRepository fileRepository;

    public DownloadController(StorageService storageService, FileRepository fileRepository) {
        this.storageService = storageService;
        this.fileRepository = fileRepository;
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<?> download(@PathVariable("token") String token,
                                      @RequestHeader("X-User-Id") String userId) {
        try {
            StoredFile storedFile = fileRepository.findByPublicToken(token)
                    .orElseThrow(() -> new NoSuchElementException("Invalid or expired download token"));

            if (storedFile.getVisibility() == Visibility.PRIVATE &&
                    !storedFile.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not allowed to download this file"));
            }

            GridFSFile file = storageService.findGridFsFileById(storedFile.getGridFsId());
            if (file == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "File content not found"));
            }

            GridFsResource resource = storageService.getResource(file);
            InputStreamResource inputStreamResource = new InputStreamResource(resource.getInputStream());

            String contentType = (file.getMetadata() != null && file.getMetadata().getString("contentType") != null)
                    ? file.getMetadata().getString("contentType")
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(inputStreamResource);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
