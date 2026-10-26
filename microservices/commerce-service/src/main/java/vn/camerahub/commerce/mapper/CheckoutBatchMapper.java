package vn.camerahub.commerce.mapper;

import vn.camerahub.commerce.entity.CheckoutBatch;
import vn.camerahub.commerce.dto.response.CheckoutBatchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CheckoutBatchMapper {
    CheckoutBatchResponse toResponse(CheckoutBatch entity);
}
