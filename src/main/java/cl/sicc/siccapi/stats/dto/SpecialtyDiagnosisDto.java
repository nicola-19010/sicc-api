package cl.sicc.siccapi.stats.dto;

import java.util.List;

public record SpecialtyDiagnosisDto(
    String specialty,
    List<DiagnosisCountDto> diagnoses
) {}
