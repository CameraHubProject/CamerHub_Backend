package vn.camerahub.moderation.mapper;

import vn.camerahub.moderation.entity.ReviewJob;
import vn.camerahub.moderation.dto.response.ReviewJobResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReviewJobMapper {
    ReviewJobResponse toResponse(ReviewJob entity);
}
