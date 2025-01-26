package com.whatstheplan.events.config.s3;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("aws.s3")
public class S3Properties {
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String region;
}
