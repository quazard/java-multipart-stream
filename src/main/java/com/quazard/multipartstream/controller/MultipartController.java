package com.quazard.multipartstream.controller;

import com.google.common.collect.ImmutableList;
import com.quazard.multipartstream.config.ClientProperties;
import com.quazard.multipartstream.model.UploadedBinaryResponse;
import com.quazard.multipartstream.util.StreamResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class MultipartController {

    private final WebClient client;
    private final ClientProperties clientProperties;

    MultipartController(
        final WebClient.Builder clientBuilder,
        final ClientProperties clientProperties
    ) {
        this.clientProperties = clientProperties;
        this.client = clientBuilder
            .build();
    }


    @PostMapping(
        value = "/stream",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Object>> streamBinaries(
        final HttpServletRequest servletRequest
    ) throws IOException, ServletException {
        return client.post()
            .uri(this.clientProperties.getUrl())
            .headers(
                httpHeaders -> httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA)
            )
            .syncBody(streamParts(servletRequest.getParts()))
            .exchange()
            .flatMap(
                clientResponse -> clientResponse
                    .bodyToMono(String.class)
                    .map(
                        s -> new ResponseEntity<Object>(s, clientResponse.statusCode())
                    )
            )
            .switchIfEmpty(
                Mono.just(ResponseEntity.badRequest().build())
            );
    }

    @PostMapping(
        value = "/store",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> storeBinaries(
        final HttpServletRequest servletRequest
    ) throws IOException, ServletException {
        return Optional.ofNullable(servletRequest.getParts())
            .map(
                parts -> parts.stream()
                    .map(
                        p -> {
                            String status;

                            try {
                                FileUtils.copyToFile(
                                    p.getInputStream(),
                                    new File("/tmp/warehouse/" + p.getSubmittedFileName())
                                );

                                status = "uploaded";
                            } catch (IOException e) {
                                log.error(e.getMessage());
                                status = "failed";
                            }

                            return UploadedBinaryResponse.builder()
                                .fileName(p.getSubmittedFileName())
                                .contentType(p.getContentType())
                                .size(p.getSize())
                                .status(status)
                                .build();
                        }
                    )
                    .collect(Collectors.toList())
            )
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.badRequest().build());
    }


    private MultiValueMap streamParts(final Collection<Part> parts) {
        return parts.stream()
            .collect(
                Collectors.toMap(
                    Part::getName,
                    p -> {
                        try {
                            return ImmutableList.of(
                                new StreamResource(
                                    p.getInputStream(),
                                    p.getSubmittedFileName(),
                                    p.getContentType(),
                                    p.getSize()
                                )
                            );
                        } catch(IOException e) {
                            log.error(e.getMessage(), e);
                            return null;
                        }
                    },
                    (m1, m2) -> m1,
                    LinkedMultiValueMap::new
                )
            );
    }

}
