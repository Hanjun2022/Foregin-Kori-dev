package core.domain.board.service.impl;

import core.domain.board.dto.CategoryListResponse;
import core.domain.board.entity.Board;
import core.domain.board.repository.BoardRepository;
import core.domain.board.service.BoardService;
import core.domain.post.dto.PostWriteAnonymousAvailableResponse;
import core.global.enums.BoardCategory;
import core.global.enums.ErrorCode;
import core.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    final BoardRepository boardRepository;

    @Override
    public List<CategoryListResponse> getCategories() {

        return Arrays.stream(BoardCategory.values())
                .filter(boardCategory -> boardCategory!=BoardCategory.ALL)
                .map(CategoryListResponse::new)
                .toList();
    }

    @Override
    public PostWriteAnonymousAvailableResponse isAnonymousAvaliable(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        if (board.getCategory() == BoardCategory.FREE_TALK || board.getCategory() == BoardCategory.QNA) {
            return new PostWriteAnonymousAvailableResponse(true);
        } else {
            return new PostWriteAnonymousAvailableResponse(false);
        }
    }
}
