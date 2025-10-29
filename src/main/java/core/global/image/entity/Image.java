package core.global.image.entity;

import core.global.enums.ImageType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "image",
        indexes = {
                @Index(name = "idx_image_type_related_id", columnList = "image_type, related_id, image_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @Column(name = "image_type", nullable = false)
    private ImageType imageType;

    @Column(name = "related_id", nullable = false)
    private Long relatedId;

    @Column(name = "url", length = 150, nullable = false)
    private String url;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Builder
    private Image(ImageType imageType, Long relatedId, String url, Integer orderIndex) {
        this.imageType = imageType;
        this.relatedId = relatedId;
        this.url = url;
        this.orderIndex = orderIndex;
    }

    public static Image of(ImageType type, Long relatedId, String url, int orderIndex) {
        return Image.builder()
                .imageType(type)
                .relatedId(relatedId)
                .url(url)
                .orderIndex(orderIndex)
                .build();
    }

    public void changePosition(int position) {
        this.orderIndex = position;
    }
}
