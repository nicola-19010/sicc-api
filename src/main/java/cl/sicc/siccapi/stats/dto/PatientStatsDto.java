package cl.sicc.siccapi.stats.dto;

import java.util.List;

public record PatientStatsDto(
    List<AgeGroupCountDto> byAge,
    List<GenderCountDto> bySex,
    List<FonasaCountDto> byFonasa,
    List<SectorCountDto> bySector
) {}
