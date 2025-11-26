package cl.sicc.siccapi.stats.dto;

import java.util.List;

public record DiagnosisStatsDto(
    List<DiagnosisCountDto> topDiagnoses,
    List<SpecialtyDiagnosisDto> diagnosisBySpecialty,
    List<MonthlyCountDto> respiratoryTrend
) {}
