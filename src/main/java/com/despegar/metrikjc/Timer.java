package com.despegar.metrikjc;

public class Timer extends Measure {

    public Timer(String metricName, long value, long timestamp) {
	super(metricName, value, timestamp);
    }

    @Override
    public MetricType getType() {
	return MetricType.TIMER;
    }

}
