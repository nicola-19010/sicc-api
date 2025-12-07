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
    private LocalDate date;
    private String patientName;
    private List<String> medications;
    private String professionalName;
    private String rut;
    private Integer age;
    private String fonasa;
    private String diagnosis;
    private String type;
}
