package com.merfonteen.profileservice.dto;

import com.merfonteen.dtos.PublicUserDto;

public record AggregatedProfileDto(PublicUserDto user,
                                   PostPageDto posts,
                                   boolean partial) {
}
