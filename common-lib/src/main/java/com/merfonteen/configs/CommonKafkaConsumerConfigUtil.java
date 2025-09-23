package com.merfonteen.configs;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

public class CommonKafkaConsumerConfigUtil {
    private final KafkaProperties props;

    public CommonKafkaConsumerConfigUtil(KafkaProperties properties) {
        this.props = properties;
    }

    public <T> ConcurrentKafkaListenerContainerFactory<String, T> containerFactory(
            Class<T> payload,
            ContainerProperties.AckMode ackMode,
            int concurrentConsumers
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, T>();
        factory.setConsumerFactory(consumerFactory(payload));
        factory.setConcurrency(concurrentConsumers);
        factory.getContainerProperties().setAckMode(ackMode);
        return factory;
    }

    public <T> ConsumerFactory<String, T> consumerFactory(Class<T> payload) {
        return new DefaultKafkaConsumerFactory<>(getCommonConsumerConfig(payload));
    }

    public <T> Map<String, Object> getCommonConsumerConfig(Class<T> payload) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, payload.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.merfonteen.*");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return config;
    }
}
