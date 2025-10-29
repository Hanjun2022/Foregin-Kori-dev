//package core.domain.post.service.impl;
//
//import core.domain.board.entity.Board;
//import core.domain.board.repository.BoardRepository;
//import core.domain.post.dto.*;
//import core.domain.post.entity.Post;
//import core.domain.post.repository.PostRepository;
//import core.domain.user.entity.User;
//import core.domain.user.repository.UserRepository;
//import core.global.enums.BoardCategory;
//import core.global.enums.ImageType;
//import core.global.enums.LikeType;
//import core.global.exception.BusinessException;
//import core.global.image.entity.Image;
//import core.global.image.repository.ImageRepository;
//import core.global.like.entity.Like;
//import core.global.like.repository.LikeRepository;
//import core.global.service.ForbiddenWordService;
//import core.global.pagination.CursorCodec;
//import core.global.pagination.CursorPageResponse;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PostServiceImplTest {
//
//    @Mock private PostRepository postRepository;
//    @Mock private BoardRepository boardRepository;
//    @Mock private LikeRepository likeRepository;
//    @Mock private UserRepository userRepository;
//    @Mock private ImageRepository imageRepository;
//    @Mock private ForbiddenWordService forbiddenWordService;
//
//    @InjectMocks
//    private PostServiceImpl service;
//
//    // ─────────────────────────────────────────────────────────────────────────────
//    // SecurityContext 세팅/정리
//    // ─────────────────────────────────────────────────────────────────────────────
//    @BeforeEach
//    void setUpSecurityContext() {
//        Authentication auth = mock(Authentication.class);
//        given(auth.getName()).willReturn("alice");
//        SecurityContext sc = mock(SecurityContext.class);
//        given(sc.getAuthentication()).willReturn(auth);
//        SecurityContextHolder.setContext(sc);
//    }
//
//    @AfterEach
//    void clearSecurityContext() {
//        SecurityContextHolder.clearContext();
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────────
//    // Get PostDetail
//    // ─────────────────────────────────────────────────────────────────────────────
//    @Test
//    @DisplayName("getPostDetail - 조회수 증가(changeCheckCount) 호출 및 상세 조회 위임")
//    void getPostDetail_success() {
//        Long postId = 100L;
//        Post post = mock(Post.class);
//        given(postRepository.findById(postId)).willReturn(Optional.of(post));
//
//        // ★ 현재 사용자 name="alice" → 이름으로 유저 찾고, 이메일 꺼내서 사용
//        User u = mock(User.class);
//        given(userRepository.findByName("alice")).willReturn(Optional.of(u));
//        given(u.getEmail()).willReturn("alice@email.com");
//
//        PostDetailResponse detail = mock(PostDetailResponse.class);
//        given(postRepository.findPostDetail("alice@email.com", postId)).willReturn(detail);
//
//        PostDetailResponse result = service.getPostDetail(postId);
//
//        then(post).should().changeCheckCount();
//        then(postRepository).should().findPostDetail("alice@email.com", postId);
//        assertThat(result).isSameAs(detail);
//    }
//
//    @Test
//    @DisplayName("getPostDetail - 게시글 없음 예외")
//    void getPostDetail_notFound() {
//        given(postRepository.findById(1L)).willReturn(Optional.empty());
//        assertThatThrownBy(() -> service.getPostDetail(1L))
//                .isInstanceOf(BusinessException.class)
//                .extracting("status")
//                .isEqualTo(HttpStatus.NOT_FOUND);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────────
//    // Write Post
//    // ─────────────────────────────────────────────────────────────────────────────
//    @Test
//    @DisplayName("writePost - boardId=1이면 쓰기 불가")
//    void writePost_board1_forbidden() {
//        PostWriteRequest req = mock(PostWriteRequest.class);
//
//        assertThatThrownBy(() -> service.writePost(1L, req))
//                .isInstanceOf(BusinessException.class)
//                .extracting("status")
//                .isEqualTo(HttpStatus.CONFLICT);
//    }
//
//    @Test
//    @DisplayName("writePost - 익명 허용 안되는 게시판에서 isAnonymous=true면 예외")
//    void writePost_anonymous_policy_violation() {
//        Long boardId = 10L;
//        Board board = mock(Board.class);
//        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));
//        given(board.getCategory()).willReturn(BoardCategory.ACTIVITY); // 익명 불가
//
//        PostWriteRequest req = mock(PostWriteRequest.class);
//        given(req.isAnonymous()).willReturn(true);
//
//        assertThatThrownBy(() -> service.writePost(boardId, req))
//                .isInstanceOf(BusinessException.class)
//                .extracting("status")
//                .isEqualTo(HttpStatus.CONFLICT);
//    }
//
//    @Test
//    @DisplayName("writePost - 정상 저장 및 이미지 저장")
//    void writePost_success_withImages() {
//        Long boardId = 10L;
//        Board board = mock(Board.class);
//        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));
//        given(board.getCategory()).willReturn(BoardCategory.FREE_TALK); // 익명 허용
//
//        given(forbiddenWordService.containsForbiddenWord(any())).willReturn(false);
//
//        User user = mock(User.class);
//        given(userRepository.findByName("alice")).willReturn(Optional.of(user));
//
//        PostWriteRequest req = mock(PostWriteRequest.class);
//        given(req.isAnonymous()).willReturn(true);
//        given(req.imageUrls()).willReturn(List.of(" https://a.jpg ", "", "https://b.png"));
//
//        willAnswer(inv -> inv.getArgument(0)).given(postRepository).save(any(Post.class));
//
//        service.writePost(boardId, req);
//
//        then(postRepository).should().save(any(Post.class));
//
//        ArgumentCaptor<List<Image>> captor = ArgumentCaptor.forClass(List.class);
//        then(imageRepository).should().saveAll(captor.capture());
//
//        List<Image> saved = captor.getValue();
//        assertThat(saved).hasSize(2);
//        assertThat(saved.stream().map(Image::getUrl)).containsExactly("https://a.jpg", "https://b.png");
//    }
//
//    @Test
//    @DisplayName("writePost - 사용자 없음 예외")
//    void writePost_userNotFound() {
//        Long boardId = 10L;
//        Board board = mock(Board.class);
//        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));
//        given(board.getCategory()).willReturn(BoardCategory.QNA);
//
//        given(forbiddenWordService.containsForbiddenWord(any())).willReturn(false);
//
//        PostWriteRequest req = mock(PostWriteRequest.class);
//        given(req.isAnonymous()).willReturn(false);
//
//        given(userRepository.findByName("alice")).willReturn(Optional.empty());
//
//        assertThatThrownBy(() -> service.writePost(boardId, req))
//                .isInstanceOf(BusinessException.class)
//                .extracting("status")
//                .isEqualTo(HttpStatus.NOT_FOUND);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────────
//    // Update Post
//    // ─────────────────────────────────────────────────────────────────────────────
//    @Test
//    @DisplayName("updatePost - 작성자와 동일 이름이면 (현재 코드 기준) 수정 금지 예외 발생")
//    void updatePost_forbidden_when_sameAuthorName() {
//        Long postId = 7L;
//        Post post = mock(Post.class);
//        User author = mock(User.class);
//        given(author.getName()).willReturn("alice");
//        given(post.getAuthor()).willReturn(author);
//        given(postRepository.findById(postId)).willReturn(Optional.of(post));
//
//        PostUpdateRequest req = mock(PostUpdateRequest.class);
//
//        assertThatThrownBy(() -> service.updatePost(postId, req))
//                .isInstanceOf(BusinessException.class)
//                .extracting("status")
//                .isEqualTo(HttpStatus.FORBIDDEN);
//    }
//
//    @Test
//    @DisplayName("updatePost - 내용 수정 + 이미지 삭제/추가 정상 동작")
//    void updatePost_success_editContent_and_images() {
//        Long postId = 9L;
//
//        Post post = mock(Post.class);
//        User author = mock(User.class);
//        given(author.getName()).willReturn("bob"); // 호출자는 "alice"
//        given(post.getAuthor()).willReturn(author);
//        given(postRepository.findById(postId)).willReturn(Optional.of(post));
//
//        PostUpdateRequest req = mock(PostUpdateRequest.class);
//        given(req.content()).willReturn("NEW CONTENT");
//        given(req.removedImages()).willReturn(List.of("old1"));
//        given(req.images()).willReturn(List.of("new1", "new2"));
//
//        Image img = mock(Image.class);
//        given(img.getUrl()).willReturn("old1");
//        given(imageRepository.findByImageTypeAndRelatedIdOrderByPositionAsc(ImageType.POST, postId))
//                .willReturn(List.of(img));
//
//        service.updatePost(postId, req);
//
//        then(post).should().changeContent("NEW CONTENT");
//        then(imageRepository).should()
//                .deleteByImageTypeAndRelatedIdAndUrlIn(ImageType.POST, postId, List.of("old1"));
//        then(img).should().changePosition(0);
//        then(imageRepository).should(times(2)).save(any(Image.class));
//    }
//
//    @Test
//    @DisplayName("updatePost - 수정 대상 게시글 없음")
//    void updatePost_postNotFound() {
//        given(postRepository.findById(99L)).willReturn(Optional.empty());
//        PostUpdateRequest req = mock(PostUpdateRequest.class);
//        assertThatThrownBy(() -> service.updatePost(99L, req))
//                .isInstanceOf(BusinessException.class)
//                .extracting("status")
//                .isEqualTo(HttpStatus.NOT_FOUND);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────────
//    // Delete Post
//    // ─────────────────────────────────────────────────────────────────────────────
//    @Test
//    @DisplayName("deletePost - 권한 없음")
//    void deletePost_forbidden() {
//        Long postId = 11L;
//        Post post = mock(Post.class);
//        User author = mock(User.class);
//        given(author.getName()).willReturn("bob");
//        given(post.getAuthor()).willReturn(author);
//        given(postRepository.findById(postId)).willReturn(Optional.of(post));
//
//        assertThatThrownBy(() -> service.deletePost(postId))
//                .isInstanceOf(BusinessException.class)
//                .extracting("status")
//                .isEqualTo(HttpStatus.FORBIDDEN);
//    }
//
//    @Test
//    @DisplayName("deletePost - 이미지가 있으면 이미지 먼저 삭제 후 게시글 삭제")
//    void deletePost_success_withImages() {
//        Long postId = 12L;
//        Post post = mock(Post.class);
//        User author = mock(User.class);
//        given(author.getName()).willReturn("alice");
//        given(post.getAuthor()).willReturn(author);
//        given(postRepository.findById(postId)).willReturn(Optional.of(post));
//
//        Image i1 = mock(Image.class);
//        Image i2 = mock(Image.class);
//        given(i1.getUrl()).willReturn("u1");
//        given(i2.getUrl()).willReturn("u2");
//        given(imageRepository.findByImageTypeAndRelatedIdOrderByPositionAsc(ImageType.POST, postId))
//                .willReturn(List.of(i1, i2));
//
//        service.deletePost(postId);
//
//        then(imageRepository).should()
//                .deleteByImageTypeAndRelatedId(ImageType.POST, postId);
//        then(postRepository).should().delete(post);
//    }
//
//    @Test
//    @DisplayName("deletePost - 이미지가 없으면 바로 게시글 삭제")
//    void deletePost_success_noImages() {
//        Long postId = 13L;
//        Post post = mock(Post.class);
//        User author = mock(User.class);
//        given(author.getName()).willReturn("alice");
//        given(post.getAuthor()).willReturn(author);
//        given(postRepository.findById(postId)).willReturn(Optional.of(post));
//
//        given(imageRepository.findByImageTypeAndRelatedIdOrderByPositionAsc(ImageType.POST, postId))
//                .willReturn(List.of());
//
//        service.deletePost(postId);
//
//        then(imageRepository).should(never())
//                .deleteByImageTypeAndRelatedId(any(), anyLong());
//        then(postRepository).should().delete(post);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────────
//    // Add/Remove Like
//    // ─────────────────────────────────────────────────────────────────────────────
//    @Test
//    @DisplayName("addLike - 이미 좋아요가 있으면 예외")
//    void addLike_alreadyExists() {
//        given(likeRepository.findLikeByUserEmailAndType("alice", 77L, LikeType.POST))
//                .willReturn(Optional.of(mock(Like.class)));
//
//        assertThatThrownBy(() -> service.addLike(77L))
//                .isInstanceOf(BusinessException.class)
//                .extracting("status")
//                .isEqualTo(HttpStatus.CONFLICT);
//    }
//
//    @Test
//    @DisplayName("addLike - 정상 저장")
//    void addLike_success() {
//        given(likeRepository.findLikeByUserEmailAndType("alice", 77L, LikeType.POST))
//                .willReturn(Optional.empty());
//
//        User u = mock(User.class);
//        given(userRepository.findByName("alice")).willReturn(Optional.of(u));
//
//        service.addLike(77L);
//
//        then(likeRepository).should().save(any(Like.class));
//    }
//
//    @Test
//    @DisplayName("removeLike - 정상 삭제")
//    void removeLike_success() {
//        service.removeLike(77L);
//        then(likeRepository).should()
//                .deleteByUserEmailAndIdAndType("alice", 77L, LikeType.POST);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────────
//    // getMyPostList — CursorPageResponse(items, hasNext, nextCursor)
//    // ─────────────────────────────────────────────────────────────────────────────
//    @Test
//    @DisplayName("getMyPostList - 첫 페이지(size+1) → hasNext=true, items trim, nextCursor 생성")
//    void firstPage_hasNext_true_trimmed_withNextCursor() {
//        int size = 3;
//        UserPostItem r1 = mock(UserPostItem.class);
//        UserPostItem r2 = mock(UserPostItem.class);
//        UserPostItem r3 = mock(UserPostItem.class); // 마지막(트림 후 기준)
//        UserPostItem r4 = mock(UserPostItem.class); // size+1
//
//        Instant lastCreated = Instant.now().minusSeconds(10);
//        long lastId = 345L;
//
//        given(r3.createdAt()).willReturn(lastCreated);
//        given(r3.postId()).willReturn(lastId);
//
//        given(postRepository.findMyPostsFirstByEmail("alice", size + 1))
//                .willReturn(List.of(r1, r2, r3, r4));
//
//        CursorPageResponse<UserPostItem> res = service.getMyPostList(null, size);
//
//        assertThat(res.hasNext()).isTrue();
//        assertThat(res.items()).hasSize(size);
//        assertThat(res.nextCursor()).isNotNull();
//
//        Map<String, Object> decoded = CursorCodec.decode(res.nextCursor());
//        assertThat(decoded.get("t")).isEqualTo(lastCreated.toString());
//        assertThat(Long.parseLong((String) decoded.get("id"))).isEqualTo(lastId);
//
//        then(postRepository).should().findMyPostsFirstByEmail("alice", size + 1);
//    }
//
//    @Test
//    @DisplayName("getMyPostList - 첫 페이지(size 이하) → hasNext=false, trim 없음, nextCursor=null")
//    void firstPage_hasNext_false_notTrimmed() {
//        int size = 3;
//        UserPostItem r1 = mock(UserPostItem.class);
//        UserPostItem r2 = mock(UserPostItem.class);
//
//        given(postRepository.findMyPostsFirstByEmail("alice", size + 1))
//                .willReturn(List.of(r1, r2)); // size 이하
//
//        CursorPageResponse<UserPostItem> res = service.getMyPostList("", size);
//
//        assertThat(res.hasNext()).isFalse();
//        assertThat(res.items()).hasSize(2);
//        assertThat(res.nextCursor()).isNull();
//
//        then(postRepository).should().findMyPostsFirstByEmail("alice", size + 1);
//    }
//
//    @Test
//    @DisplayName("getMyPostList - 다음 페이지(size+1) → hasNext=true, items trim, nextCursor 생성")
//    void nextPage_hasNext_true_trimmed_withNextCursor() {
//        int size = 2;
//        Instant cursorCreatedAt = Instant.now();
//        Long cursorId = 123L;
//        String cursor = CursorCodec.encode(Map.of(
//                "t", cursorCreatedAt.toString(),
//                "id", cursorId
//        ));
//
//        UserPostItem r1 = mock(UserPostItem.class);
//        UserPostItem r2 = mock(UserPostItem.class); // 트림 후 마지막
//        UserPostItem r3 = mock(UserPostItem.class); // size+1
//
//        Instant lastCreated = Instant.now().minusSeconds(5);
//        long lastId = 777L;
//
//        given(r2.createdAt()).willReturn(lastCreated);
//        given(r2.postId()).willReturn(lastId);
//
//        given(postRepository.findMyPostsNextByEmail(
//                eq("alice"),
//                eq(cursorCreatedAt.truncatedTo(ChronoUnit.MILLIS)),
//                eq(cursorId),
//                eq(size + 1)))
//                .willReturn(List.of(r1, r2, r3));
//
//        CursorPageResponse<UserPostItem> res = service.getMyPostList(cursor, size);
//
//        assertThat(res.hasNext()).isTrue();
//        assertThat(res.items()).hasSize(size);
//        assertThat(res.nextCursor()).isNotNull();
//
//        Map<String, Object> decoded = CursorCodec.decode(res.nextCursor());
//        assertThat(decoded.get("t")).isEqualTo(lastCreated.toString());
//        assertThat(Long.parseLong((String) decoded.get("id"))).isEqualTo(lastId);
//
//        then(postRepository).should().findMyPostsNextByEmail(
//                "alice", cursorCreatedAt.truncatedTo(ChronoUnit.MILLIS), cursorId, size + 1);
//    }
//
//    @Test
//    @DisplayName("getMyPostList - 다음 페이지(size 이하) → hasNext=false, trim 없음, nextCursor=null")
//    void nextPage_hasNext_false_notTrimmed() {
//        int size = 2;
//        Instant cursorCreatedAt = Instant.now();
//        Long cursorId = 123L;
//        String cursor = CursorCodec.encode(Map.of(
//                "t", cursorCreatedAt.toString(),
//                "id", cursorId
//        ));
//
//        UserPostItem r1 = mock(UserPostItem.class);
//
//        given(postRepository.findMyPostsNextByEmail(
//                eq("alice"),
//                eq(cursorCreatedAt.truncatedTo(ChronoUnit.MILLIS)),
//                eq(cursorId),
//                eq(size + 1)))
//                .willReturn(List.of(r1)); // size 이하
//
//        CursorPageResponse<UserPostItem> res = service.getMyPostList(cursor, size);
//
//        assertThat(res.hasNext()).isFalse();
//        assertThat(res.items()).hasSize(1);
//        assertThat(res.nextCursor()).isNull();
//
//        then(postRepository).should().findMyPostsNextByEmail(
//                "alice", cursorCreatedAt.truncatedTo(ChronoUnit.MILLIS), cursorId, size + 1);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────────
//    // isAnonymousAvailable
//    // ─────────────────────────────────────────────────────────────────────────────
//    @Test
//    @DisplayName("isAnonymousAvailable - 게시글의 익명 작성 가능 여부 반환")
//    void isAnonymousAvailable_success() {
//        Post p = mock(Post.class);
//        given(p.getAnonymous()).willReturn(Boolean.TRUE);
//        given(postRepository.findById(7L)).willReturn(Optional.of(p));
//
//        CommentWriteAnonymousAvailableResponse res = service.isAnonymousAvaliable(7L);
//
//        assertThat(res.isAnonymousAvailable()).isTrue();
//    }
//
//    @Test
//    @DisplayName("isAnonymousAvailable - 게시글 없음 예외")
//    void isAnonymousAvailable_notFound() {
//        given(postRepository.findById(7L)).willReturn(Optional.empty());
//        assertThatThrownBy(() -> service.isAnonymousAvaliable(7L))
//                .isInstanceOf(BusinessException.class)
//                .extracting("status")
//                .isEqualTo(HttpStatus.NOT_FOUND);
//    }
//}
