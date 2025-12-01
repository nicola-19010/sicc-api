package cl.sicc.siccapi.integration.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndicadorClaveDto {
    private String nombre;
    private String valor;
    private String tendencia; // "positiva", "negativa", "neutral"
}
