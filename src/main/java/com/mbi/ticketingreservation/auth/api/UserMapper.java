package com.mbi.ticketingreservation.auth.api;

import com.mbi.ticketingreservation.auth.domain.User;
import com.mbi.ticketingreservation.common.mapping.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapperConfig.class)
public interface UserMapper {

    @Mapping(target = "email", source = "request.email")
    @Mapping(target = "passwordHash", source = "passwordHash")
    @Mapping(target = "roles", expression = "java(java.util.Set.of(com.mbi.ticketingreservation.auth.domain.Role.CUSTOMER))")
    User toCustomer(RegisterRequest request, String passwordHash);

    UserResponse toResponse(User user);
}
