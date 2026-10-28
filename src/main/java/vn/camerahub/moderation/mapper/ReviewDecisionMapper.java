package vn.camerahub.moderation.mapper;

import vn.camerahub.moderation.entity.ReviewDecision;
import vn.camerahub.moderation.dto.response.ReviewDecisionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReviewDecisionMapper {
    ReviewDecisionResponse toResponse(ReviewDecision entity);
}
