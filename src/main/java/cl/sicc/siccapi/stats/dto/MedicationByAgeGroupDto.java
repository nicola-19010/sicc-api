package cl.sicc.siccapi.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MedicationByAgeGroupDto {
    private String ageGroup;
    private String medication;
    private Long count;
}
