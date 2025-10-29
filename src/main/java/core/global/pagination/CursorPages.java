package core.global.pagination;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class CursorPages {
    private CursorPages() {}

    /**
    * 최신 정렬: createdAt desc, id desc (tie-break) 가정
    */
    public static <T> CursorPageResponse<T> ofLatest(
            List<T> rows, int size,
            Function<T, Instant> createdAtGetter,
            Function<T, Long> idGetter
    ) {
        boolean hasNext = rows.size() > size;
        List<T> items = hasNext ? rows.subList(0, size) : rows;

        String nextCursor = null;
        if (!items.isEmpty()) {
            T last = items.get(items.size() - 1); // getLast() 대신 안전하게
            Instant t = createdAtGetter.apply(last);
            Long id = idGetter.apply(last);
            nextCursor = CursorCodec.encodeLatest(t, id);
        }
        return new CursorPageResponse<>(items, hasNext, nextCursor);
    }

    /**
    * 인기 정렬: score desc, id desc (tie-break) 가정
    * */
    public static <T> CursorPageResponse<T> ofPopular(
            List<T> rows, int size,
            Function<T, Long> scoreGetter,
            Function<T, Long> idGetter
    ) {
        boolean hasNext = rows.size() > size;
        List<T> items = hasNext ? rows.subList(0, size) : rows;

        String nextCursor = null;
        if (!items.isEmpty()) {
            T last = items.get(items.size() - 1);
            Long score = scoreGetter.apply(last);
            Long id = idGetter.apply(last);
            nextCursor = CursorCodec.encodePopular(score, id);
        }
        return new CursorPageResponse<>(items, hasNext, nextCursor);
    }

    /**
    * ID 커서(오래된 순/신규 순) 등 단순 케이스
    * */
    public static <T> CursorPageResponse<T> ofIdCursor(
            List<T> rows, int size,
            Function<T, Long> idGetter
    ) {
        boolean hasNext = rows.size() > size;
        List<T> items = hasNext ? rows.subList(0, size) : rows;

        String nextCursor = null;
        if (!items.isEmpty()) {
            T last = items.get(items.size() - 1);
            Long id = idGetter.apply(last);
            nextCursor = CursorCodec.encodeId(id);
        }
        return new CursorPageResponse<>(items, hasNext, nextCursor);
    }

    /**
    * 직접 payload를 구성해 커스터마이즈할 때
    * */
    public static <T> CursorPageResponse<T> ofCustom(
            List<T> rows, int size,
            Function<T, Map<String, Object>> lastItemCursorPayloadBuilder
    ) {
        boolean hasNext = rows.size() > size;
        List<T> items = hasNext ? rows.subList(0, size) : rows;

        String nextCursor = null;
        if (!items.isEmpty()) {
            T last = items.get(items.size() - 1);
            nextCursor = CursorCodec.encode(lastItemCursorPayloadBuilder.apply(last));
        }
        return new CursorPageResponse<>(items, hasNext, nextCursor);
    }
}
