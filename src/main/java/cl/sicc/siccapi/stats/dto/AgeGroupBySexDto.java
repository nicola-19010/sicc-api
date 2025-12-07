package cl.sicc.siccapi.stats.dto;

/**
 * DTO para distribución de pacientes por grupo de edad y sexo
 */
public record AgeGroupBySexDto(
        String rangoEdad,
        long hombres,
        long mujeres,
        long total,
        double porcentaje) {
}
