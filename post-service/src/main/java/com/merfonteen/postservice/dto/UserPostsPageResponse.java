package com.merfonteen.postservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserPostsPageResponse implements Serializable {
    private List<PostResponse> posts;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private boolean isLastPage;
}
