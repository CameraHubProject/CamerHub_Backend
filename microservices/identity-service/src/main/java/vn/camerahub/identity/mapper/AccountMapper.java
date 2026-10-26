package vn.camerahub.identity.mapper;

import vn.camerahub.identity.entity.Account;
import vn.camerahub.identity.dto.response.AccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountMapper {
    AccountResponse toResponse(Account entity);
}
