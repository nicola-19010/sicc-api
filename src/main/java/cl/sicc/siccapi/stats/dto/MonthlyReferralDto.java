package cl.sicc.siccapi.stats.dto;

public record MonthlyReferralDto(
        String month,
        Long count) {
}
