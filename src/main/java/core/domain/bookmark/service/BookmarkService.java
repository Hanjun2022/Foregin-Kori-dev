package core.domain.bookmark.service;

import core.domain.bookmark.dto.BookmarkItem;
import core.global.pagination.CursorPageResponse;

public interface BookmarkService {
    void addBookmark(Long postId);

    void removeBookmark(Long postId);

    CursorPageResponse<BookmarkItem> getMyBookmarks(int size, String cursor);

}
