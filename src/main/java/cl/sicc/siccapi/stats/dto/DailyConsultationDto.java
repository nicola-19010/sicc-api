package cl.sicc.siccapi.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class DailyConsultationDto {
    private LocalDate date;
    private int totalConsultations;
    private int morningConsultations;
    private int afternoonConsultations;
    private int emergencyConsultations;
}
