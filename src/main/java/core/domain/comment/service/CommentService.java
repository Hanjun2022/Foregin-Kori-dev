package core.domain.comment.service;

import core.domain.comment.dto.*;
import core.domain.comment.dto.UserCommentItem;
import core.global.enums.SortOption;
import core.global.pagination.CursorPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

public interface CommentService {
    CursorPageResponse<CommentItem> getCommentList(Long postId, Integer size, SortOption sort, String cursor, Boolean translate);

    void writeComment(Long postId, CommentWriteRequest request);

    void updateComment(Long postId, @Valid CommentUpdateRequest request);

    void deleteComment(Long postId);

    CursorPageResponse<UserCommentItem> getMyCommentList(int size, String cursor);

    void addLike(Long commentId);

    void deleteLike(Long commentId);

    void blockUser(@Positive Long commentId);
}
