package org.myorg.quickstart;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;

public class SplitValueWithPipe extends RichMapFunction<String, Tuple2<String, String>> {
    private static final long serialVersionUID = 1L;

    private transient Counter pipeValueCounter;

    @Override
    public void open(Configuration config) {
        this.pipeValueCounter = getRuntimeContext()
                .getMetricGroup()
                .counter("pipeValueCounter");
    }

    @Override
    public Tuple2<String, String> map(String value) throws Exception {
        String[] words = value.split("\\|");
        if (words.length == 2) {
            this.pipeValueCounter.inc();
            return new Tuple2<>(words[0], words[1]);
        } else {
            return new Tuple2<>(value, "");
        }
    }
}
