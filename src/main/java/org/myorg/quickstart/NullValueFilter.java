package org.myorg.quickstart;

import org.apache.flink.api.common.functions.RichFilterFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;

public class NullValueFilter extends RichFilterFunction<String> {
    private static final long serialVersionUID = 1L;

    private transient Counter nullValueCounter;

    @Override
    public void open(Configuration config) {
        this.nullValueCounter = getRuntimeContext()
                .getMetricGroup()
                .counter("nullValueCounter");
    }

    @Override
    public boolean filter(String value) throws Exception {
        if ( value != null && value.contains("|") ) {
            return true;
        } else {
            this.nullValueCounter.inc();
            return false;
        }
    }
}
