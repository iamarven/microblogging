package com.merfonteen.profileservice.service;

import com.merfonteen.dtos.PublicUserDto;
import com.merfonteen.profileservice.client.UserClient;
import com.merfonteen.profileservice.dto.AggregatedProfileDto;
import com.merfonteen.profileservice.dto.PostPageDto;
import com.merfonteen.profileservice.dto.PostsSearchRequest;
import com.merfonteen.profileservice.dto.ProfileSearchRequest;
import com.merfonteen.profileservice.util.Resilience;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProfileFacade {
    private final UserClient userClient;
    private final Resilience resilience;
    private final CacheService cacheService;
    private final PostQueryService postQueryService;

    public AggregatedProfileDto getAggregatedProfile(Long userId, ProfileSearchRequest searchRequest) {
        PublicUserDto user = null;
        boolean partial = false;

        if (searchRequest.isIncludeBasic()) {
            try {
                user = resilience.userCall(() -> cacheService.getOrLoad(userId, () -> userClient.getUser(userId)));
            } catch (Exception e) {
                partial = true;
                log.error("Error while fetching basic user info, userId='{}', ex='{}'", userId, e.getMessage());
            }
        }

        PostPageDto userPostsWithComments = postQueryService.getUserPosts(userId, new PostsSearchRequest(
                searchRequest.getPostsLimit(), true, searchRequest.getPostsCursor())
        );
        log.info("Received '{}' posts for user='{}, partial='{}'", userPostsWithComments.posts().size(), userId, partial);

        return new AggregatedProfileDto(user, userPostsWithComments, partial);
    }
}
