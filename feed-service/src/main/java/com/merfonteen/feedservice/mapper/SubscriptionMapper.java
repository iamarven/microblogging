package com.merfonteen.feedservice.mapper;

import com.merfonteen.feedservice.dto.SubscriptionDto;
import com.merfonteen.feedservice.model.Subscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "Spring")
public interface SubscriptionMapper {
    SubscriptionDto toDto(Subscription subscription);
}
