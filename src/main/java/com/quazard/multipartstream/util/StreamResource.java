package com.quazard.multipartstream.util;

import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

public class StreamResource extends InputStreamResource {

    private long contentLength;
    private String originalFileName;
    private String contentType;

    public StreamResource(
        InputStream inputStream,
        String originalFileName,
        String contentType,
        long contentLength
    ) {
        super(inputStream);
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    @Override
    public long contentLength() throws IOException {
        return this.contentLength;
    }

    @Override
    public String getFilename() {
        return this.originalFileName;
    }

    public String getContentType() {
        return this.contentType;
    }

}
