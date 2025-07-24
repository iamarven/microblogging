package com.merfonteen.postservice.repository;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.spring.api.DBRider;
import com.merfonteen.postservice.abstractContainers.AbstractPostgresIntegrationTest;
import com.merfonteen.postservice.model.Post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static com.merfonteen.postservice.repository.PostRepositoryIT.TestResources.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DBRider
@DBUnit(schema = "post_service", caseSensitiveTableNames = true, alwaysCleanAfter = true, alwaysCleanBefore = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class PostRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    @DataSet(value = "datasets/posts-by-author-id.yml")
    void shouldFindAllByAuthorId() {
        Page<Post> result = postRepository.findAllByAuthorId(AUTHOR_ID, buildPageable());
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).containsExactlyInAnyOrderElementsOf(buildExpectedPosts());
    }

    @Test
    @DataSet(value = "datasets/posts-by-author-id.yml")
    void shouldCountByAuthorId() {
        long result = postRepository.countByAuthorId(AUTHOR_ID);
        assertThat(result).isEqualTo(2);
    }

    static class TestResources {
        static final Long AUTHOR_ID = 1L;
        static final Integer PAGE = 0;
        static final Integer SIZE = 10;
        static final String CONTENT_AUTHOR_1 = "Content of post by author 1";
        static final String CONTENT_AUTHOR_2 = "Another Post by Author 1";
        static final Instant FIRST_CREATED_DATE = Instant.parse("2025-01-01T10:00:00Z");
        static final Instant SECOND_CREATED_DATE = Instant.parse("2025-01-02T10:00:00Z");

        static Pageable buildPageable() {
            return Pageable.ofSize(SIZE).withPage(PAGE);
        }

        static List<Post> buildExpectedPosts() {
            return List.of(
                    Post.builder()
                            .id(1L)
                            .authorId(AUTHOR_ID)
                            .content(CONTENT_AUTHOR_1)
                            .createdAt(FIRST_CREATED_DATE)
                            .build(),
                    Post.builder()
                            .id(2L)
                            .authorId(AUTHOR_ID)
                            .content(CONTENT_AUTHOR_2)
                            .createdAt(SECOND_CREATED_DATE)
                            .build()
            );
        }
    }
}
