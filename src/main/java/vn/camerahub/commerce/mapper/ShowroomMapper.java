package vn.camerahub.commerce.mapper;

import vn.camerahub.commerce.entity.Showroom;
import vn.camerahub.commerce.dto.response.ShowroomResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ShowroomMapper {
    ShowroomResponse toResponse(Showroom entity);
}
