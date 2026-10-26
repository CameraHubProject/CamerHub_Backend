package vn.camerahub.marketplace.mapper;

import vn.camerahub.marketplace.entity.Product;
import vn.camerahub.marketplace.dto.response.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMapper {
    ProductResponse toResponse(Product entity);
}
