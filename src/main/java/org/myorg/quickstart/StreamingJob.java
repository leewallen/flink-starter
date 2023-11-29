/**
 * Something profound.
 */
package org.myorg.quickstart;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSink;

/**
 * Starting point for a Flink streaming job using the DataStream API.
 */
public final class StreamingJob {

    public static final String UPPER = "Upper";
    public static final String PRINT_SINK = "Print Sink";
    public static final String DESTINATION_TOPIC = "Destination Topic";
    public static final String PROPERTY_TOPIC_NAME = "topic";

    private StreamingJob() {
        // prevents calls from subclass
        throw new UnsupportedOperationException();
    }

    public static void main(final String[] args) throws Exception {
        final JobConfig config = JobConfig.create();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        final KafkaSource<String> source = KafkaSource
                .<String>builder()
                .setBootstrapServers(config.brokers())
                .setTopics(config.consumer().getProperty(PROPERTY_TOPIC_NAME))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .setProperties(config.consumer())
                .build();

        final KafkaSink<String> sink = KafkaSink
                .<String>builder()
                .setBootstrapServers(config.brokers())
                .setRecordSerializer(
                    KafkaRecordSerializationSchema.builder()
                        .setTopic(config.producer().getProperty(PROPERTY_TOPIC_NAME))
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setKafkaProducerConfig(config.producer())
                .build();

        final var upperCaseStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Source Topic");
        upperCaseStream
                .map(s -> s.toUpperCase())
                .uid(UPPER).name(UPPER)
                .sinkTo(new PrintSink<>(false))
                .name(PRINT_SINK).uid(PRINT_SINK);

        upperCaseStream.sinkTo(sink)
            .uid(DESTINATION_TOPIC)
            .name(DESTINATION_TOPIC);

        env.execute("Kafka Experiment");
    }
}
