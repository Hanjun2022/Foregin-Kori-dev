//package core.domain.bookmark.service.impl;
//
//import core.domain.bookmark.dto.BookmarkItem;
//import core.domain.bookmark.entity.Bookmark;
//import core.domain.bookmark.repository.BookmarkRepository;
//import core.domain.comment.repository.CommentRepository;
//import core.domain.post.entity.Post;
//import core.domain.post.repository.PostRepository;
//import core.domain.user.entity.User;
//import core.domain.user.repository.UserRepository;
//import core.global.enums.ImageType;
//import core.global.enums.LikeType;
//import core.global.exception.BusinessException;
//import core.global.image.repository.ImageRepository;
//import core.global.like.repository.LikeRepository;
//import core.global.pagination.CursorCodec;
//import core.global.pagination.CursorPageResponse;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Slice;
//import org.springframework.data.domain.SliceImpl;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.then;
//
//@ExtendWith(MockitoExtension.class)
//class BookmarkServiceImplTest {
//
//    private static final ImageType IMAGE_TYPE_USER = ImageType.USER;
//    private static final ImageType IMAGE_TYPE_POST = ImageType.POST;
//    private static final LikeType LIKE_TYPE_POST = LikeType.POST;
//
//    private static final String AUTH_EMAIL = "alice@example.com";
//
//    @Mock private BookmarkRepository bookmarkRepository;
//    @Mock private UserRepository userRepository;
//    @Mock private PostRepository postRepository;
//    @Mock private LikeRepository likeRepository;
//    @Mock private CommentRepository commentRepository;
//    @Mock private ImageRepository imageRepository;
//
//    @InjectMocks
//    private BookmarkServiceImpl sut;
//
//    @BeforeEach
//    void setUpSecurityContext() {
//        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
//        given(auth.getName()).willReturn(AUTH_EMAIL);
//        SecurityContext sc = org.mockito.Mockito.mock(SecurityContext.class);
//        given(sc.getAuthentication()).willReturn(auth);
//        SecurityContextHolder.setContext(sc);
//    }
//
//    @AfterEach
//    void clearContext() {
//        SecurityContextHolder.clearContext();
//    }
//
//    // ---------- helpers ----------
//    private User user(Long id, String name, String email) {
//        User u = new User();
//        try {
//            var f1 = User.class.getDeclaredField("id");
//            f1.setAccessible(true);
//            f1.set(u, id);
//        } catch (Exception ignored) {}
//        try {
//            var m = User.class.getMethod("setName", String.class);
//            m.invoke(u, name);
//        } catch (Exception e) {
//            try {
//                var f2 = User.class.getDeclaredField("name");
//                f2.setAccessible(true);
//                f2.set(u, name);
//            } catch (Exception ignored) {}
//        }
//        try {
//            var m2 = User.class.getMethod("setEmail", String.class);
//            m2.invoke(u, email);
//        } catch (Exception e) {
//            try {
//                var f3 = User.class.getDeclaredField("email");
//                f3.setAccessible(true);
//                f3.set(u, email);
//            } catch (Exception ignored) {}
//        }
//        return u;
//    }
//
//    private Post post(Long id, String content, User author, boolean anonymous, Long checkCount) {
//        Post p = new Post();
//        try {
//            var f = Post.class.getDeclaredField("id");
//            f.setAccessible(true);
//            f.set(p, id);
//        } catch (Exception ignored) {}
//        try {
//            var f = Post.class.getDeclaredField("content");
//            f.setAccessible(true);
//            f.set(p, content);
//        } catch (Exception ignored) {}
//        try {
//            var f = Post.class.getDeclaredField("author");
//            f.setAccessible(true);
//            f.set(p, author);
//        } catch (Exception ignored) {}
//        try {
//            var f = Post.class.getDeclaredField("anonymous");
//            f.setAccessible(true);
//            f.set(p, anonymous);
//        } catch (Exception ignored) {}
//        try {
//            var f = Post.class.getDeclaredField("checkCount");
//            f.setAccessible(true);
//            f.set(p, checkCount);
//        } catch (Exception ignored) {}
//        return p;
//    }
//
//    private Bookmark bookmark(Long id, User u, Post p) {
//        Bookmark b = Bookmark.createBookmark(u, p);
//        try {
//            var f = Bookmark.class.getDeclaredField("id");
//            f.setAccessible(true);
//            f.set(b, id);
//        } catch (Exception ignored) {}
//        return b;
//    }
//
//    // ---------- tests ----------
//    @Nested
//    @DisplayName("getMyBookmarks")
//    class GetMyBookmarks {
//
//        @Test
//        @DisplayName("첫 페이지 size+1 → hasNext=true, nextCursor=마지막 id")
//        void firstPageSuccessWithNextCursor() {
//            // given
//            int size = 2;
//
//            User alice = user(10L, "alice", AUTH_EMAIL);
//            User bob = user(20L, "bob", "bob@example.com");
//
//            Post p1 = post(101L, "첫 번째 포스트", bob, false, 5L);
//            Post p2 = post(102L, "두 번째 포스트", bob, false, 0L);
//            Post p3 = post(103L, "세 번째 포스트", bob, false, 2L);
//
//            Bookmark b1 = bookmark(1001L, alice, p1);
//            Bookmark b2 = bookmark(1000L, alice, p2);
//            Bookmark b3 = bookmark(999L,  alice, p3);
//
//            List<Bookmark> pageContent = List.of(b1, b2, b3);
//            Pageable pageable = PageRequest.of(0, size + 1);
//            Slice<Bookmark> slice = new SliceImpl<>(pageContent, pageable, true);
//
//            given(bookmarkRepository.findByUserEmailOrderByIdDesc(eq(AUTH_EMAIL), any(Pageable.class)))
//                    .willReturn(slice);
//
//            // 집계/이미지: 표시 대상은 2개(101, 102)
//            given(likeRepository.countByRelatedIds(eq(LIKE_TYPE_POST), eq(List.of(101L, 102L))))
//                    .willReturn(List.<Object[]>of(new Object[]{101L, 3L}));
//
//            given(commentRepository.countByPostIds(eq(List.of(101L, 102L))))
//                    .willReturn(List.of(new Object[]{101L, 2L}, new Object[]{102L, 1L}));
//
//            given(imageRepository.findAllUrlsByRelatedIds(eq(IMAGE_TYPE_POST), eq(List.of(101L, 102L))))
//                    .willReturn(List.of(
//                            new Object[]{101L, "https://img/post/101-1.png"},
//                            new Object[]{101L, "https://img/post/101-2.png"}
//                    ));
//
//            given(imageRepository.findFirstUrlByRelatedIds(eq(IMAGE_TYPE_USER), eq(List.of(20L))))
//                    .willReturn(List.<Object[]>of(new Object[]{20L, "https://img/user/bob.png"}));
//
//            // when
//            CursorPageResponse<BookmarkItem> res =
//                    sut.getMyBookmarks(size, null);
//
//            // then
//            assertThat(res.items()).hasSize(2);
//            assertThat(res.hasNext()).isTrue();
//            assertThat(res.nextCursor()).isNotNull();
//
//            Map<String, Object> decoded = CursorCodec.decode(res.nextCursor());
//            Object idObj = decoded.get("id");
//            long decodedId = (idObj instanceof Number n) ? n.longValue() : Long.parseLong((String) idObj);
//            // 정렬 id desc → last는 b2(1000L)
//            assertThat(decodedId).isEqualTo(1000L);
//
//            BookmarkItem i0 = res.items().get(0);
//            assertThat(i0.content()).contains("첫 번째 포스트");
//            assertThat(i0.likeCount()).isEqualTo(3L);
//            assertThat(i0.commentCount()).isEqualTo(2L);
//            assertThat(i0.userImage()).isEqualTo("https://img/user/bob.png");
//            assertThat(i0.postImages()).containsExactlyInAnyOrder(
//                    "https://img/post/101-1.png",
//                    "https://img/post/101-2.png"
//            );
//        }
//
//        @Test
//        @DisplayName("결과 비어있으면 빈 페이지 반환")
//        void emptyResult() {
//            int size = 20;
//            Pageable pageable = PageRequest.of(0, size + 1);
//            Slice<Bookmark> emptySlice = new SliceImpl<>(List.of(), pageable, false);
//
//            given(bookmarkRepository.findByUserEmailOrderByIdDesc(eq(AUTH_EMAIL), any(Pageable.class)))
//                    .willReturn(emptySlice);
//
//            CursorPageResponse<BookmarkItem> res =
//                    sut.getMyBookmarks(size, null);
//
//            assertThat(res.items()).isEmpty();
//            assertThat(res.hasNext()).isFalse();
//            assertThat(res.nextCursor()).isNull();
//        }
//
//        @Test
//        @DisplayName("다음 페이지: cursor 이후로 조회")
//        void nextPageByCursor() {
//            int size = 2;
//            long cursorId = 1000L;
//            String cursor = CursorCodec.encodeId(cursorId);
//
//            User alice = user(10L, "alice", AUTH_EMAIL);
//            User bob = user(20L, "bob", "bob@example.com");
//
//            Post p3 = post(103L, "세 번째 포스트", bob, false, 1L);
//            Post p4 = post(104L, "네 번째 포스트", bob, false, 0L);
//
//            Bookmark b3 = bookmark(999L, alice, p3);
//            Bookmark b4 = bookmark(998L, alice, p4);
//
//            List<Bookmark> pageContent = List.of(b3, b4);
//            Pageable pageable = PageRequest.of(0, size + 1);
//            Slice<Bookmark> slice = new SliceImpl<>(pageContent, pageable, false);
//
//            given(bookmarkRepository.findByUserEmailAndIdLessThanOrderByIdDesc(eq(AUTH_EMAIL), eq(cursorId), any(Pageable.class)))
//                    .willReturn(slice);
//
//            given(likeRepository.countByRelatedIds(eq(LIKE_TYPE_POST), eq(List.of(103L, 104L))))
//                    .willReturn(List.<Object[]>of(new Object[]{103L, 1L}));
//            given(commentRepository.countByPostIds(eq(List.of(103L, 104L))))
//                    .willReturn(List.of());
//            given(imageRepository.findFirstUrlByRelatedIds(eq(IMAGE_TYPE_USER), eq(List.of(20L))))
//                    .willReturn(List.<Object[]>of(new Object[]{20L, "https://img/user/bob.png"}));
//            given(imageRepository.findAllUrlsByRelatedIds(eq(IMAGE_TYPE_POST), eq(List.of(103L, 104L))))
//                    .willReturn(List.<Object[]>of(new Object[]{103L, "https://img/post/103-1.png"}));
//
//            CursorPageResponse<BookmarkItem> res =
//                    sut.getMyBookmarks(size, cursor);
//
//            assertThat(res.items()).hasSize(2);
//            assertThat(res.hasNext()).isFalse();
//            assertThat(res.nextCursor()).isNull();
//            assertThat(res.items().get(0).postImages()).containsExactly("https://img/post/103-1.png");
//        }
//    }
//
//    @Nested
//    @DisplayName("addBookmark")
//    class AddBookmark {
//
//        @Test
//        @DisplayName("이미 존재하면 BusinessException(CONFLICT)")
//        void duplicate() {
//            Long postId = 101L;
//            User alice = user(10L, "alice", AUTH_EMAIL);
//            Post post = post(postId, "내용", alice, false, 0L);
//
//            given(bookmarkRepository.findByUserEmailAndPostId(AUTH_EMAIL, postId))
//                    .willReturn(Optional.of(bookmark(1001L, alice, post)));
//
//            assertThatThrownBy(() -> sut.addBookmark(postId))
//                    .isInstanceOf(BusinessException.class)
//                    .extracting("status")
//                    .isEqualTo(HttpStatus.CONFLICT);
//        }
//
//        @Test
//        @DisplayName("사용자 미존재 → NOT_FOUND")
//        void userNotFound() {
//            Long postId = 101L;
//
//            given(bookmarkRepository.findByUserEmailAndPostId(AUTH_EMAIL, postId))
//                    .willReturn(Optional.empty());
//            given(userRepository.findByEmail(AUTH_EMAIL))
//                    .willReturn(Optional.empty());
//
//            assertThatThrownBy(() -> sut.addBookmark(postId))
//                    .isInstanceOf(BusinessException.class)
//                    .extracting("status")
//                    .isEqualTo(HttpStatus.NOT_FOUND);
//        }
//
//        @Test
//        @DisplayName("게시물 미존재 → NOT_FOUND")
//        void postNotFound() {
//            Long postId = 999L;
//            User alice = user(10L, "alice", AUTH_EMAIL);
//
//            given(bookmarkRepository.findByUserEmailAndPostId(AUTH_EMAIL, postId))
//                    .willReturn(Optional.empty());
//            given(userRepository.findByEmail(AUTH_EMAIL))
//                    .willReturn(Optional.of(alice));
//            given(postRepository.findById(postId))
//                    .willReturn(Optional.empty());
//
//            assertThatThrownBy(() -> sut.addBookmark(postId))
//                    .isInstanceOf(BusinessException.class)
//                    .extracting("status")
//                    .isEqualTo(HttpStatus.NOT_FOUND);
//        }
//
//        @Test
//        @DisplayName("정상 추가")
//        void ok() {
//            Long postId = 101L;
//            User alice = user(10L, "alice", AUTH_EMAIL);
//            Post post = post(postId, "내용", alice, false, 0L);
//
//            given(bookmarkRepository.findByUserEmailAndPostId(AUTH_EMAIL, postId)).willReturn(Optional.empty());
//            given(userRepository.findByEmail(AUTH_EMAIL)).willReturn(Optional.of(alice));
//            given(postRepository.findById(postId)).willReturn(Optional.of(post));
//
//            sut.addBookmark(postId);
//
//            then(bookmarkRepository).should().save(any(Bookmark.class));
//        }
//    }
//
//    @Nested
//    @DisplayName("removeBookmark")
//    class RemoveBookmark {
//
//        @Test
//        @DisplayName("멱등 삭제 동작")
//        void ok() {
//            Long postId = 101L;
//
//            sut.removeBookmark(postId);
//
//            then(bookmarkRepository).should().deleteByUserEmailAndPostId(AUTH_EMAIL, postId);
//        }
//    }
//}
