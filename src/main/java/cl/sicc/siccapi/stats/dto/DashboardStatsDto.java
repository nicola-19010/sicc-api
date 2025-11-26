package cl.sicc.siccapi.stats.dto;

import java.util.Map;

public record DashboardStatsDto(
    long totalConsultations,
    long totalPatients,
    long totalPrescriptions,
    double averageConsultationsPerDay,
    Map<String, Long> consultationsByType,
    Map<String, Long> consultationsByMonth
) {}
