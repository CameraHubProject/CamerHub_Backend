package vn.camerahub.marketplace.mapper;

import vn.camerahub.marketplace.entity.ReservationBatch;
import vn.camerahub.marketplace.dto.response.ReservationBatchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReservationBatchMapper {
    ReservationBatchResponse toResponse(ReservationBatch entity);
}
