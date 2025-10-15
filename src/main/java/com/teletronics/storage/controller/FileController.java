package com.teletronics.storage.controller;

import com.teletronics.storage.dto.RenameRequest;
import com.teletronics.storage.model.StoredFile;
import com.teletronics.storage.service.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
@Tag(name = "File API", description = "Endpoints for file management")
public class FileController {

    private final FileService fileService;
    public FileController(FileService fileService) { this.fileService = fileService; }

    @PostMapping(value="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestHeader("X-User-Id") String userId,
                                             @RequestParam(value="file", required=true) MultipartFile file,
                                             @RequestParam(value = "visibility", defaultValue = "PRIVATE") String visibility,
                                             @RequestParam(value = "tags", required = false) List<String> tags) throws IOException {

        if (file == null || file.isEmpty()) {
            Map<String, String> error = Map.of(
                    "error", "Missing file in request. Please provide a non-empty multipart/form-data field named 'file'."
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (tags == null) tags = List.of();
        StoredFile stored = fileService.upload(file, userId, visibility, tags);
        return ResponseEntity.ok(stored);
    }

    @GetMapping
    public ResponseEntity<Page<StoredFile>> listFiles(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(value = "visibility", required = false) String visibility,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "sortBy", defaultValue = "filename") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") String direction,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        PageRequest pr = PageRequest.of(page, size, sort);
        Page<StoredFile> result = fileService.listFiles(userId, visibility, tag, pr);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/public")
    public ResponseEntity<List<StoredFile>> listPublic() {
        return ResponseEntity.ok(fileService.listPublic());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id,
                                       @RequestHeader("X-User-Id") String userId) {
        fileService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<StoredFile> rename(
            @PathVariable("id") String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody RenameRequest request
    ) {
        StoredFile updated = fileService.renameFile(id, userId, request.getFilename());
        return ResponseEntity.ok(updated);
    }
}
