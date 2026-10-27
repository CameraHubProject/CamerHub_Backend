package vn.camerahub.identity.mapper;

import vn.camerahub.identity.entity.AuditLog;
import vn.camerahub.identity.dto.response.AuditLogResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditLogMapper {
    AuditLogResponse toResponse(AuditLog entity);
}
