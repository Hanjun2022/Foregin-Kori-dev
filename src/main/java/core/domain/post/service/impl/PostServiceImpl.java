package core.domain.post.service.impl;

import core.domain.board.dto.BoardItem;
import core.domain.board.entity.Board;
import core.domain.board.repository.BoardRepository;
import core.domain.notification.dto.NotificationEvent;
import core.domain.post.dto.*;
import core.domain.post.entity.BlockPost;
import core.domain.post.entity.Post;
import core.domain.post.event.PostCreatedEvent;
import core.domain.post.event.PostUpdatedEvent;
import core.domain.post.repository.BlockPostRepository;
import core.domain.post.repository.PostRepository;
import core.domain.post.service.PostService;
import core.domain.user.entity.BlockUser;
import core.domain.user.entity.Follow;
import core.domain.user.entity.User;
import core.domain.user.repository.BlockRepository;
import core.domain.user.repository.FollowRepository;
import core.domain.user.repository.UserRepository;
import core.global.enums.*;
import core.global.exception.BusinessException;
import core.global.image.repository.ImageRepository;
import core.global.image.service.ImageService;
import core.global.like.entity.Like;
import core.global.like.repository.LikeRepository;
import core.global.pagination.CursorCodec;
import core.global.pagination.CursorPageResponse;
import core.global.pagination.CursorPages;
import core.global.service.ForbiddenWordService;
import core.global.service.GoogleService;
import core.global.service.TranslationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final ForbiddenWordService forbiddenWordService;
    private final ImageService imageService;
    private final BlockRepository blockRepository;
    private final BlockPostRepository blockPostRepository;
    private final TranslationService translationService;

    private final FollowRepository followRepository;
    private final ApplicationEventPublisher eventPublisher;
    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<BoardItem> getPostList(Long boardId, SortOption sort, String cursor, int size) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        final Long resolvedBoardId = (boardId != null && boardId == 1L) ? null : boardId;

        if (resolvedBoardId != null && !boardRepository.existsById(resolvedBoardId)) {
            throw new BusinessException(ErrorCode.BOARD_NOT_FOUND);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        final int pageSize = Math.min(Math.max(size, 1), 50);
        final Map<String, Object> c = safeDecode(cursor);

        return switch (sort) {
            case POPULAR -> handlePopular(user.getId(), resolvedBoardId, c, pageSize);
            case LATEST -> handleLatest(user.getId(), resolvedBoardId, c, pageSize);
            default -> handleLatest(user.getId(), resolvedBoardId, c, pageSize);
        };
    }


    // ------- 정렬 핸들러 -------
    private CursorPageResponse<BoardItem> handleLatest(Long userId, Long boardId, Map<String, Object> c, int pageSize) {
        var k = parseLatest(c); // t,id
        List<BoardItem> rows = postRepository.findLatestPosts(
                userId,
                boardId,
                truncateToMillis(k.t),
                k.id,
                pageSize + 1,
                null
        );

        if (rows == null || rows.isEmpty()) {
            return new CursorPageResponse<>(List.of(), false, null);
        }

        return CursorPages.ofLatest(
                rows, pageSize,
                BoardItem::createdAt,
                BoardItem::postId
        );
    }

    private CursorPageResponse<BoardItem> handlePopular(Long userId, Long boardId, Map<String, Object> c, int pageSize) {
        var k = parsePopular(c);
        Instant since = popularSince();
        List<BoardItem> rows = postRepository.findPopularPosts(
                userId,
                boardId,
                since,
                k.sc,
                k.id,
                pageSize + 1,
                null
        );


        if (rows == null || rows.isEmpty()) {
            return new CursorPageResponse<>(List.of(), false, null);
        }

        return CursorPages.ofPopular(
                rows, pageSize,
                BoardItem::score,
                BoardItem::postId
        );
    }


    // ------- 커서 파싱 -------
    private LatestKey parseLatest(Map<String, Object> c) {
        Instant t = null;
        Long id = null;
        Object ts = c.get("t");
        if (ts instanceof String s && !s.isBlank()) t = Instant.parse(s);
        Object idObj = c.get("id");
        if (idObj instanceof Number n) id = n.longValue();
        return new LatestKey(t, id);
    }

    private PopularKey parsePopular(Map<String, Object> c) {
        Long sc = null, id = null;
        Object scObj = c.get("sc");
        if (scObj instanceof Number n) sc = n.longValue();
        Object idObj = c.get("id");
        if (idObj instanceof Number n) id = n.longValue();
        return new PopularKey(sc, id);
    }

    private Map<String, Object> safeDecode(String cursor) {
        if (cursor == null || cursor.isBlank()) return Map.of();
        try {
            return CursorCodec.decode(cursor);
        } catch (IllegalArgumentException e) {
            return Map.of();
        }
    }

    private Instant popularSince() {
        return Instant.now().minus(Duration.ofDays(10));
    }

    // ------- 유틸 -------
    private Instant truncateToMillis(Instant i) {
        return (i == null) ? null : i.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    }

    @Override
    @Transactional
    public PostDetailResponse getPostDetail(Long postId, Boolean translate) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if(blockRepository.existsBlockedByEmail(email, post.getAuthor().getEmail()) || blockRepository.existsBlockedByEmail(post.getAuthor().getEmail(), email)) {
            throw new BusinessException(ErrorCode.BLOCKED_USER_POST);
        }

        postRepository.increaseViewCount(postId);

        if (translate) {
            PostDetailResponse postDetail = postRepository.findPostDetail(email, postId);

            String translatedContent = translationService.translatePost(postDetail.content(), user.getTranslateLanguage());
            return new PostDetailResponse(postDetail, translatedContent);
        } else {
            return postRepository.findPostDetail(email, postId);
        }
    }

    @Override
    @Transactional
    public void writePost(@Positive Long boardId, PostWriteRequest request) {
        if (boardId == 1) {
            throw new BusinessException(ErrorCode.NOT_AVAILABLE_WRITE);
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        validateAnonymousPolicy(board.getCategory(), request.isAnonymous());

        validatePostForbiddenWord(request.content());

        final Post post = getPost(email, request, board);

        imageService.saveOrUpdatePostImages(post.getId(), request.imageUrls(), null);
        publishFollowerNotification(post);
    }

    /**
     *  [새로 추가된 private 헬퍼 메소드]
     * 게시글 작성자의 팔로워들에게 알림을 발행합니다.
     * @param post 새로 작성되고 저장된 게시글 엔티티
     */
    private void publishFollowerNotification(Post post) {
        User author = post.getAuthor();
        List<Follow> follows = followRepository.findAllByFollowingAndStatus(author, FollowStatus.ACCEPTED);

        for (Follow follow : follows) {
            User recipient = follow.getUser();

            if (recipient.getId().equals(author.getId())) {
                continue;
            }

            NotificationEvent event = new NotificationEvent(
                    recipient.getId(),
                    author.getId(),
                    NotificationType.followuserpost,
                    post.getId(),
                    null,
                    null
            );
            eventPublisher.publishEvent(event);
        }
    }
    @Override
    @Transactional
    public void writePostForChat(Long roomId, PostWriteForChatRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Board board = boardRepository.findByCategory(BoardCategory.ACTIVITY)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        validateChatRoomPolicy(board.getCategory(), request.link());

        validatePostForbiddenWord(request.content());

        final Post post = getPost(email, request, board);

        imageService.saveOrUpdatePostImages(post.getId(), request.imageUrls(), null);
    }

    private void validatePostForbiddenWord(String content) {
        if (forbiddenWordService.containsForbiddenWord(content)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_WORD_DETECTED);
        }
    }

    private void validateChatRoomPolicy(BoardCategory category, String link) {
        final boolean allowAnonymous =
                category == BoardCategory.FREE_TALK || category == BoardCategory.QNA;

        if (!allowAnonymous && !link.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_AVAILABLE_LINK);
        }

    }

    private void validateAnonymousPolicy(BoardCategory category, Boolean isAnonymous) {
        final boolean allowAnonymous =
                category == BoardCategory.FREE_TALK || category == BoardCategory.QNA;

        if (!allowAnonymous && isAnonymous) {
            throw new BusinessException(ErrorCode.NOT_AVAILABLE_ANONYMOUS);
        }
    }

    private Post getPost(String email, PostWriteRequest request, Board board) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }
        final Post post = new Post(request, user, board);
        eventPublisher.publishEvent(new PostCreatedEvent(post.getId(), post.getContent()));

        return postRepository.save(post);
    }

    private Post getPost(String email, PostWriteForChatRequest request, Board board) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }
        final Post post = new Post(request, user, board);
        eventPublisher.publishEvent(new PostCreatedEvent(post.getId(), post.getContent()));

        return postRepository.save(post);
    }

    @Override
    @Transactional
    public void updatePost(Long postId, @Valid PostUpdateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!email.equals(post.getAuthor().getEmail())) {
            throw new BusinessException(ErrorCode.POST_EDIT_FORBIDDEN);
        }

        if (request.content() != null && !request.content().equals(post.getContent())) {
            post.changeContent(request.content());
        }

        imageService.saveOrUpdatePostImages(post.getId(), request.images(), request.removedImages());
        eventPublisher.publishEvent(new PostUpdatedEvent(post.getId(), post.getContent()));

    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (post.getAuthor() == null || !post.getAuthor().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.POST_DELETE_FORBIDDEN);
        }

        String folderPrefix = "posts/" + postId;
        try {
            imageService.deleteFolder(folderPrefix);
        } catch (BusinessException ex) {
            log.warn("S3 폴더 삭제 실패(prefix={}): {}", folderPrefix, ex.getMessage());
        }

        imageRepository.deleteByImageTypeAndRelatedId(ImageType.POST, postId);

        postRepository.delete(post);
    }

    @Override
    @Transactional
    public void addLike(Long postId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        Optional<Like> existedLike = likeRepository.findLikeByUserEmailAndType(email, postId, LikeType.POST);
        if (existedLike.isPresent()) {
            throw new BusinessException(ErrorCode.LIKE_ALREADY_EXIST);
        }

        likeRepository.save(Like.builder()
                .user(user)
                .type(LikeType.POST)
                .relatedId(postId)
                .build());
    }

    @Override
    @Transactional
    public void removeLike(Long postId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        likeRepository.deleteByUserEmailAndIdAndType(email, postId, LikeType.POST);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<UserPostItem> getMyPostList(String cursor, int size) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        final int pageSize = Math.min(Math.max(size, 1), 50);

        Instant cursorCreatedAt = null;
        Long cursorId = null;
        var payload = CursorCodec.decode(cursor);
        if (payload.get("t") instanceof String ts && !ts.isBlank()) cursorCreatedAt = Instant.parse(ts);
        if (payload.get("id") instanceof Number n) cursorId = n.longValue();

        List<UserPostItem> rows = (cursorId == null || cursorCreatedAt == null)
                ? postRepository.findMyPostsFirstByEmail(email, pageSize + 1)
                : postRepository.findMyPostsNextByEmail(email,
                cursorCreatedAt.truncatedTo(ChronoUnit.MILLIS),
                cursorId,
                pageSize + 1);

        boolean hasNext = rows.size() > pageSize;
        if (hasNext) rows = rows.subList(0, pageSize);

        if (rows.isEmpty()) {
            return new CursorPageResponse<>(List.of(), false, null);
        }

        UserPostItem last = rows.get(rows.size() - 1);
        String nextCursor = hasNext
                ? CursorCodec.encode(Map.of(
                "t", last.createdAt().toString(),
                "id", last.postId()
        ))
                : null;

        return new CursorPageResponse<>(rows, hasNext, nextCursor);
    }

    @Override
    public CommentWriteAnonymousAvailableResponse isAnonymousAvaliable(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        return new CommentWriteAnonymousAvailableResponse(post.getAnonymous());
    }

    @Override
    @Transactional
    public void blockUser(Long postId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        User blockedUser = postRepository.findUserByPostId(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (blockedUser.getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK);
        }

        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (blockRepository.existsBlock(me.getId(), blockedUser.getId()) || blockRepository.existsBlock(blockedUser.getId(), me.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK);
        }

        blockRepository.save(new BlockUser(me, blockedUser));
    }

    @Override
    @Transactional
    public void blockPost(Long postId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (post.getAuthor().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK);
        }

        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (blockPostRepository.existsBlock(me.getId(), post.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK);
        }

        blockPostRepository.save(new BlockPost(me, post));
    }


    private static final class LatestKey {
        final Instant t;
        final Long id;

        LatestKey(Instant t, Long id) {
            this.t = t;
            this.id = id;
        }
    }

    private static final class PopularKey {
        final Long sc;
        final Long id;

        PopularKey(Long sc, Long id) {
            this.sc = sc;
            this.id = id;
        }
    }
}
