package cl.sicc.siccapi.stats.dto;

public record SpecialtyReferralDto(
    String specialty,
    Long count,
    Double percentage
) {}

