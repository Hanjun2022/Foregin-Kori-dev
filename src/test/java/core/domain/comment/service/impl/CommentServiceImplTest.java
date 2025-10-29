//package core.domain.comment.service.impl;
//
//import core.global.service.ForbiddenWordService;
//import core.domain.comment.dto.*;
//import core.domain.comment.entity.Comment;
//import core.domain.comment.repository.CommentRepository;
//import core.domain.comment.service.CommentService;
//import core.domain.post.entity.Post;
//import core.domain.post.repository.PostRepository;
//import core.domain.user.entity.User;
//import core.domain.user.repository.UserRepository;
//import core.global.enums.ImageType;
//import core.global.enums.LikeType;
//import core.global.enums.SortOption;
//import core.global.exception.BusinessException;
//import core.global.image.repository.ImageRepository;
//import core.global.like.repository.LikeRepository;
//import core.global.pagination.CursorCodec;
//import core.global.pagination.CursorPageResponse;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.*;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//import java.time.Instant;
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class CommentServiceImplTest {
//
//    private CommentRepository commentRepository;
//    private PostRepository postRepository;
//    private UserRepository userRepository;
//    private ImageRepository imageRepository;
//    private LikeRepository likeRepository;
//    private ForbiddenWordService forbiddenWordService;
//
//    private CommentService service;
//
//    private static final String AUTH_EMAIL = "alice@example.com";
//
//    @BeforeEach
//    void setUp() {
//        commentRepository = mock(CommentRepository.class);
//        postRepository = mock(PostRepository.class);
//        userRepository = mock(UserRepository.class);
//        imageRepository = mock(ImageRepository.class);
//        likeRepository = mock(LikeRepository.class);
//        forbiddenWordService = mock(ForbiddenWordService.class);
//        lenient().when(forbiddenWordService.containsForbiddenWord(any())).thenReturn(false);
//
//        service = new CommentServiceImpl(
//                commentRepository, postRepository, userRepository, imageRepository, likeRepository, forbiddenWordService
//        );
//
//        // SecurityContext: 서비스 내부에서 email을 꺼내 쓰므로 세팅 필요
//        Authentication auth = mock(Authentication.class);
//        given(auth.getName()).willReturn(AUTH_EMAIL);
//        SecurityContext sc = mock(SecurityContext.class);
//        given(sc.getAuthentication()).willReturn(auth);
//        SecurityContextHolder.setContext(sc);
//    }
//
//    @AfterEach
//    void tearDown() {
//        SecurityContextHolder.clearContext();
//    }
//
//    // ---------- Helpers ----------
//    private Comment mockComment(long id, Instant createdAt, User author, Post post, boolean deleted, Comment parent) {
//        Comment c = mock(Comment.class);
//        lenient().when(c.getId()).thenReturn(id);
//        lenient().when(c.getCreatedAt()).thenReturn(createdAt);
//        lenient().when(c.getAuthor()).thenReturn(author);
//        lenient().when(c.getPost()).thenReturn(post);
//        lenient().when(c.isDeleted()).thenReturn(deleted);
//        lenient().when(c.getParent()).thenReturn(parent);
//        return c;
//    }
//
//    private User mockUser(long id, String name, String email) {
//        User u = mock(User.class);
//        lenient().when(u.getId()).thenReturn(id);
//        lenient().when(u.getName()).thenReturn(name);
//        lenient().when(u.getEmail()).thenReturn(email);
//        return u;
//    }
//
//    private Post mockPost(long id) {
//        Post p = mock(Post.class);
//        lenient().when(p.getId()).thenReturn(id);
//        return p;
//    }
//
//    // ============ getCommentList ============
//    @Nested
//    @DisplayName("getCommentList - LATEST")
//    class GetCommentListLatest {
//
//        @Test
//        @DisplayName("초기 로드: 최신순 페이지 반환 및 nextCursor(t,id) 생성")
//        void latest_initial() {
//            Long postId = 10L;
//            User author = mockUser(1L, "alice", AUTH_EMAIL);
//            Post post = mockPost(postId);
//
//            Instant t1 = Instant.parse("2025-08-20T12:00:00Z");
//            Instant t2 = Instant.parse("2025-08-20T12:05:00Z");
//
//            Comment c1 = mockComment(101L, t2, author, post, false, null);
//            Comment c2 = mockComment(102L, t1, author, post, false, null);
//
//            Slice<Comment> slice = new SliceImpl<>(List.of(c1, c2), PageRequest.of(0, 2), false);
//
//            given(commentRepository.findByPostId(eq(postId), any(Pageable.class))).willReturn(slice);
//
//            given(likeRepository.countByRelatedIds(eq(LikeType.COMMENT), eq(List.of(101L, 102L))))
//                    .willReturn(List.<Object[]>of(new Object[]{101L, 3L}));
//
//            given(imageRepository.findUrlByRelatedIds(eq(ImageType.USER), eq(List.of(1L))))
//                    .willReturn(List.<Object[]>of(new Object[]{1L, "u1.png"}));
//
//            CursorPageResponse<CommentItem> resp = service.getCommentList(postId, 2, SortOption.LATEST, null);
//
//            assertThat(resp.items()).hasSize(2);
//            assertThat(resp.hasNext()).isFalse();
//
//            assertThat(resp.nextCursor()).isNotNull();
//            Map<String, Object> decoded = CursorCodec.decode(resp.nextCursor());
//            assertThat(decoded.get("t")).isEqualTo(t1.toString());
//            Object idObj = decoded.get("id");
//            long idVal = (idObj instanceof Number n) ? n.longValue() : Long.parseLong((String) idObj);
//            assertThat(idVal).isEqualTo(102L);
//
//            CommentItem last = resp.items().get(1);
//            assertThat(last.likeCount()).isEqualTo(0L);
//        }
//    }
//
//    @Nested
//    @DisplayName("getCommentList - POPULAR")
//    class GetCommentListPopular {
//        @Test
//        @DisplayName("커서 로드: 인기순 페이지 + nextCursor(lc,t,id) 생성")
//        void popular_with_cursor() {
//            Long postId = 10L;
//            User author = mockUser(1L, "alice", AUTH_EMAIL);
//            Post post = mockPost(postId);
//
//            Instant base = Instant.parse("2025-08-20T12:00:00Z");
//            String cursor = CursorCodec.encode(Map.of("lc", 50L, "t", base.toString(), "id", 999L));
//
//            Comment c1 = mockComment(201L, base.plusSeconds(60), author, post, false, null);
//            Comment c2 = mockComment(202L, base, author, post, false, null);
//
//            Slice<Comment> slice = new SliceImpl<>(List.of(c1, c2), PageRequest.of(0, 2), true);
//
//            given(commentRepository.findPopularByCursor(eq(postId), eq(LikeType.COMMENT),
//                    eq(50L), eq(base), eq(999L), any(Pageable.class)))
//                    .willReturn(slice);
//
//            given(likeRepository.countByRelatedIds(eq(LikeType.COMMENT), eq(List.of(201L, 202L))))
//                    .willReturn(List.of(new Object[]{201L, 70L}, new Object[]{202L, 60L}));
//
//            given(imageRepository.findUrlByRelatedIds(eq(ImageType.USER), eq(List.of(1L))))
//                    .willReturn(List.<Object[]>of(new Object[]{1L, "img.png"}));
//
//            CursorPageResponse<CommentItem> resp = service.getCommentList(postId, 2, SortOption.POPULAR, cursor);
//
//            assertThat(resp.items()).hasSize(2);
//            assertThat(resp.hasNext()).isTrue();
//
//            Map<String, Object> decoded = CursorCodec.decode(resp.nextCursor());
//            long lcVal = (decoded.get("lc") instanceof Number n) ? n.longValue() : Long.parseLong((String) decoded.get("lc"));
//            long idVal = (decoded.get("id") instanceof Number n) ? n.longValue() : Long.parseLong((String) decoded.get("id"));
//            assertThat(lcVal).isEqualTo(60L);
//            assertThat(decoded.get("t")).isEqualTo(base.toString());
//            assertThat(idVal).isEqualTo(202L);
//        }
//    }
//
//    // ============ writeComment ============
//    @Nested
//    @DisplayName("writeComment")
//    class WriteComment {
//
//        @Test
//        @DisplayName("루트 댓글 정상 저장 (컨텍스트 이메일 사용)")
//        void write_root_success() {
//            User u = mockUser(1L, "alice", AUTH_EMAIL);
//            Post p = mockPost(10L);
//
//            given(userRepository.findByEmail(AUTH_EMAIL)).willReturn(Optional.of(u));
//            given(postRepository.findById(10L)).willReturn(Optional.of(p));
//
//            Comment created = mock(Comment.class);
//
//            try (MockedStatic<Comment> mocked = mockStatic(Comment.class)) {
//                mocked.when(() -> Comment.createRootComment(eq(p), eq(u), eq("hi"), eq(true)))
//                        .thenReturn(created);
//
//                service.writeComment(10L, new CommentWriteRequest(null, true, "hi"));
//
//                then(commentRepository).should().save(created);
//            }
//        }
//
//        @Test
//        @DisplayName("대댓글: 부모가 다른 게시물의 댓글이면 INVALID_PARENT_COMMENT")
//        void reply_parent_mismatch() {
//            User u = mockUser(1L, "alice", AUTH_EMAIL);
//            Post targetPost = mockPost(10L);
//            Post otherPost = mockPost(20L);
//            Comment parent = mockComment(99L, Instant.now(), u, otherPost, false, null);
//
//            given(userRepository.findByEmail(AUTH_EMAIL)).willReturn(Optional.of(u));
//            given(postRepository.findById(10L)).willReturn(Optional.of(targetPost));
//            given(commentRepository.findById(99L)).willReturn(Optional.of(parent));
//
//            assertThatThrownBy(() ->
//                    service.writeComment(10L, new CommentWriteRequest(99L, true, "hi"))
//            )
//                    .isInstanceOf(BusinessException.class)
//                    .extracting("status")
//                    .isEqualTo(HttpStatus.BAD_REQUEST);
//        }
//
//        @Test
//        @DisplayName("루트 댓글: Comment.createRootComment가 IllegalArgumentException이면 INVALID_COMMENT_INPUT")
//        void write_invalid_input() {
//            User u = mockUser(1L, "alice", AUTH_EMAIL);
//            Post p = mockPost(10L);
//
//            given(userRepository.findByEmail(AUTH_EMAIL)).willReturn(Optional.of(u));
//            given(postRepository.findById(10L)).willReturn(Optional.of(p));
//
//            try (MockedStatic<Comment> mocked = mockStatic(Comment.class)) {
//                mocked.when(() -> Comment.createRootComment(any(), any(), any(), anyBoolean()))
//                        .thenThrow(new IllegalArgumentException("bad"));
//
//                assertThatThrownBy(() ->
//                        service.writeComment(10L, new CommentWriteRequest(null, true, "bad"))
//                )
//                        .isInstanceOf(BusinessException.class)
//                        .extracting("status")
//                        .isEqualTo(HttpStatus.BAD_REQUEST);
//            }
//        }
//    }
//
//    // ============ updateComment ============
//    @Nested
//    @DisplayName("updateComment")
//    class UpdateComment {
//
//        @Test
//        @DisplayName("작성자 이메일 불일치면 COMMENT_EDIT_FORBIDDEN")
//        void forbidden_when_author_not_matches() {
//            User author = mockUser(1L, "alice", "owner@example.com");
//            Comment c = mock(Comment.class);
//            given(c.getAuthor()).willReturn(author);
//            given(c.isDeleted()).willReturn(false);
//            given(commentRepository.findById(100L)).willReturn(Optional.of(c));
//
//            assertThatThrownBy(() ->
//                    service.updateComment(100L, new CommentUpdateRequest("new"))
//            )
//                    .isInstanceOf(BusinessException.class)
//                    .extracting("status")
//                    .isEqualTo(HttpStatus.FORBIDDEN);
//        }
//
//        @Test
//        @DisplayName("삭제된 댓글이면 COMMENT_ALREADY_DELETED")
//        void already_deleted() {
//            User author = mockUser(1L, "alice", AUTH_EMAIL);
//            Comment c = mock(Comment.class);
//            given(c.getAuthor()).willReturn(author);
//            given(c.isDeleted()).willReturn(true);
//            given(commentRepository.findById(100L)).willReturn(Optional.of(c));
//
//            assertThatThrownBy(() ->
//                    service.updateComment(100L, new CommentUpdateRequest("x"))
//            )
//                    .isInstanceOf(BusinessException.class)
//                    .extracting("status")
//                    .isEqualTo(HttpStatus.GONE);
//        }
//
//        @Test
//        @DisplayName("작성자 본인이며 삭제되지 않았으면 changeContent 호출")
//        void change_content_called() {
//            User author = mockUser(1L, "alice", AUTH_EMAIL);
//            Comment c = mock(Comment.class);
//            given(c.getAuthor()).willReturn(author);
//            given(c.isDeleted()).willReturn(false);
//            given(commentRepository.findById(100L)).willReturn(Optional.of(c));
//
//            service.updateComment(100L, new CommentUpdateRequest("new"));
//
//            then(c).should().changeContent("new");
//        }
//    }
//
//    // ============ deleteComment ============
//    @Nested
//    @DisplayName("deleteComment")
//    class DeleteCommentTests {
//
//        @Test
//        @DisplayName("작성자 이메일 불일치면 COMMENT_DELETE_FORBIDDEN")
//        void delete_forbidden() {
//            User author = mockUser(1L, "alice", "owner@example.com"); // 컨텍스트 이메일과 불일치
//            Comment c = mock(Comment.class);
//            given(c.getAuthor()).willReturn(author);
//            given(commentRepository.findById(300L)).willReturn(Optional.of(c));
//
//            assertThatThrownBy(() -> service.deleteComment(300L))
//                    .isInstanceOf(BusinessException.class)
//                    .extracting("status")
//                    .isEqualTo(HttpStatus.FORBIDDEN);
//        }
//
//        @Test
//        @DisplayName("자식 살아있으면 소프트 삭제(markDeleted)")
//        void soft_delete_when_has_children() {
//            User author = mockUser(1L, "alice", AUTH_EMAIL);
//            Comment c = mock(Comment.class);
//            given(c.getAuthor()).willReturn(author);
//            given(commentRepository.findById(300L)).willReturn(Optional.of(c));
//            given(commentRepository.existsByParentIdAndDeletedFalse(300L)).willReturn(true);
//
//            service.deleteComment(300L);
//
//            then(c).should().markDeleted(AUTH_EMAIL);
//            then(commentRepository).should(never()).delete(any());
//        }
//
//        @Test
//        @DisplayName("자식 없으면 하드 삭제 후 상위 체인 정리(cleanup)")
//        void hard_delete_and_cleanup_chain() {
//            User author = mockUser(1L, "alice", AUTH_EMAIL);
//            Comment parent = mockComment(200L, Instant.now(), author, mockPost(10L), true, null);
//            Comment target = mockComment(300L, Instant.now(), author, mockPost(10L), false, parent);
//
//            given(commentRepository.findById(300L)).willReturn(Optional.of(target));
//            given(commentRepository.existsByParentIdAndDeletedFalse(300L)).willReturn(false);
//            given(commentRepository.countByParentId(200L)).willReturn(0L);
//
//            service.deleteComment(300L);
//
//            then(commentRepository).should().delete(target);
//            then(commentRepository).should().delete(parent);
//        }
//    }
//
//    // ============ getMyCommentList ============
//    @Nested
//    @DisplayName("getMyCommentList")
//    class GetMyCommentListTests {
//
//        @Test
//        @DisplayName("size+1 조회로 hasNext/trim/nextCursor 계산 (컨텍스트 이메일 사용)")
//        void my_comments_cursor_paging() {
//            int size = 2;
//            UserCommentItem i1 = new UserCommentItem(11L, "c1", "hello", Instant.parse("2025-08-20T12:00:00Z"));
//            UserCommentItem i2 = new UserCommentItem(12L, "c2", "hello", Instant.parse("2025-08-20T12:01:00Z"));
//            UserCommentItem i3 = new UserCommentItem(13L, "c3", "hello", Instant.parse("2025-08-20T12:02:00Z"));
//
//            given(commentRepository.findMyCommentsForCursor(eq(AUTH_EMAIL), isNull(), any()))
//                    .willReturn(List.of(i1, i2, i3));
//
//            CursorPageResponse<UserCommentItem> resp = service.getMyCommentList(size, null);
//
//            assertThat(resp.items()).containsExactly(i1, i2);
//            assertThat(resp.hasNext()).isTrue();
//            assertThat(resp.nextCursor()).isNotNull();
//
//            Map<String, Object> decoded = CursorCodec.decode(resp.nextCursor());
//            Object idObj = decoded.get("id");
//            long idVal = (idObj instanceof Number n) ? n.longValue() : Long.parseLong((String) idObj);
//            assertThat(idVal).isEqualTo(12L);
//        }
//    }
//}
