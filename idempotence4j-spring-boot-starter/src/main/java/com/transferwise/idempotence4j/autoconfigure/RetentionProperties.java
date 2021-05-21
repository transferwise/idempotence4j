package com.transferwise.idempotence4j.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
public class RetentionProperties {
    /**
     * Retention period specified in a ISO-8601 format. Can only be positive.
     * Either period or duration must be filled.
     * Default value: 30 days
     */
    private String period = "P0Y0M30D";

    /**
     * Retention duration specified in ISO-6801 format. Can only be positive.
     * Either period or duration must be filled.
     * Defaults to null.
     */
    private String duration = null;
    /**
     * Purge job configuration defines how frequently we check for expired actions and defines a batch size for removal
     * Default interval: every 15 seconds
     * Default batch size: 150
     */
    @NestedConfigurationProperty
    private PurgeProperties purge = new PurgeProperties();

    @Data
    public static class PurgeProperties {
        private String schedule = "*/15 * * * * ?";
        private Integer batchSize = 150;
    }
}
