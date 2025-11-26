package cl.sicc.siccapi.stats.dto;

import java.util.List;

public record ChronicSummaryDto(
    Long totalCronicos,
    Long totalEnTratamiento,
    Double porcentajeCronicos,
    List<PathologyDistributionDto> distribucionPorPatologia
) {
}

