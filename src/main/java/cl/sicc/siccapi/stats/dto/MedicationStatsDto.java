package cl.sicc.siccapi.stats.dto;

import java.util.List;

public record MedicationStatsDto(
    List<MedicationCountDto> topMedications,
    List<MonthlyCountDto> monthlyPrescriptions
) {}
