package vn.camerahub.moderation.mapper;

import vn.camerahub.moderation.entity.ListingIndex;
import vn.camerahub.moderation.dto.response.ListingIndexResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ListingIndexMapper {
    ListingIndexResponse toResponse(ListingIndex entity);
}
