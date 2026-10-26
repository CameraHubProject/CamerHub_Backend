package vn.camerahub.identity.mapper;

import vn.camerahub.identity.entity.Role;
import vn.camerahub.identity.dto.response.RoleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleMapper {
    RoleResponse toResponse(Role entity);
}
