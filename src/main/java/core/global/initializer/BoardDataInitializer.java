package core.global.initializer;

import core.domain.board.entity.Board;
import core.domain.board.repository.BoardRepository;
import core.global.enums.BoardCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev","local", "prod"})
@RequiredArgsConstructor
public class BoardDataInitializer implements CommandLineRunner {

    private final BoardRepository boardRepository;

    @Override
    public void run(String... args) {
        for (BoardCategory category : BoardCategory.values()) {
            if (!boardRepository.existsByCategory(category)) {
                boardRepository.save(new Board(category));
            }
        }
    }
}

