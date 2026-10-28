package vn.camerahub.moderation.mapper;

import vn.camerahub.moderation.entity.ServiceFlag;
import vn.camerahub.moderation.dto.response.ServiceFlagResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ServiceFlagMapper {
    ServiceFlagResponse toResponse(ServiceFlag entity);
}
