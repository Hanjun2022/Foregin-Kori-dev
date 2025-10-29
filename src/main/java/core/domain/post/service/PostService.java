package core.domain.post.service;

import core.domain.board.dto.BoardItem;
import core.domain.chat.dto.ToggleTranslationRequest;
import core.domain.post.dto.*;
import core.domain.post.dto.PostWriteForChatRequest;
import core.global.enums.SortOption;
import core.global.pagination.CursorPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

public interface PostService {

    CursorPageResponse<BoardItem> getPostList(Long boardId, SortOption sort, String cursor, int size);

    PostDetailResponse getPostDetail(Long postId, Boolean translate);

    void addLike(Long boardId);

    void writePost(@Positive Long boardId, PostWriteRequest request);

    void writePostForChat(Long roomId, PostWriteForChatRequest request);

    void updatePost(@Positive Long boardId, @Valid PostUpdateRequest updateRequest);

    void deletePost(@Positive Long postId);

    void removeLike(@Positive Long postId);

    CursorPageResponse<UserPostItem> getMyPostList(String cursor, int size);

    CommentWriteAnonymousAvailableResponse isAnonymousAvaliable(@Positive(message = "postId는 양수여야 합니다.") Long postId);

    void blockUser(@Positive Long postId);

    void blockPost(@Positive Long postId);
}
