package com.quazard.multipartstream.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class UploadedBinaryResponse implements Serializable {

    private String fileName;
    private String contentType;
    private long size;
    private String status;

}
