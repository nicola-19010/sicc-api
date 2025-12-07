package cl.sicc.siccapi.stats.dto;

import java.util.List;

public record ReferralStatsDto(
    Long totalReferrals,
    List<MonthlyReferralDto> monthlyTrend,
    List<SpecialtyReferralDto> bySpecialty
) {}

