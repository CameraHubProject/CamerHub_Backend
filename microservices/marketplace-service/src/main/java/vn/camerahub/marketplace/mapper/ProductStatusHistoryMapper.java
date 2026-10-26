package vn.camerahub.marketplace.mapper;

import vn.camerahub.marketplace.entity.ProductStatusHistory;
import vn.camerahub.marketplace.dto.response.ProductStatusHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductStatusHistoryMapper {
    ProductStatusHistoryResponse toResponse(ProductStatusHistory entity);
}
