package org.jenkinsci.plugins.prometheus.service;

import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.prometheus.config.PrometheusConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class})
// PowerMockIgnore needed for: https://github.com/powermock/powermock/issues/864
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "com.sun.org.apache.xalan.*"})
public class PrometheusAsyncWorkerTest {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusAsyncWorkerTest.class);

    @Mock
    private Jenkins jenkins;

    @Mock
    private PrometheusConfiguration configuration;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(jenkins.getDescriptor(PrometheusConfiguration.class)).thenReturn(configuration);
        PowerMockito.when(configuration.isUsePushgateway()).thenReturn(true);
        PowerMockito.when(configuration.getPushgatewayAddress()).thenReturn("localhost:9091");
        PowerMockito.when(configuration.getPushgatewayJobAttributeName()).thenReturn("pg_job");
    }

    @Test
    public void shouldCollectMetrics() {
        // given
        PrometheusAsyncWorker asyncWorker = new PrometheusAsyncWorker();
        PrometheusMetrics metrics = new TestPrometheusMetrics();
        asyncWorker.setPrometheusMetrics(metrics);

        // when
        asyncWorker.execute(null);

        // then
        String actual = metrics.getMetrics();
        assertThat(actual).isEqualTo("1");
    }

    private static class TestPrometheusMetrics implements PrometheusMetrics {
        private final AtomicReference<String> cachedMetrics = new AtomicReference<>("");
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public String getMetrics() {
            return cachedMetrics.get();
        }

        @Override
        public void collectMetrics() {
            String metrics = String.valueOf(counter.incrementAndGet());
            cachedMetrics.set(metrics);
        }

        @Override
        public void sendMetrics() {
            logger.debug("Prometheus metrics sent");
        }
    }
}
