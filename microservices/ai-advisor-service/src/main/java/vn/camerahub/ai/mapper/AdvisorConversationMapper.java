package vn.camerahub.ai.mapper;

import vn.camerahub.ai.entity.AdvisorConversation;
import vn.camerahub.ai.dto.response.AdvisorConversationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AdvisorConversationMapper {
    AdvisorConversationResponse toResponse(AdvisorConversation entity);
}
