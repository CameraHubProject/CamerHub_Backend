package vn.camerahub.commerce.mapper;

import vn.camerahub.commerce.entity.Order;
import vn.camerahub.commerce.dto.response.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderMapper {
    OrderResponse toResponse(Order entity);
}
