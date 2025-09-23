package com.merfonteen.likeservice.config;

import com.merfonteen.configs.CommonKafkaConsumerConfigUtil;
import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

@RequiredArgsConstructor
@Configuration
public class KafkaConsumerConfig {
    private final KafkaProperties kafkaProperties;

    private CommonKafkaConsumerConfigUtil getConfigUtil() {
        return new CommonKafkaConsumerConfigUtil(kafkaProperties);
    }

    // ---------- post-created-event ----------

    @Bean
    public ConsumerFactory<String, PostCreatedEvent> postCreatedConsumerFactory() {
        return getConfigUtil().consumerFactory(PostCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostCreatedEvent> postCreatedContainerFactory() {
        return getConfigUtil().containerFactory(PostCreatedEvent.class, ContainerProperties.AckMode.MANUAL, 3);
    }

    // ---------- post-removed-event ----------

    @Bean
    public ConsumerFactory<String, PostRemovedEvent> postRemovedConsumerFactory() {
        return getConfigUtil().consumerFactory(PostRemovedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostRemovedEvent> postRemovedContainerFactory() {
        return getConfigUtil().containerFactory(PostRemovedEvent.class, ContainerProperties.AckMode.MANUAL, 3);
    }
}
