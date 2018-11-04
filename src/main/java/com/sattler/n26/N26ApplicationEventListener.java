package com.sattler.n26;

import java.text.NumberFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sattler.n26.producer.N26StatisticsProducer;
import com.sattler.n26.producer.N26StatisticsProducerProperties;

/**
 * N26 Statistics Application Event Listener
 * 
 * @author Pete Sattler
 */
@Component
public class N26ApplicationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(N26ApplicationEventListener.class);
    private static final ThreadFactory EXECUTOR_THREAD_FACTORY = new BasicThreadFactory.Builder().namingPattern("N26StatisticsProducer").priority(Thread.MAX_PRIORITY).daemon(false).build();
    private final N26StatisticsProducerProperties statsProducerProps;
    private final N26StatisticsProducer statsProducer;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(EXECUTOR_THREAD_FACTORY);
    private Future<Integer> producerFuture;                   // Only a single thread used

    @Autowired
    public N26ApplicationEventListener(N26StatisticsProducerProperties statsProducerProps, N26StatisticsProducer statsProducer) {
        super();
        this.statsProducerProps = statsProducerProps;
        this.statsProducer = statsProducer;
    }

    /**
     * Start-up the N26 statistics producer
     */
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws InterruptedException {
        LOGGER.info("Starting N26 statistics producer: [{}]", statsProducer);
        this.producerFuture = executorService.submit(statsProducer);
        int retries = 0;
        while (!statsProducer.hasStarted()) {
            TimeUnit.SECONDS.sleep(statsProducerProps.getStartUpCheckIntervalSeconds());
            if (retries > statsProducerProps.getStartUpMaxRetries()) {
                LOGGER.error("N26 statistics producer failed to start!!!");
                break;
            }
            retries++;
        }
        if (statsProducer.hasStarted()) {
            LOGGER.error("N26 statistics producer has started successfully");
        }
    }

    /**
     * Properly shutdown the N26 statistics producer
     */
    @EventListener
    public void onApplicationEvent(ContextClosedEvent event) throws InterruptedException, ExecutionException {
        LOGGER.info("Gracefully shutting down  N26 statistics producer: [{}]", statsProducer);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(statsProducerProps.getShutdownMaxWaitTimeSeconds(), TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        LOGGER.error("N26 statistics producer shutdown after generating [{}] transactions", NumberFormat.getInstance().format(producerFuture.get()));
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}