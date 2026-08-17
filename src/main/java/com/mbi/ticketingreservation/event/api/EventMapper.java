package com.mbi.ticketingreservation.event.api;

import com.mbi.ticketingreservation.common.mapping.BaseMapperConfig;
import com.mbi.ticketingreservation.event.domain.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapperConfig.class)
public interface EventMapper {

    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "venue", source = "request.venue")
    @Mapping(target = "startsAt", source = "request.startsAt")
    @Mapping(target = "endsAt", source = "request.endsAt")
    @Mapping(target = "capacity", source = "request.capacity")
    Event toEntity(CreateEventRequest request, Long ownerId);

    @Mapping(target = "activeReservedSeats", source = "activeReservedSeats")
    EventResponse toResponse(Event event, long activeReservedSeats);
}
