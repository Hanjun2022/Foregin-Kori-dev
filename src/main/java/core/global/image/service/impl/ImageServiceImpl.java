package core.global.image.service.impl;

import core.global.enums.ErrorCode;
import core.global.enums.ImageType;
import core.global.exception.BusinessException;
import core.global.image.dto.ImageDto;
import core.global.image.dto.PresignedUrlRequest;
import core.global.image.dto.PresignedUrlResponse;
import core.global.image.entity.Image;
import core.global.image.repository.ImageRepository;
import core.global.image.service.ImageService;
import core.global.image.utils.UrlUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static software.amazon.awssdk.services.s3.model.ObjectIdentifier.builder;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final ImageRepository imageRepository;

    @Value("${ncp.s3.bucket}")
    private String bucket;
    @Value("${ncp.s3.endpoint}")
    private String endPoint;

    @Value("${cdn.base-url}")
    private String cdnBaseUrl;

    private static String extOf(String key) {
        int dot = key.lastIndexOf('.');
        String ext = (dot > -1 && dot < key.length() - 1) ? key.substring(dot + 1) : "jpg";
        if (ext.length() > 8) ext = "jpg";
        return ext.toLowerCase();
    }

    /**
     * ✅ Presigned URL 생성 (일괄)
     */
    @Override
    public List<PresignedUrlResponse> generatePresignedUrls(PresignedUrlRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        if (request.files() == null || request.files().isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        if (request.uploadSessionId() == null || request.uploadSessionId().isBlank()) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }


        List<PresignedUrlResponse> out = new ArrayList<>(request.files().size());
        for (PresignedUrlRequest.FileSpec f : request.files()) {
            out.add(generateOne(email, request.imageType(), request.uploadSessionId(), f));
        }
        return out;
    }

    private PresignedUrlResponse generateOne(
            String email,
            ImageType imageType,
            String uploadSessionId,
            PresignedUrlRequest.FileSpec fileSpec
    ) {
        String filename = fileSpec.filename();
        String contentType = (fileSpec.contentType() == null || fileSpec.contentType().isBlank())
                ? "image/jpeg"
                : fileSpec.contentType();

        String key = UrlUtil.buildRawKey(email, imageType, uploadSessionId, filename);

        // 서명에 포함할 메타데이터
        Map<String, String> meta = Map.of(
                "owner", email,
                "session", uploadSessionId,
                "image-type", imageType.name().toLowerCase()
        );

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .metadata(meta)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofMinutes(10))
                .build();

        var presigned = s3Presigner.presignPutObject(presignRequest);

        // 클라이언트가 그대로 써야 하는 헤더
        Map<String, String> clientHeaders = new LinkedHashMap<>();
        clientHeaders.put("Content-Type", contentType);
        clientHeaders.put("x-amz-meta-owner", email);
        clientHeaders.put("x-amz-meta-session", uploadSessionId);
        clientHeaders.put("x-amz-meta-image-type", imageType.name().toLowerCase());

        String publicUrl = UrlUtil.buildPublicUrlFromKey(endPoint, bucket, key);

        return new PresignedUrlResponse(
                key,
                presigned.url().toString(),
                "PUT",
                clientHeaders
        );
    }

    @Transactional
    @Override
    public void saveOrUpdatePostImages(Long postId, List<String> toAdd, List<String> toRemove) {
        final List<String> adds = (toAdd == null) ? List.of() : toAdd;
        final List<String> removes = (toRemove == null) ? List.of() : toRemove;
        if (adds.isEmpty() && removes.isEmpty()) return;

        // 1) DB 삭제 + 삭제 대상 키 수집(사용자 제거)
        List<String> bulkDeleteKeys = new ArrayList<>();
        if (!removes.isEmpty()) {
            List<String> removeKeys = removes.stream()
                    .map(raw -> UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, raw))
                    .toList();
            List<String> removeUrls = removeKeys.stream()
                    .map(k -> UrlUtil.buildCdnUrlFromKey(cdnBaseUrl, k))
                    .toList();
            imageRepository.deleteByImageTypeAndRelatedIdAndUrlIn(ImageType.POST, postId, removeUrls);

            bulkDeleteKeys.addAll(
                    removeKeys.stream().filter(k -> !isDefaultUrlOrKey(k)).toList()
            );

        }

        List<Image> survivors = imageRepository
                .findByImageTypeAndRelatedIdOrderByPositionAsc(ImageType.POST, postId);

        int pos = 0;
        Set<String> survivorUrls = new HashSet<>();
        for (Image img : survivors) {
            img.changePosition(pos++);

            String storedKey = UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, img.getUrl());
            survivorUrls.add(UrlUtil.buildCdnUrlFromKey(cdnBaseUrl, storedKey));
        }

        if (adds.isEmpty()) {
            deleteObjectsBulk(bulkDeleteKeys);
            return;
        }

        final String basePrefix = "posts/" + postId;
        final int startOrder = pos;

        var pool = java.util.concurrent.Executors.newFixedThreadPool(
                Math.min(Math.max(1, adds.size()), 8) // 동시성 8 권장
        );
        var tasks = new ArrayList<java.util.concurrent.Callable<Image>>();
        var stagingToDelete = new java.util.concurrent.ConcurrentLinkedQueue<String>();

        for (int i = 0; i < adds.size(); i++) {
            final int myOrder = startOrder + i;
            final String raw = adds.get(i);
            tasks.add(() -> {
                String srcKey = UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, raw);
                if (isDefaultUrlOrKey(srcKey)) {
                    String finalUrl = UrlUtil.buildCdnUrlFromKey(cdnBaseUrl, srcKey);
                    if (survivorUrls.contains(finalUrl)) return null;
                    return Image.of(ImageType.POST, postId, finalUrl, myOrder);
                }

                String finalKey = ensureFinalKey(basePrefix, myOrder, srcKey);
                String finalUrl = UrlUtil.buildCdnUrlFromKey(cdnBaseUrl, finalKey);
                if (survivorUrls.contains(finalUrl)) return null;

                if (isStagingKey(srcKey) && !srcKey.equals(finalKey) && !isDefaultUrlOrKey(srcKey)) {
                    stagingToDelete.add(srcKey);
                }
                return Image.of(ImageType.POST, postId, finalUrl, myOrder);
            });
        }

        List<Image> toSave = new ArrayList<>();
        try {
            for (var f : pool.invokeAll(tasks)) {
                Image created = f.get();
                if (created != null) toSave.add(created);
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        } finally {
            pool.shutdown();
        }

        if (!toSave.isEmpty()) imageRepository.saveAll(toSave);

        // 4) 한 번에 삭제(사용자 제거 + 스테이징 원본)
        if (!stagingToDelete.isEmpty()) bulkDeleteKeys.addAll(stagingToDelete);
        deleteObjectsBulk(bulkDeleteKeys);
    }


    private boolean isStagingKey(String key) {
        String k = UrlUtil.trimSlashes(key);
        return k.startsWith("temp/");
    }

    private String ensureFinalKey(String basePrefix, int order, String srcKey) {
        String base = basePrefix.endsWith("/") ? basePrefix.substring(0, basePrefix.length() - 1) : basePrefix;
        if (!isStagingKey(srcKey)) return srcKey;

        String basename = srcKey.substring(srcKey.lastIndexOf('/') + 1);
        String dstKey = "%s/%03d_%s".formatted(base, order, basename);
        if (srcKey.equals(dstKey)) return srcKey;

        try {
            s3Client.copyObject(b -> b
                    .sourceBucket(bucket).sourceKey(srcKey)
                    .destinationBucket(bucket).destinationKey(dstKey)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .metadataDirective(MetadataDirective.COPY)
            );
        } catch (S3Exception e) {
            log.warn("[POST IMG] copy failed: src={}, dst={}, status={}, msg={}",
                    srcKey, dstKey, e.statusCode(),
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        } catch (SdkException e) {
            log.warn("[POST IMG] copy failed: src={}, dst={}, err={}", srcKey, dstKey, e.getMessage());
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        return dstKey; // ← 여기서 삭제하지 않음
    }

    private void deleteObjectsBulk(List<String> keys) {
        if (keys == null || keys.isEmpty()) return;

        List<String> filtered = keys.stream()
                .filter(k -> !isDefaultUrlOrKey(k))
                .toList();
        if (filtered.isEmpty()) return;

        final int LIMIT = 1000; // S3/NCP 일반 한도
        for (int i = 0; i < keys.size(); i += LIMIT) {
            List<String> chunk = keys.subList(i, Math.min(i + LIMIT, keys.size()));
            try {
                var res = s3Client.deleteObjects(b -> b.bucket(bucket).delete(d -> d.objects(
                        chunk.stream()
                                .map(k -> builder().key(k).build())
                                .toList()
                )));
                if (res != null && res.errors() != null && !res.errors().isEmpty()) {
                    for (var err : res.errors()) {
                        log.warn("[POST IMG] bulk delete error key={}, code={}, msg={}",
                                err.key(), err.code(), err.message());
                    }
                }
            } catch (SdkException e) {
                log.warn("[POST IMG] bulk delete failed size={}, err={}", chunk.size(), e.getMessage());
            }
        }
    }

    private boolean existsOnS3(String key) {
        try {
            s3Client.headObject(b -> b.bucket(bucket).key(key));
            return true;
        } catch (S3Exception e) {
            return false;
        } catch (SdkException e) {
            log.warn("HEAD fail: key={}, err={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * ✅ 단일 객체 삭제
     */
    @Override
    public void deleteObject(String keyOrUrl) {
        String key = UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, keyOrUrl);

        if (key.startsWith("default/")) {
            return;
        }
        boolean exists = existsOnS3(key);
        if (!exists) {
            throw new BusinessException(ErrorCode.IMAGE_FILE_DELETE_FAILED);
        }

        try {
            s3Client.deleteObject(b -> b.bucket(bucket).key(key));
        } catch (SdkException e) {
            throw new BusinessException(ErrorCode.IMAGE_FILE_DELETE_FAILED);
        }
    }

    /**
     * ✅ 폴더 삭제 (prefix 기준)
     */
    @Override
    public void deleteFolder(String fileLocation) {
        String prefix = UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, fileLocation);
        if (!prefix.endsWith("/")) prefix += "/";

        // prefix 자체가 default면 즉시 스킵
        if (isDefaultUrlOrKey(prefix)) return;

        String continuation = null;
        try {
            do {
                var reqBuilder = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix);
                if (continuation != null) reqBuilder.continuationToken(continuation);
                var res = s3Client.listObjectsV2(reqBuilder.build());

                var toDelete = res.contents().stream()
                        .map(S3Object::key)
                        .filter(k -> !k.endsWith("/"))
                        .filter(k -> !isDefaultUrlOrKey(k))
                        .map(k -> ObjectIdentifier.builder().key(k).build())
                        .collect(Collectors.toList());

                if (!toDelete.isEmpty()) {
                    var delReq = DeleteObjectsRequest.builder()
                            .bucket(bucket)
                            .delete(Delete.builder().objects(toDelete).build())
                            .build();
                    s3Client.deleteObjects(delReq);
                }

                continuation = res.isTruncated() ? res.nextContinuationToken() : null;
            } while (continuation != null);
        } catch (SdkException e) {
            throw new BusinessException(ErrorCode.IMAGE_FOLDER_DELETE_FAILED);
        }
    }

    @Override
    @Transactional
    public String upsertUserProfileImage(Long userId, String requestedKeyOrUrl) {
        // 1) 입력 검증
        if (requestedKeyOrUrl == null || requestedKeyOrUrl.isBlank()) {
            log.warn("[UPI] fail.input_validation reason=null_or_blank userId={}", userId);
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        // 2) URL/Key 판정 및 변환
        boolean isDefaultIncoming = isDefaultUrlOrKey(requestedKeyOrUrl);
        String reqKey = UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, requestedKeyOrUrl);

        // 3) 기본이미지가 아니면 헤더 검사(용량 제한 포함)
        if (!isDefaultIncoming) {
            long maxBytes = 10L * 1024 * 1024;
            validateImageHeadOrThrow(reqKey, maxBytes);
        }

        // 4) 기존 이미지 조회
        Optional<Image> existingOpt =
                imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, userId);

        // 5) 최종 후보 키/URL 계산 (버전드 키 전략)
        boolean reqIsStaging = isStagingKey(reqKey);
        String candidateFinalKey;
        if (!isDefaultIncoming && reqIsStaging) {
            // staging 객체의 ETag로 버전 키 생성
            String ext = extOf(reqKey);
            String etag;
            String contentType = null;
            try {
                var head = s3Client.headObject(b -> b.bucket(bucket).key(reqKey));
                etag = head.eTag();                    // 예: "d41d8cd98f00b204e9800998ecf8427e" 또는 "etag-...-N"
                if (etag != null) {
                    etag = etag.replace("\"", "").replace(":", "_");
                }
                contentType = head.contentType();      // 업로드 시 넣은 content-type
            } catch (SdkException e) {
                log.warn("[UPI] headObject.failed userId={} key={} err={}", userId, reqKey, e.getMessage());
                throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
            if (etag == null || etag.isBlank()) {
                // ETag가 없을 일은 드물지만, 안전망으로 시간스탬프 사용
                etag = String.valueOf(System.currentTimeMillis());
            }
            candidateFinalKey = "users/%d/profile.%s.%s".formatted(userId, etag, ext);
            // 실제 copy는 아래 9)에서 수행
        } else {
            // default이거나 이미 영구키면 그대로 사용
            candidateFinalKey = (!isDefaultIncoming && !reqIsStaging)
                    ? reqKey
                    : (isDefaultIncoming ? reqKey : reqKey);
        }
        String candidateFinalUrl = UrlUtil.buildCdnUrlFromKey(cdnBaseUrl, candidateFinalKey);

        // 6) 동일 URL이면 no-op (버전 키면 보통 달라서 여기 안 걸림)
        if (existingOpt.isPresent() && java.util.Objects.equals(existingOpt.get().getUrl(), candidateFinalUrl)) {
            return candidateFinalUrl;
        }

        // 7) 기존 S3 삭제 (있으면, 그리고 default가 아니면)
        existingOpt.ifPresent(old -> {
            if (!isDefaultUrlOrKey(old.getUrl())) {
                try {
                    String oldKey = UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, old.getUrl());
                    s3Client.deleteObject(b -> b.bucket(bucket).key(oldKey));
                } catch (SdkException e) {
                    // 실패해도 치명적이지 않으므로 경고만
                    log.warn("[UPI] old_s3_delete_ignored userId={} url={} err={}", userId, old.getUrl(), e.getMessage());
                }
            }
        });

        // 8) 기존 DB 삭제
        imageRepository.deleteByImageTypeAndRelatedId(ImageType.USER, userId);

        // 9) staging → 영구(버전드 키) 이동 또는 as-is 사용
        String finalKey = reqKey;
        if (!isDefaultIncoming && reqIsStaging) {
            String dstKey = candidateFinalKey;
            try {
                // 메타데이터는 REPLACE하여 표준화(원치 않으면 COPY로 유지 가능)
                s3Client.copyObject(b -> b
                        .sourceBucket(bucket).sourceKey(reqKey)
                        .destinationBucket(bucket).destinationKey(dstKey)
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .metadataDirective(MetadataDirective.REPLACE)
                        .cacheControl("public, max-age=31536000, immutable")); // 버전 키이므로 aggressive 캐시 OK
                s3Client.deleteObject(b -> b.bucket(bucket).key(reqKey));
                finalKey = dstKey;
            } catch (SdkException e) {
                log.warn("[UPI] staging_move_failed userId={} src={} dst={} err={}", userId, reqKey, dstKey, e.getMessage());
                throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
        } else {
            // default거나 이미 영구키면 그대로 사용
            finalKey = candidateFinalKey;
        }

        // 10) 저장
        String finalUrl = UrlUtil.buildCdnUrlFromKey(cdnBaseUrl, finalKey);
        imageRepository.save(Image.of(ImageType.USER, userId, finalUrl, 0));

        // 11) 종료
        return finalUrl;
    }




    @Override
    @Transactional
    public void deleteUserProfileImage(Long userId) {

        imageRepository.deleteByImageTypeAndRelatedId(ImageType.USER, userId);
        String folder = "users/%d/".formatted(userId);
        try {
            // 같은 클래스 내에 deleteFolder가 있다면 그대로 호출
            deleteFolder(folder);
        } catch (BusinessException e) {
            // 폴더 삭제 실패는 경고만 남기고, 아래 레거시 개별 삭제도 시도
            log.warn("profile folder delete failed (ignored): {}", e.getMessage());
        }
    }

    @Transactional
    @Override
    public String upsertChatRoomProfileImage(Long chatRoomId, String requestedKeyOrUrl) {
        if (requestedKeyOrUrl == null || requestedKeyOrUrl.isBlank()) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        boolean isDefaultIncoming = isDefaultUrlOrKey(requestedKeyOrUrl);
        String reqKey = UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, requestedKeyOrUrl);

        // 존재/타입/용량 검증 (10MB 예시)
        if (!isDefaultIncoming) {
            validateImageHeadOrThrow(reqKey, 10L * 1024 * 1024);
        }

        Optional<Image> existingOpt =
                imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.CHAT_ROOM, chatRoomId);

        // (신규) 후보 finalKey/Url 계산 (staging이면 이동 후 키, 아니면 reqKey 그대로, default는 그대로)
        String candidateFinalKey = (!isDefaultIncoming && isStagingKey(reqKey))
                ? "chatRoom/%d/chat_profile.%s".formatted(chatRoomId, extOf(reqKey))
                : reqKey;
        String candidateFinalUrl = UrlUtil.buildCdnUrlFromKey(cdnBaseUrl, candidateFinalKey);

        if (existingOpt.isPresent() && Objects.equals(existingOpt.get().getUrl(), candidateFinalUrl)) {
            return candidateFinalUrl;
        }

        // 기존 S3 삭제 (있고, default가 아니면)
        existingOpt.ifPresent(old -> {
            if (!isDefaultUrlOrKey(old.getUrl())) {
                try {
                    String oldKey = UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, old.getUrl());
                    s3Client.deleteObject(b -> b.bucket(bucket).key(oldKey));
                } catch (SdkException e) {
                    log.warn("[CHAT_ROOM {}] old S3 delete ignored: {}", chatRoomId, e.getMessage());
                }
            } else {
                log.info("[CHAT_ROOM {}] old image is default - skip S3 delete", chatRoomId);
            }
        });

        imageRepository.deleteByImageTypeAndRelatedId(ImageType.CHAT_ROOM, chatRoomId);

        String finalKey = reqKey;
        if (!isDefaultIncoming && isStagingKey(reqKey)) {
            String dstKey = "chatRoom/%d/chat_profile.%s".formatted(chatRoomId, extOf(reqKey));
            try {
                s3Client.copyObject(b -> b.sourceBucket(bucket).sourceKey(reqKey)
                        .destinationBucket(bucket).destinationKey(dstKey)
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .metadataDirective(MetadataDirective.COPY));
                s3Client.deleteObject(b -> b.bucket(bucket).key(reqKey));
            } catch (SdkException e) {
                throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
            finalKey = dstKey;
        }

        String finalUrl = UrlUtil.buildCdnUrlFromKey(cdnBaseUrl, finalKey);
        imageRepository.save(Image.of(ImageType.CHAT_ROOM, chatRoomId, finalUrl, 0));
        return finalUrl;
    }

    @Transactional
    @Override
    public void deleteChatRoomProfileImage(Long chatRoomId) {

        String folder = "chatRoom/%d/".formatted(chatRoomId);
        try {
            // 같은 클래스 내에 deleteFolder가 있다면 그대로 호출
            deleteFolder(folder);
        } catch (BusinessException e) {
            // 폴더 삭제 실패는 경고만 남기고, 아래 레거시 개별 삭제도 시도
            log.warn("profile folder delete failed (ignored): {}", e.getMessage());
        }

        imageRepository.deleteByImageTypeAndRelatedId(ImageType.CHAT_ROOM, chatRoomId);
    }

    @Override
    public String getUserProfileKey(Long userId) {
        return imageRepository
                .findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, userId)
                .map(Image::getUrl)
                .orElse(null);
    }

    private boolean isDefaultUrlOrKey(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) return false;
        String k = UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, keyOrUrl);
        k = UrlUtil.trimSlashes(k);
        return k.startsWith("default/"); // 예: default/character_03.png
    }

    @Override
    public String normalizeKey(String keyOrUrl) {
        return (keyOrUrl == null || keyOrUrl.isBlank()) ? null
                : UrlUtil.toKeyFromUrlOrKey(endPoint, bucket, cdnBaseUrl, keyOrUrl);
    }

    @Override
    public String toPublicUrl(String keyOrNull) {
        if (keyOrNull == null || keyOrNull.isBlank()) return null;
        return UrlUtil.buildCdnUrlFromKey(cdnBaseUrl, keyOrNull);
    }


    // 내부 검증/확장자 유틸 (이미 클래스에 없다면 추가)
    private void validateImageHeadOrThrow(String key, long maxBytes) {
        HeadObjectResponse head;
        try {
            head = s3Client.headObject(b -> b.bucket(bucket).key(key));
        } catch (SdkException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        long size = head.contentLength();
        if (size <= 0 || size > maxBytes) throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        String ct = Optional.ofNullable(head.contentType()).orElse("").toLowerCase();
        if (!ct.startsWith("image/")) throw new BusinessException(ErrorCode.IMAGE_FILE_UPLOAD_TYPE_ERROR);
    }

    public List<ImageDto> findImagesForChatRooms(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Image> images = imageRepository.findAllByImageTypeAndRelatedIdIn(ImageType.CHAT_ROOM, roomIds);

        return images.stream()
                .map(image -> new ImageDto(image.getId(), image.getRelatedId(), image.getUrl()))
                .collect(Collectors.toList());
    }

}
