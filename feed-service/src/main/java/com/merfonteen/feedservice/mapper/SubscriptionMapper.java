package com.merfonteen.feedservice.mapper;

import com.merfonteen.feedservice.dto.SubscriptionDto;
import com.merfonteen.feedservice.model.Subscription;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "Spring")
public interface SubscriptionMapper {
    SubscriptionDto toDto(Subscription subscription);
    Subscription toEntity(SubscriptionDto subscriptionDto);
    List<SubscriptionDto> toDtos(List<Subscription> subscriptions);
}
