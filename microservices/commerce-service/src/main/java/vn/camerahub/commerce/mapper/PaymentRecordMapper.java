package vn.camerahub.commerce.mapper;

import vn.camerahub.commerce.entity.PaymentRecord;
import vn.camerahub.commerce.dto.response.PaymentRecordResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentRecordMapper {
    PaymentRecordResponse toResponse(PaymentRecord entity);
}
