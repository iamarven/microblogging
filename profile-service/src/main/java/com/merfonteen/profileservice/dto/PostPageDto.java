package com.merfonteen.profileservice.dto;

import java.io.Serializable;
import java.util.List;

public record PostPageDto(List<PostItemDto> posts, String nextCursor) implements Serializable {
}
