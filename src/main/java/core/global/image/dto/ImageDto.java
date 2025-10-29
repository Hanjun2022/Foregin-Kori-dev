package core.global.image.dto;



public record ImageDto(
        Long imageId,
        Long relatedId,
        String imageUrl
) {}