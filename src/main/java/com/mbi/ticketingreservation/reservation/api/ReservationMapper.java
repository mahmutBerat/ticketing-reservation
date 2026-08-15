package com.mbi.ticketingreservation.reservation.api;

import com.mbi.ticketingreservation.common.mapping.BaseMapperConfig;
import com.mbi.ticketingreservation.reservation.domain.Reservation;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface ReservationMapper {

    ReservationResponse toResponse(Reservation reservation);

}
