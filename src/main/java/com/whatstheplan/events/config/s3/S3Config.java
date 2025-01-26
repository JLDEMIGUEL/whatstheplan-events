package com.whatstheplan.events.config.s3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public S3AsyncClient s3Client(S3Properties s3Props) {
        return S3AsyncClient.builder()
                .httpClient(NettyNioAsyncHttpClient.builder().build())
                .region(Region.of(s3Props.getRegion()))
                .credentialsProvider(() -> AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey()))
                .build();
    }
}
