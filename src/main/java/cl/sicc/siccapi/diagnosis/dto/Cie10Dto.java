package cl.sicc.siccapi.diagnosis.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Cie10Dto {
    private String code;
    private String name;
}

