package vn.camerahub.ai.mapper;

import vn.camerahub.ai.entity.AdvisorRateWindow;
import vn.camerahub.ai.dto.response.AdvisorRateWindowResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AdvisorRateWindowMapper {
    AdvisorRateWindowResponse toResponse(AdvisorRateWindow entity);
}
