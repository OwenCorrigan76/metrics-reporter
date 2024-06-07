/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics;

import io.prometheus.client.exporter.HTTPServer;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class PrometheusMetricsReporterConfigTest {
    @Test
    public void testDefaults() {
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(Collections.emptyMap());
        assertEquals(PrometheusMetricsReporterConfig.LISTENER_CONFIG_DEFAULT, config.listener());
        assertTrue(config.isAllowed("random_name"));
    }

    @Test
    public void testOverrides() {
        Map<String, String> props = new HashMap<>();
        props.put(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://:0");
        props.put(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "kafka_server.*");
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props);

        assertEquals("http://:0", config.listener());
        assertFalse(config.isAllowed("random_name"));
        assertTrue(config.isAllowed("kafka_server_metric"));
    }

    @Test
    public void testAllowlist() {
        Map<String, String> props = new HashMap<>();
        props.put(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "kafka_server.*,kafka_network.*");
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props);

        assertFalse(config.isAllowed("random_name"));
        assertTrue(config.isAllowed("kafka_server_metric"));
        assertTrue(config.isAllowed("kafka_network_metric"));
    }



    @Test
    public void testListenerParseListener() {
        assertEquals(new PrometheusMetricsReporterConfig.Listener("", 8080), PrometheusMetricsReporterConfig.Listener.parseListener("http://:8080"));
        assertEquals(new PrometheusMetricsReporterConfig.Listener("123", 8080), PrometheusMetricsReporterConfig.Listener.parseListener("http://123:8080"));
        assertEquals(new PrometheusMetricsReporterConfig.Listener("::1", 8080), PrometheusMetricsReporterConfig.Listener.parseListener("http://::1:8080"));
        assertEquals(new PrometheusMetricsReporterConfig.Listener("::1", 8080), PrometheusMetricsReporterConfig.Listener.parseListener("http://[::1]:8080"));
        assertEquals(new PrometheusMetricsReporterConfig.Listener("random", 8080), PrometheusMetricsReporterConfig.Listener.parseListener("http://random:8080"));

        assertThrows(ConfigException.class, () -> PrometheusMetricsReporterConfig.Listener.parseListener("http"));
        assertThrows(ConfigException.class, () -> PrometheusMetricsReporterConfig.Listener.parseListener("http://"));
        assertThrows(ConfigException.class, () -> PrometheusMetricsReporterConfig.Listener.parseListener("http://random"));
        assertThrows(ConfigException.class, () -> PrometheusMetricsReporterConfig.Listener.parseListener("http://random:"));
        assertThrows(ConfigException.class, () -> PrometheusMetricsReporterConfig.Listener.parseListener("http://:-8080"));
        assertThrows(ConfigException.class, () -> PrometheusMetricsReporterConfig.Listener.parseListener("http://random:-8080"));
        assertThrows(ConfigException.class, () -> PrometheusMetricsReporterConfig.Listener.parseListener("http://:8080random"));
        assertThrows(ConfigException.class, () -> PrometheusMetricsReporterConfig.Listener.parseListener("randomhttp://:8080random"));
        assertThrows(ConfigException.class, () -> PrometheusMetricsReporterConfig.Listener.parseListener("randomhttp://:8080"));
    }

    @Test
    public void testValidator() {
        PrometheusMetricsReporterConfig.ListenerValidator validator = new PrometheusMetricsReporterConfig.ListenerValidator();
        validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://:0");
        validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://123:8080");
        validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://::1:8080");
        validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://[::1]:8080");
        validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://random:8080");

        assertThrows(ConfigException.class, () -> validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http"));
        assertThrows(ConfigException.class, () -> validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://"));
        assertThrows(ConfigException.class, () -> validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://random"));
        assertThrows(ConfigException.class, () -> validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://random:"));
        assertThrows(ConfigException.class, () -> validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://:-8080"));
        assertThrows(ConfigException.class, () -> validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://random:-8080"));
        assertThrows(ConfigException.class, () -> validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "http://:8080random"));
        assertThrows(ConfigException.class, () -> validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "randomhttp://:8080random"));
        assertThrows(ConfigException.class, () -> validator.ensureValid(PrometheusMetricsReporterConfig.LISTENER_CONFIG, "randomhttp://:8080"));
    }

    @Test
    public void testIsListenerEnabled() {
        Map<String, Boolean> props = new HashMap<>();
        props.put(PrometheusMetricsReporterConfig.LISTENER_ENABLE_CONFIG, true);
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props);
        Optional<HTTPServer> httpServerOptional = config.startHttpServer();

        assertTrue(httpServerOptional.isPresent());
        assertTrue(config.isListenerEnabled());
        httpServerOptional.ifPresent(HTTPServer::close);
    }

    @Test
    public void testIsListenerDisabled() {
        Map<String, Boolean> props = new HashMap<>();
        props.put(PrometheusMetricsReporterConfig.LISTENER_ENABLE_CONFIG, false);
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props);
        Optional<HTTPServer> httpServerOptional = config.startHttpServer();

        assertTrue(httpServerOptional.isEmpty());
        assertFalse(config.isListenerEnabled());
    }

    @Test
    public void testCompileAllowlistWithValidPatterns() {
        Map<String, String> props = new HashMap<>();
        props.put(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "kafka_server.*,metrics_.*,.*_total");
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props);

        Pattern compiledPattern = config.compileAllowlist(Arrays.asList("kafka_server.*", "metrics_.*", ".*_total"));

        assertTrue(compiledPattern.matcher("kafka_server_heartbeat_metrics_failed_reauthentication_total").matches());
        assertTrue(compiledPattern.matcher("metrics_latency_avg").matches());
        assertTrue(compiledPattern.matcher("some_metric_total").matches());
    }

    @Test
    public void testCompileAllowlistWithInvalidPatterns() {
        Map<String, String> props = new HashMap<>();
        props.put(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "kafka_server.*,metrics_[a,o],.*_total");
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props);

        Pattern compiledPattern = config.compileAllowlist(Arrays.asList("kafka_server.*", "metrics_[", ".*_total"));

        assertTrue(compiledPattern.matcher("kafka_server_heartbeat_metrics_failed_reauthentication_total").matches());
        assertFalse(compiledPattern.matcher("metrics,_latency,_avg").matches());  // Invalid pattern "metrics_["
        assertTrue(compiledPattern.matcher("some_metric_total").matches());
    }

    @Test
    public void testCompileAllowlistWithEmptyList() {
        Map<String, String> props = new HashMap<>();
        props.put(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "");
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props);

        Pattern compiledPattern = config.compileAllowlist(Collections.emptyList());

        assertFalse(compiledPattern.matcher("kafka_server_heartbeat_metrics_failed_reauthentication_total").matches());
    }

}

