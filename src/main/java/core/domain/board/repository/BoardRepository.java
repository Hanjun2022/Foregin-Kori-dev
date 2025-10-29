package core.domain.board.repository;

import core.domain.board.entity.Board;
import core.global.enums.BoardCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface BoardRepository extends JpaRepository<Board, Long> {

    Boolean existsByCategory(BoardCategory category);

    Optional<Board> findByCategory(BoardCategory boardCategory);
}
