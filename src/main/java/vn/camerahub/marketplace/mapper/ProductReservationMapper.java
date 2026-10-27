package vn.camerahub.marketplace.mapper;

import vn.camerahub.marketplace.entity.ProductReservation;
import vn.camerahub.marketplace.dto.response.ProductReservationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductReservationMapper {
    ProductReservationResponse toResponse(ProductReservation entity);
}
