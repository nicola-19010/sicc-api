package cl.sicc.siccapi.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PrescriptionDetailDto {
    private Long id;
    private LocalDate fecha;
    private String paciente;
    private List<String> medicamentos;
    private String profesional;
}
