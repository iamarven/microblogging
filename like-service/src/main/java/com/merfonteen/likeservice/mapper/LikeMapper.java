package com.merfonteen.likeservice.mapper;

import com.merfonteen.likeservice.dto.LikeDto;
import com.merfonteen.likeservice.model.Like;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "Spring")
public interface LikeMapper {
    LikeDto toDto(Like like);
    List<LikeDto> toDtos(List<Like> likes);
}
