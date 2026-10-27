package vn.camerahub.commerce.mapper;

import vn.camerahub.commerce.entity.BuybackRequest;
import vn.camerahub.commerce.dto.response.BuybackRequestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BuybackRequestMapper {
    BuybackRequestResponse toResponse(BuybackRequest entity);
}
