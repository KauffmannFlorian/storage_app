package com.teletronics.storage.service;

import com.teletronics.storage.model.StoredFile;
import com.teletronics.storage.model.Visibility;
import com.teletronics.storage.repository.FileRepository;
import org.apache.tika.Tika;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.DigestUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    private final StorageService storageService;
    private final FileRepository fileRepository;
    private final Tika tika = new Tika();

    public FileService(StorageService storageService, FileRepository fileRepository) {
        this.storageService = storageService;
        this.fileRepository = fileRepository;
    }

    public StoredFile upload(MultipartFile file, String userId, String visibilityStr, List<String> tags) throws IOException {
        if (tags == null) tags = List.of();
        if (tags.size() > 5) {
            throw new IllegalArgumentException("Max 5 tags allowed");
        }

        Visibility visibility;
        try {
            visibility = Visibility.valueOf(visibilityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid visibility value. Must be PUBLIC or PRIVATE.");
        }

        // compute hash (md5)
        String hash = DigestUtils.md5DigestAsHex(file.getInputStream());

        Optional<StoredFile> existingByHash = fileRepository.findByUserIdAndHash(userId, hash);
        if (existingByHash.isPresent()) {
            throw new IllegalArgumentException("File with same content already uploaded by this user.");
        }

        Optional<StoredFile> existingByName = fileRepository.findByUserIdAndFilename(userId, file.getOriginalFilename());
        if (existingByName.isPresent()) {
            throw new IllegalArgumentException("File with same name already exists for this user.");
        }

        String detectedType = tika.detect(file.getInputStream());

        // metadata for gridfs
        Document meta = new Document();
        meta.put("userId", userId);
        meta.put("visibility", visibility);
        meta.put("tags", tags);
        meta.put("hash", hash);
        meta.put("contentType", detectedType);

        String gridFsId = null;
        try {
            gridFsId = storageService.store(file, meta);
            String publicToken = UUID.randomUUID().toString();
            String downloadLink = "/files/download/" + publicToken;

            StoredFile sf = new StoredFile(gridFsId, file.getOriginalFilename(), userId, visibility, tags,
                    file.getContentType(), file.getSize(), hash, Instant.now(), publicToken, downloadLink);
            fileRepository.save(sf);
            return sf;
        } catch (DuplicateKeyException e) {
            // Unique constraint at DB level (user+hash or user+filename) prevented duplicate
            throw new IllegalStateException("File already exists (concurrent upload or duplicate)", e);
        } catch (IOException e) {
            // propagate
            throw e;
        }
    }

    public Page<StoredFile> listFiles(String userId, String visibility, String tag, Pageable pageable) {
        Page<StoredFile> files;
        if (userId != null) {
            if (tag != null) files = fileRepository.findByUserIdAndTagsContaining(userId, tag, pageable);
            else files =  fileRepository.findByUserId(userId, pageable);
        } else {
            if (visibility != null) {
                if (tag != null) files =  fileRepository.findByVisibilityAndTagsContaining(Visibility.valueOf(visibility.toUpperCase()), tag, pageable);
                else files = fileRepository.findByVisibility(Visibility.valueOf(visibility.toUpperCase()), pageable);
            } else {
                // default to public only
                if (tag != null) files = fileRepository.findByVisibilityAndTagsContaining(Visibility.valueOf("PUBLIC"), tag, pageable);
                files = fileRepository.findByVisibility(Visibility.valueOf("PUBLIC"), pageable);
            }
        }
        return new PageImpl<>(
                files.getContent().stream()
                        .peek(file -> file.setDownloadLink("/files/download/" + file.getPublicToken()))
                        .collect(Collectors.toList()),
                pageable,
                files.getTotalElements()
        );
    }

    public java.util.List<StoredFile> listPublic() {
        List<StoredFile> publicFiles = fileRepository.findByVisibility(Visibility.PUBLIC);

        publicFiles.forEach(file ->
                file.setDownloadLink("/files/download/" + file.getPublicToken())
        );

        return publicFiles;    }

    public void delete(String id, String userId) {
        StoredFile sf = fileRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("File not found"));
        if (!sf.getUserId().equals(userId)) {
            throw new SecurityException("User is not allowed to delete the file");
        }
        storageService.delete(sf.getGridFsId());
        fileRepository.deleteById(id);
    }

    public StoredFile renameFile(String fileId, String userId, String newFilename) {
        if (newFilename == null || newFilename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be empty");
        }

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("File not found"));

        if (!file.getUserId().equals(userId)) {
            throw new SecurityException("You can only rename your own files");
        }

        Optional<StoredFile> duplicate = fileRepository.findByUserIdAndFilename(userId, newFilename);
        if (duplicate.isPresent()) {
            throw new IllegalArgumentException("A file with the same name already exists");
        }

        file.setFilename(newFilename);
        return fileRepository.save(file);
    }
}
