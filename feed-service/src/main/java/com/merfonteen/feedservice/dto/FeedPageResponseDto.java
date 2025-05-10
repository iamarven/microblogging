package com.merfonteen.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedPageResponseDto implements Serializable {
    private List<FeedDto> feeds;
    @JsonProperty("current_page")
    private Integer currentPage;
    @JsonProperty("total_pages")
    private Integer totalPages;
    @JsonProperty("total_elements")
    private Long totalElements;
    @JsonProperty("is_last_page")
    private Boolean isLastPage;
}
