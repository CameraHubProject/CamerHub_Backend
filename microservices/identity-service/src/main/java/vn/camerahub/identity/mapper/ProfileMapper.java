package vn.camerahub.identity.mapper;

import vn.camerahub.identity.entity.Account;
import vn.camerahub.identity.dto.response.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProfileMapper {
    @Mapping(target = "status", source = "accountStatus")
    ProfileResponse toResponse(Account user);

    @Mapping(target = "status", source = "accountStatus")
    @Mapping(target = "joinedAt", source = "createdAt")
    @Mapping(target = "version", source = "profileVersion")
    PublicAccountResponse toPublicResponse(Account user);
}
