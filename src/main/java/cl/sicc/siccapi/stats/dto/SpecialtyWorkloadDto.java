package cl.sicc.siccapi.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SpecialtyWorkloadDto {
    private String specialty;
    private int totalProfessionals;
    private int totalConsultations;
    private double averagePerProfessional;
    private int capacityUtilization;
    private String status; // "Normal", "Alta demanda", "Sobrecargado"
}
