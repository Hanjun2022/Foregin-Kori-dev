package core.domain.listhistory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "list_history")
@Getter
@NoArgsConstructor
public class ListHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "visited_id", nullable = false)
    private Long visitedId; // 무엇을 방문했는지 미상 (확실하지 않음)

    @Column(name = "created_time")
    private Instant createdTime;
}
