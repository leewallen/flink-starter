package org.myorg.quickstart;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.io.Serializable;

public class UpperCase extends KeyedProcessFunction<String, Tuple2<String, String>, String> implements Serializable {

    private static final long serialVersionUID = 1L;
    private transient ValueState<CountWithTimestamp> state;
    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
        super.open(parameters);
        state = getRuntimeContext().getState(new ValueStateDescriptor<>("nullEmptyCounts", CountWithTimestamp.class));

    }


    @Override
    public void processElement(Tuple2<String, String> value, KeyedProcessFunction<String, Tuple2<String, String>, String>.Context ctx, Collector<String> out) throws Exception {

        var current = state.value();
        if (current == null) {
            current = new CountWithTimestamp();
            current.key = value.f0;
            current.lastModified = ctx.timestamp();
            current.count = 0;
        }

        if (value.f1 == null || value.f1.isEmpty()) {
            current.count++;
            current.lastModified = ctx.timestamp();
        } else {
            out.collect(value.f1.toUpperCase());
        }

        // write the state back
        state.update(current);

        // schedule the next timer 60 seconds from the current event time
        ctx.timerService().registerEventTimeTimer(current.lastModified + 60000);
    }

    @Override
    public void onTimer(
            long timestamp,
            OnTimerContext ctx,
            Collector<String> out) throws Exception {

        // get the state for the key that scheduled the timer
        CountWithTimestamp result = state.value();

        // check if this is an outdated timer or the latest timer
        if (timestamp == result.lastModified + 60000) {
            // emit the count on timeout
            ctx.output(
                new OutputTag<String>("side"){},
                result.key + ":" + result.count);
        }
    }
}
