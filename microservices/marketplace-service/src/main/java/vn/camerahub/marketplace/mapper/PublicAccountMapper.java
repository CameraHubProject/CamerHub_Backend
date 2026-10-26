package vn.camerahub.marketplace.mapper;

import vn.camerahub.marketplace.entity.PublicAccount;
import vn.camerahub.marketplace.dto.response.PublicAccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PublicAccountMapper {
    PublicAccountResponse toResponse(PublicAccount entity);
}
