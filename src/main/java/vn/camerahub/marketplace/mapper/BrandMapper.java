package vn.camerahub.marketplace.mapper;

import vn.camerahub.marketplace.entity.Brand;
import vn.camerahub.marketplace.dto.response.BrandResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BrandMapper {
    BrandResponse toResponse(Brand entity);
}
