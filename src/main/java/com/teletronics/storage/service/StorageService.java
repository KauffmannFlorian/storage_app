package com.teletronics.storage.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;

import java.io.InputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class StorageService {

    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations operations;

    @Autowired
    public StorageService(GridFsTemplate gridFsTemplate, GridFsOperations operations) {
        this.gridFsTemplate = gridFsTemplate;
        this.operations = operations;
    }

    public String store(MultipartFile file, Document metadata) throws IOException {
        try (InputStream is = file.getInputStream()) {
            ObjectId id = gridFsTemplate.store(is, file.getOriginalFilename(), file.getContentType(), metadata);
            return id.toHexString();
        }
    }

    public GridFSFile findGridFsFileById(String gridFsId) {
        return gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(gridFsId))));
    }

    public GridFsResource getResource(GridFSFile file) {
        return operations.getResource(file);
    }

    public void delete(String gridFsId) {
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(gridFsId))));
    }

    public Optional<GridFSFile> findByFilename(String filename) {
        return Optional.ofNullable(gridFsTemplate.findOne(new Query(Criteria.where("filename").is(filename))));
    }
}
