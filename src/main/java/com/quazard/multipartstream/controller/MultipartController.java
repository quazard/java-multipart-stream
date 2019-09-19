package com.quazard.multipartstream.controller;

import com.google.common.collect.ImmutableList;
import com.quazard.multipartstream.config.ClientProperties;
import com.quazard.multipartstream.util.StreamResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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


    @PostMapping(value = "/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> streamBinaries(
        final HttpServletRequest servletRequest
    ) throws IOException, ServletException {
        ClientResponse clientResponse = client
            .post()
            .uri(this.clientProperties.getUrl())
            .header("content-type", MediaType.MULTIPART_FORM_DATA_VALUE)
            .syncBody(
                servletRequest
                    .getParts()
                    .stream()
                    .collect(
                        Collectors.<Part, String, Object, MultiValueMap>toMap(
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
                    )
            )
            .exchange()
            .block();

        return new ResponseEntity<Object>(
            clientResponse.bodyToMono(Map.class).block(),
            clientResponse.statusCode()
        );
    }

    @PostMapping(value = "/store", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> storeBinaries(
        final HttpServletRequest servletRequest
    ) throws IOException, ServletException {
        Map<String, String> response = new HashMap<>();

        servletRequest
            .getParts()
            .stream()
            .filter(
                p -> !(p.getContentType().equals(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            )
            .forEach(
                p -> {
                    try {
                        FileUtils.copyToFile(
                            p.getInputStream(),
                            new File("/tmp/warehouse/" + p.getSubmittedFileName())
                        );

                        response.put(p.getSubmittedFileName(), "uploaded");
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            );

        return ResponseEntity.ok(response);
    }

}
