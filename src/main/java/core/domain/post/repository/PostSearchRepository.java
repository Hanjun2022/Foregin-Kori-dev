package core.domain.post.repository;

import core.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostSearchRepository extends JpaRepository<Post, Long> {
}
