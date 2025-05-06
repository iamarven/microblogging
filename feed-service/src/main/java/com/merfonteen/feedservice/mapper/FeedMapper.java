package com.merfonteen.feedservice.mapper;

import com.merfonteen.feedservice.dto.FeedDto;
import com.merfonteen.feedservice.model.Feed;
import org.mapstruct.Mapper;

@Mapper(componentModel = "Spring")
public interface FeedMapper {
    FeedDto toDto(Feed Feed);
}
