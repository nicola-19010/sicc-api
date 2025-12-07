package cl.sicc.siccapi.stats.dto;

import java.util.List;
import java.util.Map;

public record PrescriptionsByAgeGroupDto(
    String medication,
    Map<String, Long> byAgeGroup,
    Long total
) {}

