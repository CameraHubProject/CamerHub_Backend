package vn.camerahub.marketplace.mapper;

import vn.camerahub.marketplace.entity.CameraModel;
import vn.camerahub.marketplace.dto.response.CameraModelResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CameraModelMapper {
    CameraModelResponse toResponse(CameraModel entity);
}
