package cl.sicc.siccapi.integration.ai.controller;

import cl.sicc.siccapi.integration.ai.dto.IAResumenDto;
import cl.sicc.siccapi.integration.ai.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIService aiService;

    @GetMapping("/resumen")
    public ResponseEntity<IAResumenDto> generarResumenIA(
            @RequestParam(required = false) String periodo) {
        log.info("Generando resumen IA para periodo: {}", periodo);
        IAResumenDto resumen = aiService.generarResumenIA(periodo);
        return ResponseEntity.ok(resumen);
    }

    @PostMapping("/cache/invalidate")
    public ResponseEntity<Map<String, String>> invalidarCache() {
        log.info("Invalidando caché de IA");
        aiService.invalidarCache();
        return ResponseEntity.ok(Map.of("mensaje", "Caché invalidado exitosamente"));
    }

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getEstadisticasCache() {
        log.info("Obteniendo estadísticas de caché");
        Map<String, Object> stats = aiService.getEstadisticasCache();
        return ResponseEntity.ok(stats);
    }

    /**
     * E1 - Predicción mensual de número de consultas
     * @param meses número de meses a predecir
     * @return predicción de consultas
     */
    @GetMapping("/predicciones/consultas")
    public ResponseEntity<Map<String, Object>> predecirConsultas(
            @RequestParam(defaultValue = "3") int meses) {
        log.info("Generando predicción de consultas para {} meses", meses);
        Map<String, Object> prediccion = aiService.predecirConsultas(meses);
        return ResponseEntity.ok(prediccion);
    }

    /**
     * E8 - Predicción simple de necesidad de medicamentos más recetados
     * @return predicción de medicamentos
     */
    @GetMapping("/predicciones/medicamentos")
    public ResponseEntity<Map<String, Object>> predecirMedicamentos() {
        log.info("Generando predicción de medicamentos");
        Map<String, Object> prediccion = aiService.predecirMedicamentos();
        return ResponseEntity.ok(prediccion);
    }

    /**
     * E10 - Diagnósticos emergentes detectados automáticamente
     * @return lista de diagnósticos emergentes
     */
    @GetMapping("/diagnosticos/emergentes")
    public ResponseEntity<Map<String, Object>> detectarDiagnosticosEmergentes() {
        log.info("Detectando diagnósticos emergentes");
        Map<String, Object> emergentes = aiService.detectarDiagnosticosEmergentes();
        return ResponseEntity.ok(emergentes);
    }

    /**
     * E4 - Proyección de carga médica por especialidad
     * @return proyección de carga
     */
    @GetMapping("/predicciones/carga")
    public ResponseEntity<Map<String, Object>> proyectarCargaMedica() {
        log.info("Proyectando carga médica por especialidad");
        Map<String, Object> proyeccion = aiService.proyectarCargaMedica();
        return ResponseEntity.ok(proyeccion);
    }

    /**
     * Análisis de tendencias por tipo
     * @param tipo tipo de análisis (consultas, medicamentos, diagnosticos)
     * @return análisis de tendencias
     */
    @GetMapping("/tendencias/{tipo}")
    public ResponseEntity<Map<String, Object>> analizarTendencias(
            @PathVariable String tipo) {
        log.info("Analizando tendencias de: {}", tipo);
        Map<String, Object> tendencias = aiService.analizarTendencias(tipo);
        return ResponseEntity.ok(tendencias);
    }

    /**
     * Detectar anomalías en los datos
     * @return lista de anomalías detectadas
     */
    @GetMapping("/anomalias")
    public ResponseEntity<Map<String, Object>> detectarAnomalias() {
        log.info("Detectando anomalías en los datos");
        Map<String, Object> anomalias = aiService.detectarAnomalias();
        return ResponseEntity.ok(anomalias);
    }

    /**
     * Generar recomendaciones contextuales
     * @param contexto contexto para las recomendaciones
     * @return lista de recomendaciones
     */
    @GetMapping("/recomendaciones")
    public ResponseEntity<Map<String, Object>> generarRecomendaciones(
            @RequestParam(required = false, defaultValue = "general") String contexto) {
        log.info("Generando recomendaciones para contexto: {}", contexto);
        Map<String, Object> recomendaciones = aiService.generarRecomendaciones(contexto);
        return ResponseEntity.ok(recomendaciones);
    }
}
