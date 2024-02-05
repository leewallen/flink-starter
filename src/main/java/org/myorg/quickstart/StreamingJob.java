/**
 * Something profound.
 */
package org.myorg.quickstart;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starting point for a Flink streaming job using the DataStream API.
 */
public final class StreamingJob {

    public static final String UPPER_UID = "Upper";
    public static final String PRINT_SINK_UID = "Print Sink";
    public static final String SOURCE_TOPIC = "Source Topic";
    public static final String PROPERTY_TOPIC_NAME = "topic";
    private static final Logger log = LoggerFactory.getLogger(StreamingJob.class);
    public static final String FILTER_UID = "Filter";
    public static final String MAP_UID = "Map";

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

        PrintSink<String> printSink = new PrintSink<>(PRINT_SINK_UID);

        StreamingJob job = new StreamingJob();
        job.run(StreamExecutionEnvironment.getExecutionEnvironment(),
                sourceBuilder.build(),
                printSink);
    }

    public void run (StreamExecutionEnvironment streamEnv,
                     KafkaSource<String> kafkaSource,
                     PrintSink<String> printSink) throws Exception {

        streamEnv.setRuntimeMode(RuntimeExecutionMode.STREAMING);

        // start a checkpoint every 1000 ms
        streamEnv.enableCheckpointing(10000);

// advanced options:

        // set mode to exactly-once (this is the default)
        streamEnv.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);

        // make sure 500 ms of progress happen between checkpoints
        streamEnv.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);

        // checkpoints have to complete within one minute, or are discarded
        streamEnv.getCheckpointConfig().setCheckpointTimeout(60000);

        // only two consecutive checkpoint failures are tolerated
        streamEnv.getCheckpointConfig().setTolerableCheckpointFailureNumber(2);

        final var sourceStream = streamEnv.fromSource(kafkaSource,
                        WatermarkStrategy.noWatermarks(),
                        SOURCE_TOPIC)
                .uid(SOURCE_TOPIC)
                .name(SOURCE_TOPIC)
                .returns(String.class);

        final var filteredStream = sourceStream
                .filter(new NullValueFilter())
                .uid(FILTER_UID)
                .name(FILTER_UID)
                .returns(String.class);

        final var mappedStream = filteredStream
                .map(new SplitValueWithPipe())
                .uid(MAP_UID)
                .name(MAP_UID)
                .returns(tupleTypeInfo);

        final var upperStream = mappedStream.keyBy(v -> v.f0.trim())
                .process(new UpperCase())
                .uid(UPPER_UID)
                .name(UPPER_UID);

        upperStream.sinkTo(printSink)
                .uid(PRINT_SINK_UID)
                .name(PRINT_SINK_UID);

        streamEnv.execute("Kafka Experiment");
    }
}
