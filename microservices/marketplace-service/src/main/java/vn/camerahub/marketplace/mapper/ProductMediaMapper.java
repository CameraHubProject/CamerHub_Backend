package vn.camerahub.marketplace.mapper;

import vn.camerahub.marketplace.entity.ProductMedia;
import vn.camerahub.marketplace.dto.response.ProductMediaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMediaMapper {
    ProductMediaResponse toResponse(ProductMedia entity);
}
