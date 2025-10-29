//package core.domain.board.service.impl;
//
//import core.domain.board.dto.CategoryListResponse;
//import core.domain.board.entity.Board;
//import core.domain.board.repository.BoardRepository;
//import core.domain.post.dto.PostWriteAnonymousAvailableResponse;
//import core.global.enums.BoardCategory;
//import core.global.exception.BusinessException;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.mock;
//
//@ExtendWith(MockitoExtension.class)
//class BoardServiceImplTest {
//
//    @Mock
//    BoardRepository boardRepository;
//
//    @InjectMocks
//    BoardServiceImpl boardService;
//
//    @Nested
//    @DisplayName("getCategories")
//    class GetCategories {
//
//        @Test
//        @DisplayName("ALL을 제외한 카테고리만 반환한다")
//        void returnsCategoriesExceptALL() {
//            // when
//            List<CategoryListResponse> result = boardService.getCategories();
//
//            assertThat(result)
//                    .extracting(CategoryListResponse::category)
//                    .doesNotContain(BoardCategory.ALL);
//
//            long expectedSize = Arrays.stream(BoardCategory.values())
//                    .filter(c -> c != BoardCategory.ALL)
//                    .count();
//
//            assertThat(result).hasSize((int) expectedSize);
//        }
//    }
//
//    @Nested
//    @DisplayName("isAnonymousAvailable")
//    class IsAnonymousAvailable {
//
//        @Test
//        @DisplayName("FREE_TALK인 경우 true")
//        void freeTalkTrue() {
//            // given
//            long boardId = 1L;
//            Board board = mock(Board.class);
//            given(board.getCategory()).willReturn(BoardCategory.FREE_TALK);
//            given(boardRepository.findById(boardId)).willReturn(Optional.of(board));
//
//            // when
//            PostWriteAnonymousAvailableResponse res = boardService.isAnonymousAvaliable(boardId);
//
//            // then
//            assertThat(res.isAnonymousAvaliable()).isTrue();
//        }
//
//        @Test
//        @DisplayName("QNA인 경우 true")
//        void qnaTrue() {
//            // given
//            long boardId = 2L;
//            Board board = mock(Board.class);
//            given(board.getCategory()).willReturn(BoardCategory.QNA);
//            given(boardRepository.findById(boardId)).willReturn(Optional.of(board));
//
//            // when
//            PostWriteAnonymousAvailableResponse res = boardService.isAnonymousAvaliable(boardId);
//
//            // then
//            assertThat(res.isAnonymousAvaliable()).isTrue();
//        }
//
//        @Test
//        @DisplayName("FREE_TALK, QNA 외의 카테고리는 false")
//        void othersFalse() {
//            // given
//            long boardId = 3L;
//            Optional<BoardCategory> other =
//                    Arrays.stream(BoardCategory.values())
//                            .filter(c -> c != BoardCategory.FREE_TALK && c != BoardCategory.QNA)
//                            .findFirst();
//
//            BoardCategory picked = other.orElse(BoardCategory.ALL);
//
//            Board board = mock(Board.class);
//            given(board.getCategory()).willReturn(picked);
//            given(boardRepository.findById(boardId)).willReturn(Optional.of(board));
//
//            // when
//            PostWriteAnonymousAvailableResponse res = boardService.isAnonymousAvaliable(boardId);
//
//            // then
//            assertThat(res.isAnonymousAvaliable()).isFalse();
//        }
//
//        @Test
//        @DisplayName("board가 존재하지 않으면 BusinessException을 던진다")
//        void throwsWhenNotFound() {
//            // given
//            long boardId = 404L;
//            given(boardRepository.findById(boardId)).willReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() -> boardService.isAnonymousAvaliable(boardId))
//                    .isInstanceOf(BusinessException.class)
//                    .extracting("status")
//                    .isEqualTo(HttpStatus.NOT_FOUND);
//        }
//    }
//}
