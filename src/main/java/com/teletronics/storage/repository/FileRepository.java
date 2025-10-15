package com.teletronics.storage.repository;

import com.teletronics.storage.model.StoredFile;
import com.teletronics.storage.model.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;
import java.util.List;

public interface FileRepository extends MongoRepository<StoredFile, String> {
    Optional<StoredFile> findByUserIdAndHash(String userId, String hash);
    Optional<StoredFile> findByUserIdAndFilename(String userId, String filename);
    Optional<StoredFile> findByPublicToken(String publicToken);
    List<StoredFile> findByVisibility(Visibility visibility);
    @Query("{ 'visibility': ?0, 'tags': { $regex: ?1, $options: 'i' } }")
    Page<StoredFile> findByVisibilityAndTagsContaining(Visibility visibility, String tag, Pageable pageable);
    Page<StoredFile> findByUserId(String userId, Pageable pageable);
    @Query("{ 'userId': ?0, 'tags': { $regex: ?1, $options: 'i' } }")
    Page<StoredFile> findByUserIdAndTagsContaining(String userId, String tag, Pageable pageable);
    Page<StoredFile> findByVisibility(Visibility visibility, Pageable pageable);
}
