package vn.camerahub.identity.mapper;

import vn.camerahub.identity.entity.AuthIdentity;
import vn.camerahub.identity.dto.response.AuthIdentityResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuthIdentityMapper {
    AuthIdentityResponse toResponse(AuthIdentity entity);
}
