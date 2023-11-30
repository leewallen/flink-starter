/**
 * Something profound.
 */
package org.myorg.quickstart;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;

/**
 * Starting point for a Flink streaming job using the DataStream API.
 */
public final class StreamingJob {

    public static final String UPPER = "Upper";
    public static final String DESTINATION_TOPIC = "Destination Topic";
    public static final String PROPERTY_TOPIC_NAME = "topic";
    public static final String SOURCE_TOPIC = "Source Topic";

    private TupleTypeInfo<Tuple2<String, String>> tupleTypeInfo = new TupleTypeInfo<>(
            Types.STRING,  // Type information for the first field
            Types.STRING   // Type information for the second field
    );

    public static void main(final String[] args) throws Exception {
        final JobConfig config = JobConfig.create();


        KafkaSourceBuilder<String> sourceBuilder = KafkaSource
                .<String>builder()
                .setBootstrapServers(config.brokers())
                .setTopics(config.consumer().getProperty(PROPERTY_TOPIC_NAME))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .setProperties(config.consumer());

        KafkaSinkBuilder<String> sinkBuilder = KafkaSink
                .<String>builder()
                .setBootstrapServers(config.brokers())
                .setRecordSerializer(
                    KafkaRecordSerializationSchema.builder()
                        .setTopic(config.producer().getProperty(PROPERTY_TOPIC_NAME))
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setKafkaProducerConfig(config.producer());

        StreamingJob job = new StreamingJob();
        job.run(StreamExecutionEnvironment.getExecutionEnvironment(),
                sourceBuilder.build(),
                sinkBuilder.build());
    }

    public void run (StreamExecutionEnvironment streamEnv,
                     KafkaSource<String> kafkaSource,
                     KafkaSink<String> kafkaSink) throws Exception {
        final var sourceStream = streamEnv.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), SOURCE_TOPIC)
                .uid(SOURCE_TOPIC)
                .name(SOURCE_TOPIC)
                .returns(String.class);

        final var filteredStream = sourceStream.filter((String s) -> s != null && !s.isEmpty() && s.contains("|"))
                .uid("Filter")
                .name("Filter")
                .returns(String.class);

        final var mappedStream = filteredStream
                .map((String s) -> {
                    var split = s.split("\\|");
                    return new Tuple2<String, String>(split[0], split[1]);
                }).uid("Map").name("Map")
                .returns(tupleTypeInfo);

        final var upperStream = mappedStream.keyBy(v -> v.f0)
                .process(new UpperCase())
                .uid(UPPER)
                .name(UPPER);

        upperStream.sinkTo(kafkaSink)
                .uid(DESTINATION_TOPIC)
                .name(DESTINATION_TOPIC);

        sourceStream.getSideOutput(new OutputTag<String>("side"){}).print();
        upperStream.getSideOutput(new OutputTag<String>("side"){}).print();

        streamEnv.execute("Kafka Experiment");
    }
}
