package cl.sicc.siccapi.prescription.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PharmaceuticalFormDto {
    private Long id;
    private String name;

    public PharmaceuticalFormDto() {}

}
