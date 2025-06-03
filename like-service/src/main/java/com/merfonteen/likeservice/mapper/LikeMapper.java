package com.merfonteen.likeservice.mapper;

import com.merfonteen.likeservice.dto.LikeDto;
import com.merfonteen.likeservice.model.Like;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "Spring")
public interface LikeMapper {

    @Mapping(source = "id", target = "likeId")
    LikeDto toDto(Like like);

    @Mapping(source = "likeId", target = "id")
    Like toEntity(LikeDto likeDto);

    List<LikeDto> toDtos(List<Like> likes);
}
