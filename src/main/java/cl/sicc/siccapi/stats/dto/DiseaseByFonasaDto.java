package cl.sicc.siccapi.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DiseaseByFonasaDto {
    private String fonasa;
    private String disease;
    private Long count;
}
