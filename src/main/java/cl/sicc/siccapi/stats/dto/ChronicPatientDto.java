package cl.sicc.siccapi.stats.dto;

import java.time.LocalDate;
import java.util.List;

public record ChronicPatientDto(
    String nombre,
    List<String> patologias,
    LocalDate ultimaConsulta
) {
}

