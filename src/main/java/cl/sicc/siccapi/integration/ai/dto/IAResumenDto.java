package cl.sicc.siccapi.integration.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IAResumenDto {
    private String titulo;
    private String resumen;
    private List<IndicadorClaveDto> indicadoresClave;
    private List<String> alertas;
    private List<String> recomendaciones;
    private LocalDateTime fechaGeneracion;
    private MetricasDto metricas;
    private List<MedicamentoTopDto> medicamentosTop;
    private PrediccionesDto predicciones;
}
