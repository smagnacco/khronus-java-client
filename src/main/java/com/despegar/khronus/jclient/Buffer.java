package com.despegar.khronus.jclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores a list of metrics in memory and periodically sends them to Khronus.
 *
 */
public class Buffer {
    private static final Logger LOG = LoggerFactory.getLogger(Buffer.class);
    /**
     * Metrics stored in memory
     */
    private final LinkedBlockingQueue<Measure> measures;
    /**
     * Flush periodically the queue and send the metrics to Khronus cluster
     */
    private final ScheduledExecutorService executor;
    /**
     * Http client wrapper
     */
    private final Sender sender;
    /**
     * json serializer
     */
    private JsonSerializer jsonSerializer;
    
    

    public Buffer(KhronusConfig config) {
	this.measures = new LinkedBlockingQueue<>(config.getMaximumMeasures());
	this.sender = new Sender(config);
	this.jsonSerializer = new JsonSerializer(config.getSendIntervalMillis(), config.getApplicationName());
	
	BasicThreadFactory threadFactory = new BasicThreadFactory.Builder().namingPattern("KhronusClientSender").build();
	this.executor = Executors.newScheduledThreadPool(1, threadFactory);
	this.executor.scheduleWithFixedDelay(send(), config.getSendIntervalMillis(), config.getSendIntervalMillis(), TimeUnit.MILLISECONDS);
	
	LOG.debug("Buffer to store metrics created [MaximumMeasures: %d; SendIntervalMillis: %d]",
		config.getMaximumMeasures(),config.getSendIntervalMillis());
    }

    public void add(Measure measure) {
	if (!measures.offer(measure)){
	    LOG.warn("Could not add measure because the buffer is full. Measure discarted");
	}
    }

    /**
     * Flush periodically the queue and send the metrics to Khronus cluster
     */
    private Runnable send() {
	return new Runnable() {
	    @Override
	    public void run() {
		LOG.debug("Starting new tick to send metrics");
		Collection<Measure> copiedMeasures = new ArrayList<>();
		measures.drainTo(copiedMeasures);
		
		String json = jsonSerializer.serialize(copiedMeasures);
		
		LOG.trace("Json to be posted to Khronus: {}", json);
		
		sender.send(json);
	    }
	};
    }
    
}