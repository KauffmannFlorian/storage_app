package com.teletronics.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for renaming a file")
public class RenameRequest {

    @Schema(description = "New filename for the file", example = "updated_filename.pdf")
    private String filename;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
