package cl.sicc.siccapi.user.mapper;

import cl.sicc.siccapi.user.domain.User;
import cl.sicc.siccapi.user.dto.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User user);

    @Mapping(target = "password", ignore = true)
    User toEntity(UserDTO dto);
}

