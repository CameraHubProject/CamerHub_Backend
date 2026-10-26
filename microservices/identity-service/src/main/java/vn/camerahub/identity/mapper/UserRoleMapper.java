package vn.camerahub.identity.mapper;

import vn.camerahub.identity.entity.UserRole;
import vn.camerahub.identity.dto.response.UserRoleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserRoleMapper {
    UserRoleResponse toResponse(UserRole entity);
}
