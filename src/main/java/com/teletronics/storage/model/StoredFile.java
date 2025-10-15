package com.teletronics.storage.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "files")
@CompoundIndexes({
    @CompoundIndex(name = "user_hash_idx", def = "{'userId': 1, 'hash': 1}", unique = true),
    @CompoundIndex(name = "user_filename_idx", def = "{'userId': 1, 'filename': 1}", unique = true)
})
public class StoredFile {
    @Id
    private String id;
    private String gridFsId;
    private String filename;
    private String userId;
    @Indexed
    private Visibility visibility;; // PUBLIC / PRIVATE
    private List<String> tags;
    private String contentType;
    private long size;
    private String hash;
    private Instant uploadDate;
    @Indexed(unique = true)
    private String publicToken;
    @Transient
    private String downloadLink;

    public StoredFile() {}

    public StoredFile(String gridFsId, String filename, String userId, Visibility visibility, java.util.List<String> tags,
                      String contentType, long size, String hash, Instant uploadDate, String publicToken, String downloadLink) {
        this.gridFsId = gridFsId;
        this.filename = filename;
        this.userId = userId;
        this.visibility = visibility;
        this.tags = tags;
        this.contentType = contentType;
        this.size = size;
        this.hash = hash;
        this.uploadDate = uploadDate;
        this.publicToken = publicToken;
        this.downloadLink = downloadLink;
    }

    // getters and setters
    public String getId() { return id; }
    public String getGridFsId() { return gridFsId; }
    public void setGridFsId(String gridFsId) { this.gridFsId = gridFsId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    public java.util.List<String> getTags() { return tags; }
    public void setTags(java.util.List<String> tags) { this.tags = tags; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public java.time.Instant getUploadDate() { return uploadDate; }
    public void setUploadDate(java.time.Instant uploadDate) { this.uploadDate = uploadDate; }
    public String getPublicToken() { return publicToken; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }
    public String getDownloadLink() { return downloadLink; }
    public void setDownloadLink(String downloadLink) { this.downloadLink = downloadLink; }
}
