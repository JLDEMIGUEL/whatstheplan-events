package com.whatstheplan.events.testconfig.utils;

import lombok.experimental.UtilityClass;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@UtilityClass
public class S3MockUtils {

    public static void mockS3PutObject(S3AsyncClient s3ClientMock) {
        CompletableFuture<PutObjectResponse> futureResponse =
                CompletableFuture.completedFuture(PutObjectResponse.builder().eTag("mock-etag").build());

        given(s3ClientMock.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .willReturn(futureResponse);
    }

    public static void mockS3DeleteObject(S3AsyncClient s3ClientMock) {
        CompletableFuture<DeleteObjectResponse> futureResponse =
                CompletableFuture.completedFuture(DeleteObjectResponse.builder().build());

        given(s3ClientMock.deleteObject(any(DeleteObjectRequest.class)))
                .willReturn(futureResponse);
    }
}
