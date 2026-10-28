package vn.camerahub.ai.mapper;

import vn.camerahub.ai.entity.AdvisorUsage;
import vn.camerahub.ai.dto.response.AdvisorUsageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AdvisorUsageMapper {
    AdvisorUsageResponse toResponse(AdvisorUsage entity);
}
