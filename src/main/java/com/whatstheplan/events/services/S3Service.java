package com.whatstheplan.events.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final String EVENTS_PATH_PREFIX = "events/";

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    private final S3AsyncClient s3AsyncClient;

    public Mono<String> uploadFile(FilePart file) {
        String filePath = EVENTS_PATH_PREFIX + UUID.randomUUID() + "_" + file.filename();

        return DataBufferUtils.join(file.content())
                .map(this::convertDataBuffer)
                .flatMap(bytes -> {
                    PutObjectRequest request = createPutObjectRequest(file, filePath, bytes);
                    return Mono.fromFuture(s3AsyncClient.putObject(request, AsyncRequestBody.fromBytes(bytes)))
                            .thenReturn(filePath);
                })
                .doOnSuccess(path -> log.info("Uploaded file to S3 with path: {}", path))
                .doOnError(error -> log.error("Failed to upload file to S3", error));
    }

    private PutObjectRequest createPutObjectRequest(FilePart file, String filePath, byte[] bytes) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(filePath)
                .contentType(file.headers().getContentType().toString())
                .contentLength((long) bytes.length)
                .build();
    }

    private byte[] convertDataBuffer(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return bytes;
    }

}
