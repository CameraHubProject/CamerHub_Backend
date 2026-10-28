package vn.camerahub.commerce.mapper;

import vn.camerahub.commerce.entity.SellerReview;
import vn.camerahub.commerce.dto.response.SellerReviewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SellerReviewMapper {
    SellerReviewResponse toResponse(SellerReview entity);
}
