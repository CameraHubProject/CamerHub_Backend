package vn.camerahub.identity.mapper;

import vn.camerahub.identity.entity.UserAddress;
import vn.camerahub.identity.dto.response.UserAddressResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserAddressMapper {
    UserAddressResponse toResponse(UserAddress entity);
}
