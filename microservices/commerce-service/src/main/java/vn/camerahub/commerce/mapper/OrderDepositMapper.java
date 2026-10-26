package vn.camerahub.commerce.mapper;

import vn.camerahub.commerce.entity.OrderDeposit;
import vn.camerahub.commerce.dto.response.OrderDepositResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderDepositMapper {
    OrderDepositResponse toResponse(OrderDeposit entity);
}
