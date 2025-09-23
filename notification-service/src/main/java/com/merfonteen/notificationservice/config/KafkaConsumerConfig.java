package com.merfonteen.notificationservice.config;

import com.merfonteen.configs.CommonKafkaConsumerConfigUtil;
import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.kafkaEvents.CommentRemovedEvent;
import com.merfonteen.kafkaEvents.LikeRemovedEvent;
import com.merfonteen.kafkaEvents.LikeSentEvent;
import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.kafkaEvents.SubscriptionCreatedEvent;
import com.merfonteen.kafkaEvents.SubscriptionRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@RequiredArgsConstructor
@Configuration
public class KafkaConsumerConfig {
    private final KafkaProperties properties;

    private CommonKafkaConsumerConfigUtil getConfigUtil() {
        return new CommonKafkaConsumerConfigUtil(properties);
    }

    // ---------- comment-created-event ----------

    @Bean
    public ConsumerFactory<String, CommentCreatedEvent> commentCreatedConsumerFactory() {
        return getConfigUtil().consumerFactory(CommentCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CommentCreatedEvent> commentCreatedContainerFactory() {
        return getConfigUtil().containerFactory(CommentCreatedEvent.class, ContainerProperties.AckMode.MANUAL_IMMEDIATE, 3);
    }

    // ---------- comment-removed-event ----------

    @Bean
    public ConsumerFactory<String, CommentRemovedEvent> commentRemovedConsumerFactory() {
        return getConfigUtil().consumerFactory(CommentRemovedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CommentRemovedEvent> commentRemovedContainerFactory() {
        return getConfigUtil().containerFactory(CommentRemovedEvent.class, ContainerProperties.AckMode.MANUAL_IMMEDIATE, 3);
    }

    // ---------- post-created-event ----------

    @Bean
    public ConsumerFactory<String, PostCreatedEvent> postCreatedConsumerFactory() {
        return getConfigUtil().consumerFactory(PostCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostCreatedEvent> postCreatedContainerFactory() {
        return getConfigUtil().containerFactory(PostCreatedEvent.class, ContainerProperties.AckMode.MANUAL_IMMEDIATE, 3);
    }

    // ---------- post-removed-event ----------

    @Bean
    public ConsumerFactory<String, PostRemovedEvent> postRemovedConsumerFactory() {
        return getConfigUtil().consumerFactory(PostRemovedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostRemovedEvent> postRemovedContainerFactory() {
        return getConfigUtil().containerFactory(PostRemovedEvent.class, ContainerProperties.AckMode.MANUAL_IMMEDIATE, 3);
    }

    // ---------- like-sent-event ----------

    @Bean
    public ConsumerFactory<String, LikeSentEvent> likeSentConsumerFactory() {
        return getConfigUtil().consumerFactory(LikeSentEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LikeSentEvent> likeSentContainerFactory() {
        return getConfigUtil().containerFactory(LikeSentEvent.class, ContainerProperties.AckMode.MANUAL_IMMEDIATE, 3);
    }

    // ---------- like-removed-event ----------

    @Bean
    public ConsumerFactory<String, LikeRemovedEvent> likeRemovedConsumerFactory() {
        return getConfigUtil().consumerFactory(LikeRemovedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LikeRemovedEvent> likeRemovedContainerFactory() {
        return getConfigUtil().containerFactory(LikeRemovedEvent.class, ContainerProperties.AckMode.MANUAL_IMMEDIATE, 3);
    }

    // ---------- subscription-created-event ----------

    @Bean
    public ConsumerFactory<String, SubscriptionCreatedEvent> subscriptionCreatedConsumerFactory() {
        return getConfigUtil().consumerFactory(SubscriptionCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SubscriptionCreatedEvent> subscriptionCreatedContainerFactory() {
        return getConfigUtil().containerFactory(SubscriptionCreatedEvent.class, ContainerProperties.AckMode.MANUAL_IMMEDIATE, 3);
    }

    // ---------- subscription-removed-event ----------

    @Bean
    public ConsumerFactory<String, SubscriptionRemovedEvent> subscriptionRemovedConsumerFactory() {
        return getConfigUtil().consumerFactory(SubscriptionRemovedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SubscriptionRemovedEvent> subscriptionRemovedContainerFactory() {
        return getConfigUtil().containerFactory(SubscriptionRemovedEvent.class, ContainerProperties.AckMode.MANUAL_IMMEDIATE, 3);
    }
}
