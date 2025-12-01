package cl.sicc.siccapi.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProfessionalWorkloadDto {
    private String professionalName;
    private String specialty;
    private int totalConsultations;
    private double averagePerDay;
    private int patientsToday;
    private int pendingAppointments;
}
