/**
 * Provides a wrapper around the configuration for the job.
 */
package org.myorg.quickstart;

import java.util.Properties;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;


final class JobConfig {
    public static final String TOPIC = "topic";
    private final Config config;

    private JobConfig(final Config config) {
        this.config = config;
        // This verifies that the Config is sane and has our
        // reference config. Importantly, we specify the "simple-lib"
        // path so we only validate settings that belong to this
        // library. Otherwise, we might throw mistaken errors about
        // settings we know nothing about.
        config.checkValid(ConfigFactory.defaultReference());
    }

    public static JobConfig create() {
        final Config appConfig = ConfigFactory.load();
        final Config envConfig = ConfigFactory.load(
                "application." + System.getenv("FLINK_ENV") + ".conf"
        );

        return new JobConfig(appConfig.withFallback(envConfig));
    }

    public String brokers() {
        return config.getString("kafka.endpoints");
    }

    public Properties consumer() {
        final Properties props = new Properties();

        props.setProperty("group.id", config.getString("kafka.consumer.groupId"));
        props.setProperty(TOPIC, config.getString("kafka.consumer.topic"));

        return props;
    }
}
