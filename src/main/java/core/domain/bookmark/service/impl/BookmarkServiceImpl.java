package core.domain.bookmark.service.impl;

import core.domain.bookmark.dto.BookmarkItem;
import core.domain.bookmark.entity.Bookmark;
import core.domain.bookmark.repository.BookmarkRepository;
import core.domain.bookmark.service.BookmarkService;
import core.domain.comment.repository.CommentRepository;
import core.domain.post.entity.Post;
import core.domain.post.repository.PostRepository;
import core.domain.user.entity.User;
import core.domain.user.repository.UserRepository;
import core.global.enums.ErrorCode;
import core.global.enums.ImageType;
import core.global.enums.LikeType;
import core.global.exception.BusinessException;
import core.global.image.repository.ImageRepository;
import core.global.like.repository.LikeRepository;
import core.global.pagination.CursorCodec;
import core.global.pagination.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ImageRepository imageRepository;

    private static final ImageType IMAGE_TYPE_USER = ImageType.USER;
    private static final ImageType IMAGE_TYPE_POST = ImageType.POST;
    private static final LikeType LIKE_TYPE_POST   = LikeType.POST;

    @Transactional(readOnly = true)
    @Override
    public CursorPageResponse<BookmarkItem> getMyBookmarks( int size, String  cursor) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }


        Pageable pageable = PageRequest.of(0, size + 1);

        Long cursorId = null;
        var payload = CursorCodec.decode(cursor);
        Object idObj = payload.get("id");
        if (idObj instanceof Number n) cursorId = n.longValue();


        Slice<Bookmark> slice = (cursorId == null)
                ? bookmarkRepository.findByUserEmailOrderByIdDesc(email, pageable)
                : bookmarkRepository.findByUserEmailAndIdLessThanOrderByIdDesc(email, cursorId, pageable);

        List<Bookmark> content = slice.getContent();
        boolean hasNext = content.size() > size;
        if (hasNext) {
            content = content.subList(0, size);
        }

        if (content.isEmpty()) {
            return new CursorPageResponse<>(List.of(), false, null);
        }


        List<Post> posts = content.stream().map(Bookmark::getPost).toList();
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        List<Long> authorIds = posts.stream()
                .map(p -> p.getAuthor() != null ? p.getAuthor().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Long> likeMap = likeRepository.countByRelatedIds(LIKE_TYPE_POST, postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        Map<Long, Long> commentMap = commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        Map<Long, String> userImageMap = authorIds.isEmpty() ? Map.of()
                : imageRepository.findFirstUrlByRelatedIds(IMAGE_TYPE_USER, authorIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (String) row[1]));

        Map<Long, List<String>> postImagesMap = imageRepository.findAllUrlsByRelatedIds(IMAGE_TYPE_POST, postIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())
                ));

        Set<Long> myLikedPostIds = postIds.isEmpty()
                ? Set.of()
                : new HashSet<>(likeRepository.findMyLikedRelatedIdsByEmail(
                email, LIKE_TYPE_POST, postIds
        ));

        List<BookmarkItem> items = new ArrayList<>(content.size());
        for (Bookmark b : content) {
            items.add(toResponse(b, likeMap, commentMap, userImageMap, postImagesMap, myLikedPostIds));
        }

        Long lastId = content.get(content.size() - 1).getId();
        String nextCursor = hasNext ? CursorCodec.encodeId(lastId) : null;

        return new CursorPageResponse<>(items, hasNext, nextCursor);
    }

    private BookmarkItem toResponse(
            Bookmark b,
            Map<Long, Long> likeMap,
            Map<Long, Long> commentMap,
            Map<Long, String> userImageMap,
            Map<Long, List<String>> postImagesMap,
            Set<Long> myLikedPostIds
    ) {
        Post p = b.getPost();

        String authorName = Boolean.TRUE.equals(p.getAnonymous())
                ? "Anonymity"
                : (p.getAuthor() != null ? p.getAuthor().getName() : null);

        Long postId = p.getId();
        Long likeCount    = likeMap.getOrDefault(postId, 0L);
        Long commentCount = commentMap.getOrDefault(postId, 0L);
        Long checkCount   = p.getCheckCount();

        String userImage = (p.getAuthor() == null) ? null
                : userImageMap.get(p.getAuthor().getId());

        List<String> postImages = postImagesMap.getOrDefault(postId, List.of());

        boolean isLiked = myLikedPostIds.contains(postId);

        return new BookmarkItem(
                b.getId(),
                postId,
                authorName,
                safeTrim(p.getContent()),
                isLiked,
                likeCount,
                commentCount,
                checkCount,
                true,
                userImage,
                postImages
        );
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        int len = s.codePointCount(0, s.length());
        if (len <= 200) return s;
        int endIndex = s.offsetByCodePoints(0, 200);
        return s.substring(0, endIndex);
    }


    @Override
    @Transactional
    public void addBookmark( Long postId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }


        Optional<Bookmark> bookmark = bookmarkRepository.findByUserEmailAndPostId(email, postId);
        if (bookmark.isPresent()) {
            throw new BusinessException(ErrorCode.BOOKMARK_ALREADY_EXIST);
        }

        Post post = postRepository.findById(postId).
                orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        bookmarkRepository.save(Bookmark.createBookmark(user, post));
    }

    @Override
    @Transactional
    public void removeBookmark( Long postId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }


        bookmarkRepository.deleteByUserEmailAndPostId(email, postId);
    }


}
