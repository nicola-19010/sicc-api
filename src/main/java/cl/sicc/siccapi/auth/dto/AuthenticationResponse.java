package cl.sicc.siccapi.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta de autenticación.
 *
 * NOTA: Los JWT (access_token y refresh_token) se entregan vía cookies HttpOnly,
 * NO en el body de la respuesta. Este DTO solo expone datos básicos del usuario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String email;
    private String firstname;
    private String lastname;
}

