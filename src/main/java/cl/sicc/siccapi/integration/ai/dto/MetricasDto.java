package cl.sicc.siccapi.integration.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricasDto {
    private String recetasMes;
    private String pacientesAtendidos;
    private String consultasPromedio;
}
