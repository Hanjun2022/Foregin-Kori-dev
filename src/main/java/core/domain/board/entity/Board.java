package core.domain.board.entity;

import core.global.enums.BoardCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "board",
        uniqueConstraints = @UniqueConstraint(name = "uk_board_category", columnNames = "board_category")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    @Column(name = "board_category", nullable = false)
    private BoardCategory category;

    public Board(BoardCategory category) {
        this.category = category;
    }
}
