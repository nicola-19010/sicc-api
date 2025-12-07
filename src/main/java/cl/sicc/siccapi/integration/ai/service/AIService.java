package cl.sicc.siccapi.integration.ai.service;

import cl.sicc.siccapi.integration.ai.dto.*;
import cl.sicc.siccapi.patient.repository.PatientRepository;
import cl.sicc.siccapi.prescription.repository.PrescriptionRepository;
import cl.sicc.siccapi.stats.service.StatsService;
import cl.sicc.siccapi.stats.dto.*;
import cl.sicc.siccapi.consultation.repository.ConsultationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableCaching
public class AIService {

    private final RestTemplate restTemplate;

    // Repositorios para consultas reales a la base de datos
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ConsultationRepository consultationRepository;

    private final StatsService statsService;

    @Value("${ai.provider:deepseek}")
    private String aiProvider;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${deepseek.api.key:}")
    private String deepseekApiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com}")
    private String deepseekApiUrl;

    @Value("${deepseek.api.model:deepseek-chat}")
    private String deepseekModel;

    /**
     * Genera el periodo actual en formato "mes año" (ej: "diciembre 2025")
     */
    private String obtenerPeriodoActual() {
        java.time.LocalDate now = java.time.LocalDate.now();
        String[] meses = { "enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre" };
        return meses[now.getMonthValue() - 1] + " " + now.getYear();
    }

    // Remover cache manual, usar @Cacheable de Spring con Redis

    @Cacheable(value = "iaResumen", key = "#periodo ?: 'default'")
    public IAResumenDto generarResumenIA(String periodo) {
        String periodoDisplay = periodo != null && !periodo.trim().isEmpty() ? periodo : "Periodo actual";

        log.info("📋 Solicitud de resumen IA para periodo: {}", periodoDisplay);

        // Verificar configuración según el proveedor
        if ("groq".equalsIgnoreCase(aiProvider)) {
            if (groqApiKey == null || groqApiKey.isEmpty() || groqApiKey.equals("your-groq-api-key-here")) {
                throw new RuntimeException(
                        "Groq API key no configurada. Obtén tu API key GRATIS en: https://console.groq.com/");
            }
        } else {
            throw new RuntimeException("Proveedor de IA no válido. Use 'groq'");
        }

        try {
            // Generar resumen con el proveedor configurado
            IAResumenDto resumen;
            if ("groq".equalsIgnoreCase(aiProvider)) {
                log.info("Usando Groq API (GRATUITA) con modelo: {}", groqModel);
                resumen = generarResumenConGroq(periodo);
            } else {
                log.info("Usando DeepSeek API (PAGO)");
                resumen = generarResumenConDeepSeek(periodo);
            }

            log.info("✅ Resumen IA generado exitosamente según requisito E15 (Resumen automatizado NLG)");
            log.info("   📊 Periodo: {} | Proveedor: {} | Modelo: {}",
                    periodoDisplay,
                    aiProvider.toUpperCase(),
                    "groq".equalsIgnoreCase(aiProvider) ? groqModel : "deepseek-chat");
            return resumen;

        } catch (Exception e) {
            log.error("Error generando resumen IA. Verifique la configuración de la API.", e);
            throw new RuntimeException("Error al generar resumen con IA: " + e.getMessage(), e);
        }
    }

    // Metodo para generar resumen usando Groq API (GRATUITA)
    private IAResumenDto generarResumenConGroq(String periodo) {
        try {
            // Recopilar datos actuales del sistema
            Map<String, Object> datosSistema = recopilarDatosSistema();

            // Crear prompt para Groq según requisitos E15
            String prompt = crearPromptResumenMejorado(datosSistema, periodo);

            // Llamar a Groq API
            log.info("Generando resumen NLG según requisito E15 para periodo: {}",
                    periodo != null ? periodo : "actual");
            String respuestaIA = llamarGroqAPI(prompt);

            // Usar la respuesta de IA como resumen principal
            log.info("Respuesta de Groq API recibida, procesando resumen NLG");

            // Crear DTO con respuesta de IA
            IAResumenDto resumen = parsearRespuestaGroqMejorada(respuestaIA, datosSistema, periodo);

            log.info("Resumen IA procesado exitosamente por Groq");

            return resumen;
        } catch (Exception e) {
            log.error("Error generando resumen con Groq API, usando resumen mejorado con datos reales", e);
            return generarResumenConDatosReales(recopilarDatosSistema(), periodo);
        }
    }

    // Crear prompt mejorado según requisitos E15, E1, E2, E6
    private String crearPromptResumenMejorado(Map<String, Object> datos, String periodo) {
        // Generar periodo dinámicamente si no se proporciona
        String periodoTexto = periodo != null ? periodo : obtenerPeriodoActual();

        log.info("🤖 Creando prompt para IA con datos recopilados:");
        log.info("   📊 Consultas totales: {}", datos.get("consultasTotales"));
        log.info("   👥 Pacientes totales: {}", datos.get("pacientesTotales"));
        log.info("   💊 Recetas totales: {}", datos.get("recetasTotales"));
        log.info("   🏥 Pacientes crónicos: {} ({}%)", datos.get("pacientesCronicos"), datos.get("porcentajeCronicos"));

        // Mostrar top medicamentos que se enviarán a la IA
        log.info("   💊 Top medicamentos enviados a IA:");
        for (int i = 0; i < Math.min(3, ((List<?>) datos.get("topMedicamentos")).size()); i++) {
            Map<String, Object> med = (Map<String, Object>) ((List<?>) datos.get("topMedicamentos")).get(i);
            log.info("      {}. {}: {} recetas", i + 1, med.get("nombre"), med.get("recetas"));
        }

        return String.format(
                """
                        Eres un experto en análisis de salud pública para CESFAM (Centro de Salud Familiar).
                        Tu tarea es generar un análisis completo y profesional basado en los datos del sistema.

                        DATOS DEL SISTEMA PARA EL PERIODO %s:
                        - Consultas totales: %d
                        - Pacientes únicos atendidos: %d
                        - Recetas emitidas: %d
                        - Pacientes crónicos: %d (%.1f%% del total)

                        TOP 3 MEDICAMENTOS MÁS RECETADOS (E8):
                        1. %s: %d recetas
                        2. %s: %d recetas
                        3. %s: %d recetas

                        DISTRIBUCIÓN POR EDAD (E3):
                        - 0-17 años: %d pacientes
                        - 18-29 años: %d pacientes
                        - 30-49 años: %d pacientes
                        - 50-64 años: %d pacientes
                        - 65+ años: %d pacientes

                        DISTRIBUCIÓN FONASA (E11):
                        - Tramo A: %d pacientes
                        - Tramo B: %d pacientes
                        - Tramo C: %d pacientes
                        - Tramo D: %d pacientes

                        INSTRUCCIONES PARA GENERAR ANÁLISIS COMPLETO:

                        1. **RESUMEN EJECUTIVO (NLG)**: Escribe un resumen ejecutivo profesional de 2-3 párrafos en español,
                           destacando tendencias, perfil demográfico, uso de medicamentos y situación de crónicos.

                        2. **INDICADORES CLAVE**: Genera 4-5 indicadores clave con tendencias (positiva/negativa/neutral)
                           basados en los datos reales. Cada indicador debe tener nombre, valor y tendencia.

                        3. **ALERTAS**: Identifica 2-3 alertas importantes basadas en análisis de los datos,
                           incluyendo referencias a requisitos (E6, E10, E11, etc.).

                        4. **RECOMENDACIONES**: Proporciona 3-4 recomendaciones estratégicas específicas,
                           con referencias a requisitos (E3, E4, E5, E8, etc.).

                        5. **TOP MEDICAMENTOS**: Lista los 3 medicamentos más recetados con sus cantidades.

                        6. **PREDICCIÓN SEMANAL**: Calcula una predicción realista de consultas para la próxima semana
                           basada en los datos actuales (aproximadamente consultas_mes * 1.05 / 4).

                        FORMATO DE RESPUESTA: Debes responder ÚNICAMENTE con un objeto JSON válido con esta estructura exacta:

                        {
                          "titulo": "Análisis Ejecutivo CESFAM - [Periodo] ([Total Consultas] Consultas)",
                          "resumen": "[Texto del resumen ejecutivo en 2-3 párrafos]",
                          "indicadoresClave": [
                            {"nombre": "Consultas Mensuales", "valor": "[valor formateado]", "tendencia": "neutral|positiva|negativa"},
                            {"nombre": "Pacientes Únicos Atendidos", "valor": "[valor formateado]", "tendencia": "neutral|positiva|negativa"},
                            ...
                          ],
                          "alertas": [
                            "Alerta específica con referencia a requisito",
                            "Otra alerta importante",
                            ...
                          ],
                          "recomendaciones": [
                            "Recomendación estratégica con referencia a requisito",
                            "Otra recomendación específica",
                            ...
                          ],
                          "medicamentosTop": [
                            {"nombre": "Nombre del medicamento", "recetas": numero_entero},
                            {"nombre": "Nombre del medicamento", "recetas": numero_entero},
                            {"nombre": "Nombre del medicamento", "recetas": numero_entero}
                          ],
                          "prediccionSemanal": numero_entero
                        }

                        IMPORTANTE:
                        - Usa formato español profesional médico-administrativo
                        - Incluye referencias a requisitos (E1, E3, E5, E6, E8, E11, etc.)
                        - Los valores deben ser realistas y basados en los datos proporcionados
                        - El JSON debe ser válido y parseable
                        - No incluyas texto adicional fuera del JSON
                        """,
                periodoTexto,
                (Integer) datos.get("consultasTotales"),
                (Integer) datos.get("pacientesTotales"),
                (Integer) datos.get("recetasTotales"),
                (Integer) datos.get("pacientesCronicos"),
                (Double) datos.get("porcentajeCronicos"),
                getTopMedicamento(datos, 0), getTopRecetas(datos, 0),
                getTopMedicamento(datos, 1), getTopRecetas(datos, 1),
                getTopMedicamento(datos, 2), getTopRecetas(datos, 2),
                getPacientesPorEdad(datos, "0-17"),
                getPacientesPorEdad(datos, "18-29"),
                getPacientesPorEdad(datos, "30-49"),
                getPacientesPorEdad(datos, "50-64"),
                getPacientesPorEdad(datos, "65+"),
                getFonasaCount(datos, "A"),
                getFonasaCount(datos, "B"),
                getFonasaCount(datos, "C"),
                getFonasaCount(datos, "D"));
    }

    // Parsear respuesta de Groq y crear DTO estructurado
    @SuppressWarnings("unchecked")
    private IAResumenDto parsearRespuestaGroqMejorada(String respuestaIA, Map<String, Object> datos, String periodo) {
        try {
            // Intentar parsear el JSON completo generado por IA
            if (respuestaIA != null && !respuestaIA.trim().isEmpty()) {
                try {
                    // Limpiar la respuesta para obtener solo el JSON
                    String jsonLimpio = limpiarRespuestaJSON(respuestaIA);

                    // Parsear el JSON usando Jackson
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> jsonResponse = objectMapper.readValue(jsonLimpio, Map.class);

                    log.info("✅ JSON de IA parseado exitosamente");

                    // Extraer componentes del JSON generado por IA
                    String tituloIA = (String) jsonResponse.get("titulo");
                    String resumenIA = (String) jsonResponse.get("resumen");

                    // Parsear indicadores clave
                    List<Map<String, Object>> indicadoresJson = (List<Map<String, Object>>) jsonResponse
                            .get("indicadoresClave");
                    List<IndicadorClaveDto> indicadores = new ArrayList<>();
                    if (indicadoresJson != null) {
                        for (Map<String, Object> ind : indicadoresJson) {
                            indicadores.add(new IndicadorClaveDto(
                                    (String) ind.get("nombre"),
                                    (String) ind.get("valor"),
                                    (String) ind.get("tendencia")));
                        }
                    }

                    // Parsear alertas
                    List<String> alertas = (List<String>) jsonResponse.get("alertas");
                    if (alertas == null)
                        alertas = new ArrayList<>();

                    // Parsear recomendaciones
                    List<String> recomendaciones = (List<String>) jsonResponse.get("recomendaciones");
                    if (recomendaciones == null)
                        recomendaciones = new ArrayList<>();

                    // Parsear medicamentos top
                    List<Map<String, Object>> medicamentosJson = (List<Map<String, Object>>) jsonResponse
                            .get("medicamentosTop");
                    List<MedicamentoTopDto> medicamentosTop = new ArrayList<>();
                    if (medicamentosJson != null) {
                        for (Map<String, Object> med : medicamentosJson) {
                            medicamentosTop.add(new MedicamentoTopDto(
                                    (String) med.get("nombre"),
                                    ((Number) med.get("recetas")).intValue()));
                        }
                    }

                    // Parsear predicción semanal
                    Integer prediccionSemanal = null;
                    Object predObj = jsonResponse.get("prediccionSemanal");
                    if (predObj instanceof Number) {
                        prediccionSemanal = ((Number) predObj).intValue();
                    }

                    // Usar título de IA si está disponible, sino generar uno
                    String tituloFinal = tituloIA != null && !tituloIA.trim().isEmpty()
                            ? tituloIA
                            : generarTitulo(periodo, datos);

                    // Usar resumen de IA si está disponible, sino generar uno
                    String resumenFinal = resumenIA != null && !resumenIA.trim().isEmpty()
                            ? resumenIA
                            : generarResumenTexto(datos, periodo);

                    // Usar indicadores de IA si están disponibles, sino generarlos
                    if (indicadores.isEmpty()) {
                        indicadores = generarIndicadoresClave(datos);
                    }

                    // Usar alertas de IA si están disponibles, sino generarlas
                    if (alertas.isEmpty()) {
                        alertas = generarAlertas(datos);
                    }

                    // Usar recomendaciones de IA si están disponibles, sino generarlas
                    if (recomendaciones.isEmpty()) {
                        recomendaciones = generarRecomendaciones(datos);
                    }

                    // Usar medicamentos de IA si están disponibles, sino usar datos del sistema
                    if (medicamentosTop.isEmpty()) {
                        medicamentosTop = generarTopMedicamentos(datos);
                    }

                    // Usar predicción de IA si está disponible, sino calcularla
                    String prediccionFinal = prediccionSemanal != null
                            ? String.format("%,d", prediccionSemanal)
                            : calcularPrediccionSemanal(datos);

                    // Crear DTO final
                    return new IAResumenDto(
                            tituloFinal,
                            resumenFinal,
                            indicadores,
                            alertas,
                            recomendaciones,
                            LocalDateTime.now(),
                            new MetricasDto(
                                    String.valueOf(datos.get("recetasTotales")),
                                    String.valueOf(datos.get("pacientesTotales")),
                                    "4.8"),
                            medicamentosTop,
                            new PrediccionesDto(prediccionFinal));

                } catch (Exception jsonParseError) {
                    log.warn("Error parseando JSON de IA, usando fallback: {}", jsonParseError.getMessage());
                    // Continuar con el fallback
                }
            }

            // Fallback: generar resumen con datos reales si el JSON no se pudo parsear
            log.info("Usando fallback: resumen generado con datos reales");
            return generarResumenConDatosReales(datos, periodo);

        } catch (Exception e) {
            log.error("Error parseando respuesta de IA, generando resumen con datos reales", e);
            return generarResumenConDatosReales(datos, periodo);
        }
    }

    private IAResumenDto generarResumenConDeepSeek(String periodo) {
        // Recopilar datos actuales del sistema
        Map<String, Object> datosSistema = recopilarDatosSistema();

        // Crear prompt para DeepSeek
        String prompt = crearPromptResumen(datosSistema, periodo);

        // Llamar a DeepSeek API
        String respuestaIA = llamarDeepSeekAPI(prompt);

        // Parsear respuesta y crear DTO
        return parsearRespuestaIA(respuestaIA);
    }

    private String crearPromptResumen(Map<String, Object> datos, String periodo) {
        return String.format("""
                Genera un resumen ejecutivo inteligente del CESFAM para el periodo %s.

                Datos del sistema:
                - Consultas totales: %d
                - Pacientes totales: %d
                - Recetas emitidas: %d
                - Pacientes crónicos: %d (%.1f%%)

                Top 3 medicamentos más recetados:
                1. %s: %d recetas
                2. %s: %d recetas
                3. %s: %d recetas

                Distribución por edad:
                - 0-17 años: %d pacientes
                - 18-29 años: %d pacientes
                - 30-49 años: %d pacientes
                - 50-64 años: %d pacientes
                - 65+ años: %d pacientes

                Distribución FONASA:
                - Tramo A: %d pacientes
                - Tramo B: %d pacientes
                - Tramo C: %d pacientes
                - Tramo D: %d pacientes

                Por favor genera:
                1. Un título apropiado para el análisis
                2. Un resumen ejecutivo conciso del estado del CESFAM
                3. Indicadores clave con tendencias (positiva/negativa/neutral)
                4. Alertas importantes basadas en los datos
                5. Recomendaciones estratégicas
                6. Métricas específicas del mes actual

                Formato: JSON válido con las siguientes claves:
                - titulo: string
                - resumen: string
                - indicadoresClave: array de objetos {nombre, valor, tendencia}
                - alertas: array de strings
                - recomendaciones: array de strings
                - metricas: {recetasMes, pacientesAtendidos, consultasPromedio}
                - medicamentosTop: array de {nombre, recetas}
                - predicciones: {demandaProximaSemana}
                """,
                periodo != null ? periodo : obtenerPeriodoActual(),
                (Integer) datos.get("consultasTotales"),
                (Integer) datos.get("pacientesTotales"),
                (Integer) datos.get("recetasTotales"),
                (Integer) datos.get("pacientesCronicos"),
                (Double) datos.get("porcentajeCronicos"),
                getTopMedicamento(datos, 0),
                getTopRecetas(datos, 0),
                getTopMedicamento(datos, 1),
                getTopRecetas(datos, 1),
                getTopMedicamento(datos, 2), getTopRecetas(datos, 2),
                getPacientesPorEdad(datos, "0-17"),
                getPacientesPorEdad(datos, "18-29"),
                getPacientesPorEdad(datos, "30-49"),
                getPacientesPorEdad(datos, "50-64"),
                getPacientesPorEdad(datos, "65+"),
                getFonasaCount(datos, "A"),
                getFonasaCount(datos, "B"),
                getFonasaCount(datos, "C"),
                getFonasaCount(datos, "D"));
    }

    @SuppressWarnings("unchecked")
    private String llamarDeepSeekAPI(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepseekApiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", "deepseek-chat",
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", prompt)),
                    "temperature", 0.7,
                    "max_tokens", 2000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    deepseekApiUrl + "/chat/completions",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> body = response.getBody();
            if (body != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            throw new RuntimeException("Respuesta inválida de DeepSeek API");

        } catch (Exception e) {
            log.error("Error llamando a DeepSeek API", e);
            throw new RuntimeException("Error en IA service", e);
        }
    }

    @SuppressWarnings("unchecked")
    private IAResumenDto parsearRespuestaIA(String respuesta) {
        try {
            // Intentar parsear el JSON completo generado por IA
            if (respuesta != null && !respuesta.trim().isEmpty()) {
                try {
                    // Limpiar la respuesta para obtener solo el JSON
                    String jsonLimpio = limpiarRespuestaJSON(respuesta);

                    // Parsear el JSON usando Jackson
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> jsonResponse = objectMapper.readValue(jsonLimpio, Map.class);

                    log.info("✅ JSON de DeepSeek parseado exitosamente");

                    // Extraer componentes del JSON generado por IA
                    String tituloIA = (String) jsonResponse.get("titulo");
                    String resumenIA = (String) jsonResponse.get("resumen");

                    // Parsear indicadores clave
                    List<Map<String, Object>> indicadoresJson = (List<Map<String, Object>>) jsonResponse
                            .get("indicadoresClave");
                    List<IndicadorClaveDto> indicadores = new ArrayList<>();
                    if (indicadoresJson != null) {
                        for (Map<String, Object> ind : indicadoresJson) {
                            indicadores.add(new IndicadorClaveDto(
                                    (String) ind.get("nombre"),
                                    (String) ind.get("valor"),
                                    (String) ind.get("tendencia")));
                        }
                    }

                    // Parsear alertas
                    List<String> alertas = (List<String>) jsonResponse.get("alertas");
                    if (alertas == null)
                        alertas = new ArrayList<>();

                    // Parsear recomendaciones
                    List<String> recomendaciones = (List<String>) jsonResponse.get("recomendaciones");
                    if (recomendaciones == null)
                        recomendaciones = new ArrayList<>();

                    // Parsear medicamentos top
                    List<Map<String, Object>> medicamentosJson = (List<Map<String, Object>>) jsonResponse
                            .get("medicamentosTop");
                    List<MedicamentoTopDto> medicamentosTop = new ArrayList<>();
                    if (medicamentosJson != null) {
                        for (Map<String, Object> med : medicamentosJson) {
                            medicamentosTop.add(new MedicamentoTopDto(
                                    (String) med.get("nombre"),
                                    ((Number) med.get("recetas")).intValue()));
                        }
                    }

                    // Parsear predicción semanal
                    Integer prediccionSemanal = null;
                    Object predObj = jsonResponse.get("prediccionSemanal");
                    if (predObj instanceof Number) {
                        prediccionSemanal = ((Number) predObj).intValue();
                    }

                    // Usar título de IA si está disponible, sino generar uno
                    String tituloFinal = tituloIA != null && !tituloIA.trim().isEmpty()
                            ? tituloIA
                            : generarTitulo(null, recopilarDatosSistema());

                    // Usar resumen de IA si está disponible, sino generar uno
                    String resumenFinal = resumenIA != null && !resumenIA.trim().isEmpty()
                            ? resumenIA
                            : generarResumenTexto(recopilarDatosSistema(), null);

                    // Usar indicadores de IA si están disponibles, sino generarlos
                    if (indicadores.isEmpty()) {
                        indicadores = generarIndicadoresClave(recopilarDatosSistema());
                    }

                    // Usar alertas de IA si están disponibles, sino generarlas
                    if (alertas.isEmpty()) {
                        alertas = generarAlertas(recopilarDatosSistema());
                    }

                    // Usar recomendaciones de IA si están disponibles, sino generarlas
                    if (recomendaciones.isEmpty()) {
                        recomendaciones = generarRecomendaciones(recopilarDatosSistema());
                    }

                    // Usar medicamentos de IA si están disponibles, sino usar datos del sistema
                    if (medicamentosTop.isEmpty()) {
                        medicamentosTop = generarTopMedicamentos(recopilarDatosSistema());
                    }

                    // Usar predicción de IA si está disponible, sino calcularla
                    String prediccionFinal = prediccionSemanal != null
                            ? String.format("%,d", prediccionSemanal)
                            : calcularPrediccionSemanal(recopilarDatosSistema());

                    // Crear DTO final
                    return new IAResumenDto(
                            tituloFinal,
                            resumenFinal,
                            indicadores,
                            alertas,
                            recomendaciones,
                            LocalDateTime.now(),
                            new MetricasDto(
                                    String.valueOf(recopilarDatosSistema().get("recetasTotales")),
                                    String.valueOf(recopilarDatosSistema().get("pacientesTotales")),
                                    "4.8"),
                            medicamentosTop,
                            new PrediccionesDto(prediccionFinal));

                } catch (Exception jsonParseError) {
                    log.warn("Error parseando JSON de DeepSeek, usando fallback: {}", jsonParseError.getMessage());
                    // Continuar con el fallback
                }
            }

            // Fallback: generar resumen con datos reales si el JSON no se pudo parsear
            log.info("Usando fallback: resumen generado con datos reales");
            return generarResumenConDatosReales(recopilarDatosSistema(), null);

        } catch (Exception e) {
            log.error("Error parseando respuesta de DeepSeek, generando resumen con datos reales", e);
            return generarResumenConDatosReales(recopilarDatosSistema(), null);
        }
    }

    // Metodo para limpiar la respuesta JSON de posibles textos adicionales
    private String limpiarRespuestaJSON(String respuesta) {
        if (respuesta == null)
            return "";

        String cleaned = respuesta.trim();

        // Buscar el inicio del JSON (primer '{')
        int startIndex = cleaned.indexOf('{');
        if (startIndex == -1)
            return cleaned;

        // Buscar el final del JSON (último '}')
        int endIndex = cleaned.lastIndexOf('}');
        if (endIndex == -1 || endIndex <= startIndex)
            return cleaned;

        // Extraer solo la parte JSON
        String jsonPart = cleaned.substring(startIndex, endIndex + 1);

        // Validar que sea JSON básico (tiene llaves balanceadas)
        long openBraces = jsonPart.chars().filter(ch -> ch == '{').count();
        long closeBraces = jsonPart.chars().filter(ch -> ch == '}').count();

        if (openBraces == closeBraces && openBraces > 0) {
            return jsonPart;
        }

        return cleaned; // Retornar original si no se puede limpiar
    }

    // Limpiar caché expirada cada hora
    @Scheduled(fixedRate = 3600000) // 1 hora en milisegundos
    public void limpiarCacheExpirada() {
        log.info("Limpiando caché expirada de IA");
        // cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    // Metodo para invalidar caché manualmente
    @CacheEvict(value = "iaResumen", allEntries = true)
    public void invalidarCache() {
        log.info("Invalidando caché de IA manualmente");
    }

    // Metodo para obtener estadísticas de caché
    public Map<String, Object> getEstadisticasCache() {
        Map<String, Object> stats = new HashMap<>();
        // stats.put("totalEntradas", cache.size());
        // stats.put("entradasExpiradas", cache.values().stream().mapToInt(entry ->
        // entry.isExpired() ? 1 : 0).sum());
        return stats;
    }

    /**
     * E1 - Predicción mensual de número de consultas
     * Genera predicción basada en tendencia histórica
     */
    public Map<String, Object> predecirConsultas(int meses) {
        Map<String, Long> monthlyData = statsService.getConsultationsByMonth();
        long consultasBase = statsService.getTotalConsultations();

        // Calcular promedio mensual reciente (últimos 3 meses)
        double promedioMensual = monthlyData.values().stream()
                .skip(Math.max(0, monthlyData.size() - 3))
                .mapToLong(Long::longValue)
                .average()
                .orElse(consultasBase > 0 ? consultasBase / 12.0 : 0);

        if (promedioMensual == 0)
            promedioMensual = 100; // Fallback mínimo

        // Calcular tasa de crecimiento real basada en los últimos 2 meses
        double growthRate = 0.02; // Default 2%
        List<String> sortedMonths = new ArrayList<>(monthlyData.keySet());
        Collections.sort(sortedMonths);

        if (sortedMonths.size() >= 2) {
            String lastMonth = sortedMonths.get(sortedMonths.size() - 1);
            String prevMonth = sortedMonths.get(sortedMonths.size() - 2);
            long lastCount = monthlyData.getOrDefault(lastMonth, 0L);
            long prevCount = monthlyData.getOrDefault(prevMonth, 0L);

            if (prevCount > 0) {
                double rawRate = (double) (lastCount - prevCount) / prevCount;
                // Limitar la tasa para evitar proyecciones extremas (-20% a +20%)
                growthRate = Math.max(-0.2, Math.min(0.2, rawRate));
            }
        }

        List<Map<String, Object>> predicciones = new ArrayList<>();

        for (int i = 1; i <= meses; i++) {
            Map<String, Object> pred = new HashMap<>();
            pred.put("mes", i);
            // Proyección con crecimiento calculado dinámicamente
            double proyeccion = promedioMensual * Math.pow(1.0 + growthRate, i);
            pred.put("consultas", (int) proyeccion);
            pred.put("confianza", Math.max(50, 90 - (i * 5))); // Confianza decreciente
            predicciones.add(pred);
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("predicciones", predicciones);
        resultado.put("baseHistorica", consultasBase);
        resultado.put("tendencia", growthRate > 0 ? "creciente" : (growthRate < 0 ? "decreciente" : "estable"));
        resultado.put("factores", Arrays.asList(
                "Tendencia histórica",
                String.format("Tasa de crecimiento calculada: %.1f%%", growthRate * 100),
                "Datos reales sistema"));

        return resultado;
    }

    /**
     * E8 - Predicción de necesidad de medicamentos
     */
    public Map<String, Object> predecirMedicamentos() {
        // Usar la predicción dinámica del StatsService en lugar de heurísticas
        // estáticas
        List<MedicationForecastDto> forecasts = statsService.getMedicationForecast();

        List<Map<String, Object>> predicciones = new ArrayList<>();
        for (MedicationForecastDto forecast : forecasts) {
            Map<String, Object> pred = new HashMap<>();
            pred.put("medicamento", forecast.getMedication());
            // En el modelo actual, currentStock representa uso reciente (recetas actuales)
            pred.put("recetasActuales", forecast.getCurrentStock());
            pred.put("recetasEstimadas", forecast.getPredictedDemand());
            pred.put("stockRecomendado", forecast.getRecommendedStock());
            pred.put("prioridad", forecast.getGrowthRate() > 10 ? "alta" : "media");
            predicciones.add(pred);
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("predicciones", predicciones);
        resultado.put("mesProyectado", "Próximo mes");
        resultado.put("recomendacion", "Ajustar stock según predicción dinámica de demanda");

        return resultado;
    }

    /**
     * E10 - Detectar diagnósticos emergentes automáticamente
     */
    public Map<String, Object> detectarDiagnosticosEmergentes() {
        List<EmergingDiagnosisDto> emerging = statsService.getEmergingDiagnoses();

        List<Map<String, Object>> emergentes = new ArrayList<>();
        for (EmergingDiagnosisDto diag : emerging) {
            Map<String, Object> item = new HashMap<>();
            item.put("diagnostico", diag.getDiagnosis());
            item.put("casosActuales", diag.getCurrentMonth());
            item.put("casosEsperados", diag.getPreviousMonth());
            item.put("incremento", diag.getVariation());

            item.put("severidad", diag.getVariation() > 50 ? "crítica" : (diag.getVariation() > 20 ? "alta" : "media"));
            item.put("fechaDeteccion", LocalDateTime.now());
            emergentes.add(item);
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("diagnosticosEmergentes", emergentes);
        resultado.put("totalEmergentes", emergentes.size());
        resultado.put("alertaCritica", emergentes.stream().anyMatch(e -> "crítica".equals(e.get("severidad"))));
        resultado.put("recomendacion", "Vigilancia basada en datos reales");

        return resultado;
    }

    /**
     * E4 - Proyección de carga médica por especialidad
     */
    public Map<String, Object> proyectarCargaMedica() {
        List<SpecialtyWorkloadDto> specialties = statsService.getSpecialtyWorkload();

        List<Map<String, Object>> proyecciones = new ArrayList<>();
        int totalAlerts = 0;

        for (SpecialtyWorkloadDto spec : specialties) {
            Map<String, Object> proy = new HashMap<>();
            long cargaActual = spec.getTotalConsultations();
            int totalProfessionals = spec.getTotalProfessionals();

            proy.put("especialidad", spec.getSpecialty());
            proy.put("cargaActual", cargaActual);

            // Proyección: Crecimiento estimado del 5%
            long cargaProyectada = Math.round(cargaActual * 1.05);
            proy.put("cargaProyectada", cargaProyectada);

            // Capacidad Real Mensual (30 días):
            // - Cada profesional puede atender ~12 consultas/día
            // - En un mes (22 días laborables) = 12 × 22 = 264 consultas/mes/profesional
            // - Considerando eficiencia del 85%: 264 × 0.85 = 224 consultas/mes/profesional
            long capacidadRealMensual = totalProfessionals * 224L;
            if (capacidadRealMensual == 0) {
                capacidadRealMensual = 1; // Evitar división por cero
            }

            proy.put("capacidadMaxima", capacidadRealMensual);

            // Calcular % ocupación actual y proyectado
            double ocupacionActual = Math.min(100.0, (double) cargaActual / capacidadRealMensual * 100);
            double ocupacionProyectada = Math.min(100.0, (double) cargaProyectada / capacidadRealMensual * 100);

            proy.put("porcentajeOcupacion", Math.round(ocupacionActual * 10) / 10.0); // Redondear a 1 decimal

            // Determinar estado según ocupación proyectada
            String estado;
            if (ocupacionProyectada >= 90) {
                estado = "Crítico - Requiere más profesionales";
                totalAlerts++;
            } else if (ocupacionProyectada >= 75) {
                estado = "Alta demanda - Monitorear";
                totalAlerts++;
            } else if (ocupacionProyectada >= 50) {
                estado = "Capacidad adecuada";
            } else {
                estado = "Capacidad disponible";
            }

            proy.put("estado", estado);
            proy.put("promedioPorProfesional", totalProfessionals > 0 ?
                    Math.round((double) cargaActual / totalProfessionals * 10) / 10.0 : 0);

            proyecciones.add(proy);
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("proyecciones", proyecciones);
        resultado.put("mesProyectado", "Próximo mes");

        List<String> alertas = new ArrayList<>();
        alertas.add("Capacidad calculada: 224 consultas/mes/profesional (22 días × 12 cupos × 85% eficiencia)");
        if (totalAlerts > 0) {
            alertas.add(totalAlerts + " especialidad(es) requieren atención");
        } else {
            alertas.add("Todas las especialidades operan en niveles adecuados");
        }
        resultado.put("alertas", alertas);

        return resultado;
    }

    /**
     * Analizar tendencias por tipo
     */
    public Map<String, Object> analizarTendencias(String tipo) {
        Map<String, Object> analisis = new HashMap<>();

        if ("consultas".equalsIgnoreCase(tipo)) {
            analisis.put("tipo", "consultas");

            // Calcular tendencia real
            try {
                Map<String, Long> monthlyData = statsService.getConsultationsByMonth();
                if (monthlyData.size() >= 2) {
                    // Obtener valores (asumiendo que el mapa tiene orden o podemos iterar)
                    // Idealmente statsService debería devolver lista ordenada
                    List<Long> values = new ArrayList<>(monthlyData.values());
                    long last = values.get(values.size() - 1);
                    long prev = values.get(values.size() - 2);

                    double change = prev > 0 ? ((double) (last - prev) / prev) * 100 : 0;
                    analisis.put("tendenciaActual", change >= 0 ? "creciente" : "decreciente");
                    analisis.put("cambioMensual", change);
                    analisis.put("prediccionTrimestre", change > 0 ? "tendencia al alza" : "tendencia a la baja");
                } else {
                    analisis.put("tendenciaActual", "estable");
                    analisis.put("cambioMensual", 0.0);
                    analisis.put("prediccionTrimestre", "datos insuficientes");
                }
            } catch (Exception e) {
                log.error("Error calculando tendencia", e);
                analisis.put("tendenciaActual", "desconocida");
                analisis.put("cambioMensual", 0.0);
            }

            analisis.put("factores", Arrays.asList("Temporada actual", "Datos históricos del sistema"));

        } else if ("medicamentos".equalsIgnoreCase(tipo)) {
            analisis.put("tipo", "medicamentos");
            analisis.put("tendenciaActual", "estable"); // Simplificación
            analisis.put("cambioMensual", 0.0);
            analisis.put("factores", Arrays.asList("Consumo histórico"));
        } else if ("diagnosticos".equalsIgnoreCase(tipo)) {
            analisis.put("tipo", "diagnosticos");
            analisis.put("tendenciaActual", "variable");
            analisis.put("cambioMensual", 0.0);
            analisis.put("factores", Arrays.asList("Nuevos casos detectados"));
        } else {
            analisis.put("error", "Tipo no válido");
        }

        return analisis;
    }

    /**
     * Detectar anomalías en los datos
     */
    /**
     * Detectar anomalías en los datos usando estadísticas reales
     */
    public Map<String, Object> detectarAnomalias() {
        List<Map<String, Object>> anomalias = new ArrayList<>();

        try {
            // Obtener consultas diarias de los últimos 30 días
            List<DailyConsultationDto> history = statsService.getDailyConsultations(30);

            if (history.size() > 7) {
                // Calcular media móvil simple de 7 días
                double sum = 0;
                for (int i = 0; i < history.size(); i++) {
                    sum += history.get(i).getTotalConsultations();
                }
                double average = sum / history.size();

                // Buscar desviaciones > 50% del promedio
                for (DailyConsultationDto day : history) {
                    double deviation = day.getTotalConsultations() - average;
                    double percentDeviation = average > 0 ? (deviation / average) * 100 : 0;

                    if (Math.abs(percentDeviation) > 50) {
                        Map<String, Object> anomalia = new HashMap<>();
                        anomalia.put("tipo", percentDeviation > 0 ? "Pico inusual" : "Caída inusual");
                        anomalia.put("fecha", day.getDate());
                        anomalia.put("valor", day.getTotalConsultations());
                        anomalia.put("esperado", (int) average);
                        anomalia.put("desviacion", percentDeviation);
                        anomalia.put("severidad", Math.abs(percentDeviation) > 80 ? "alta" : "media");
                        anomalia.put("fechaDeteccion", LocalDateTime.now());
                        anomalias.add(anomalia);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error detectando anomalías", e);
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("anomalias", anomalias);
        resultado.put("totalAnomalias", anomalias.size());
        resultado.put("periodoAnalizado", "Últimos 30 días");

        return resultado;
    }

    /**
     * Generar recomendaciones contextuales
     */
    public Map<String, Object> generarRecomendaciones(String contexto) {
        List<String> recomendaciones = new ArrayList<>();

        switch (contexto.toLowerCase()) {
            case "demanda":
                recomendaciones.add("Redistribuir recursos hacia Medicina General");
                recomendaciones.add("Considerar extensión de horarios en días de alta demanda");
                recomendaciones.add("Implementar sistema de citas prioritarias");
                break;
            case "medicamentos":
                recomendaciones.add("Aumentar stock de medicamentos respiratorios en 15%");
                recomendaciones.add("Revisar inventario de medicamentos crónicos");
                recomendaciones.add("Coordinar con farmacia para abastecimiento");
                break;
            case "personal":
                recomendaciones.add("Reforzar equipo de Medicina General con 1-2 profesionales");
                recomendaciones.add("Optimizar distribución de turnos médicos");
                recomendaciones.add("Considerar contratación temporal en temporada alta");
                break;
            default:
                recomendaciones.add("Optimizar sistema de agendamiento");
                recomendaciones.add("Implementar telemedicina para consultas de seguimiento");
                recomendaciones.add("Mejorar campañas preventivas de salud");
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("contexto", contexto);
        resultado.put("recomendaciones", recomendaciones);
        resultado.put("prioridad", "alta");
        resultado.put("fechaGeneracion", LocalDateTime.now());

        return resultado;
    }

    private Map<String, Object> recopilarDatosSistema() {
        log.info("🔍 Recopilando datos reales del sistema desde la base de datos");

        Map<String, Object> datos = new HashMap<>();

        try {
            // Consultas totales del mes actual
            long consultasTotales = consultationRepository.count();
            datos.put("consultasTotales", (int) consultasTotales);
            log.info("📊 Consultas totales: {}", consultasTotales);

            // Pacientes únicos atendidos
            long pacientesTotales = patientRepository.count();
            datos.put("pacientesTotales", (int) pacientesTotales);
            log.info("👥 Pacientes únicos atendidos: {}", pacientesTotales);

            // Recetas emitidas
            long recetasTotales = prescriptionRepository.count();
            datos.put("recetasTotales", (int) recetasTotales);
            log.info("💊 Recetas emitidas: {}", recetasTotales);

            // Pacientes crónicos (estimación: pacientes con más de 3 consultas en el último
            // año)
            // Para simplificar, usamos una estimación del 15%
            int pacientesCronicos = (int) (pacientesTotales * 0.15);
            datos.put("pacientesCronicos", pacientesCronicos);
            datos.put("porcentajeCronicos", 15.0);
            log.info("🏥 Pacientes crónicos: {} ({}%)", pacientesCronicos, 15.0);

            // Top medicamentos más recetados (consulta JPQL)
            log.info("🔍 Consultando top medicamentos...");
            List<Map<String, Object>> topMedicamentos = obtenerTopMedicamentos();
            datos.put("topMedicamentos", topMedicamentos);
            log.info("💊 Top medicamentos obtenidos: {} medicamentos", topMedicamentos.size());
            for (int i = 0; i < Math.min(3, topMedicamentos.size()); i++) {
                Map<String, Object> med = topMedicamentos.get(i);
                log.info("   {}. {}: {} recetas", i + 1, med.get("nombre"), med.get("recetas"));
            }

            // Distribución por edad
            Map<String, Integer> pacientesPorEdad = obtenerDistribucionPorEdad();
            datos.put("pacientesPorEdad", pacientesPorEdad);
            log.info("📈 Distribución por edad: {}", pacientesPorEdad);

            // Distribución FONASA
            Map<String, Integer> distribucionFonasa = obtenerDistribucionFonasa();
            datos.put("distribucionFonasa", distribucionFonasa);
            log.info("🏛️ Distribución FONASA: {}", distribucionFonasa);

            log.info("✅ Datos recopilados exitosamente: {} consultas, {} pacientes, {} recetas, {} medicamentos top",
                    consultasTotales, pacientesTotales, recetasTotales, topMedicamentos.size());

        } catch (Exception e) {
            log.error("❌ Error recopilando datos del sistema, usando datos de respaldo", e);
            // Fallback a datos hardcodeados si hay error en BD
            return obtenerDatosRespaldo();
        }

        return datos;
    }

    private List<Map<String, Object>> obtenerTopMedicamentos() {
        // Consulta JPQL para obtener top medicamentos más recetados
        try {
            // Primero verificar que existen datos en las tablas
            log.info("🔍 Verificando existencia de datos en BD...");
            Long totalMedicamentos = prescriptionRepository.countTotalMedicamentos();
            Long totalPrescriptionMedications = prescriptionRepository.countTotalPrescriptionMedications();

            log.info("📊 Datos encontrados en BD:");
            log.info("   💊 Total medicamentos: {}", totalMedicamentos);
            log.info("   📋 Total prescripciones-medicamentos: {}", totalPrescriptionMedications);

            if (totalMedicamentos == 0) {
                log.warn("⚠️ No hay medicamentos en la tabla 'medication'");
                return new ArrayList<>();
            }

            if (totalPrescriptionMedications == 0) {
                log.warn("⚠️ No hay registros en la tabla 'prescription_medication'");
                return new ArrayList<>();
            }

            log.info("🔍 Ejecutando consulta JPQL para top medicamentos...");
            List<Object[]> results = prescriptionRepository.findTopMedicamentos(PageRequest.of(0, 10)); // Obtener más
                                                                                                        // para tener
                                                                                                        // opciones
            log.info("📊 Consulta JPQL ejecutada, resultados obtenidos: {}", results.size());

            List<Map<String, Object>> topMedicamentos = new ArrayList<>();

            log.info("🔄 Procesando resultados de consulta (nombres REALES de BD)...");
            for (int i = 0; i < results.size(); i++) {
                Object[] result = results.get(i);
                String nombreRealBD = (String) result[0]; // Nombre REAL de la base de datos
                Long count = (Long) result[1];
                log.info("   Resultado {}: '{}' -> {} recetas (NOMBRE REAL DE BD)", i + 1, nombreRealBD, count);

                Map<String, Object> med = new HashMap<>();
                med.put("nombre", nombreRealBD); 
                med.put("recetas", count.intValue());
                topMedicamentos.add(med);
            }

            log.info("✅ Procesamiento completado. Total medicamentos REALES de BD: {}", topMedicamentos.size());

            
            if (topMedicamentos.isEmpty()) {
                log.warn("⚠️ La consulta JPQL no devolvió resultados");
                return new ArrayList<>();
            }

            // Retornar todos los medicamentos que existen en BD (máximo 3)
            List<Map<String, Object>> resultadoFinal = topMedicamentos.subList(0, Math.min(3, topMedicamentos.size()));
            log.info("🎯 Retornando medicamentos REALES de BD:");
            for (int i = 0; i < resultadoFinal.size(); i++) {
                Map<String, Object> med = resultadoFinal.get(i);
                log.info("   {}. {}: {} recetas (REAL DE BD)", i + 1, med.get("nombre"), med.get("recetas"));
            }

            return resultadoFinal;

        } catch (Exception e) {
            log.error("❌ Error obteniendo top medicamentos de BD: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private Map<String, Integer> obtenerDistribucionPorEdad() {
        try {
            Map<String, Integer> distribucion = new HashMap<>();
            statsService.getPatientsByAgeAndSex().forEach(dto -> distribucion.put(dto.rangoEdad(), (int) dto.total()));
            return distribucion;
        } catch (Exception e) {
            log.warn("Error obteniendo distribución por edad, usando datos de respaldo", e);
            return Map.of("0-17", 0, "18-29", 0, "30-49", 0, "50-64", 0, "65+", 0);
        }
    }

    private Map<String, Integer> obtenerDistribucionFonasa() {
        try {
            Map<String, Integer> distribucion = new HashMap<>();
            statsService.getPatientsByFonasa().forEach(dto -> distribucion.put(dto.tier(), (int) dto.count()));
            return distribucion;
        } catch (Exception e) {
            log.warn("Error obteniendo distribución FONASA, usando datos de respaldo", e);
            return Map.of("A", 0, "B", 0, "C", 0, "D", 0);
        }
    }

    private Map<String, Object> obtenerDatosRespaldo() {
        log.warn("Usando datos de respaldo por error en base de datos. Retornando valores neutros.");
        Map<String, Object> datos = new HashMap<>();
        datos.put("consultasTotales", 0);
        datos.put("pacientesTotales", 0);
        datos.put("recetasTotales", 0);
        datos.put("pacientesCronicos", 0);
        datos.put("porcentajeCronicos", 0.0);

        datos.put("topMedicamentos", new ArrayList<>());
        datos.put("pacientesPorEdad", new HashMap<>());
        datos.put("distribucionFonasa", new HashMap<>());

        return datos;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    @SuppressWarnings("unchecked")
    private String getTopMedicamento(Map<String, Object> datos, int index) {
        try {
            List<Map<String, Object>> topMeds = (List<Map<String, Object>>) datos.get("topMedicamentos");
            if (topMeds != null && topMeds.size() > index) {
                return (String) topMeds.get(index).get("nombre");
            }
        } catch (Exception e) {
            log.warn("Error obteniendo top medicamento index {}: {}", index, e.getMessage());
        }
        return "N/A";
    }

    @SuppressWarnings("unchecked")
    private Integer getTopRecetas(Map<String, Object> datos, int index) {
        try {
            List<Map<String, Object>> topMeds = (List<Map<String, Object>>) datos.get("topMedicamentos");
            if (topMeds != null && topMeds.size() > index) {
                Object recetas = topMeds.get(index).get("recetas");
                if (recetas instanceof Integer) {
                    return (Integer) recetas;
                }
            }
        } catch (Exception e) {
            log.warn("Error obteniendo top recetas index {}: {}", index, e.getMessage());
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private Integer getPacientesPorEdad(Map<String, Object> datos, String rango) {
        try {
            Map<String, Integer> pacientesPorEdad = (Map<String, Integer>) datos.get("pacientesPorEdad");
            if (pacientesPorEdad != null && pacientesPorEdad.containsKey(rango)) {
                return pacientesPorEdad.get(rango);
            }
        } catch (Exception e) {
            log.warn("Error obteniendo pacientes por edad rango {}: {}", rango, e.getMessage());
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private Integer getFonasaCount(Map<String, Object> datos, String tramo) {
        try {
            Map<String, Integer> distribucionFonasa = (Map<String, Integer>) datos.get("distribucionFonasa");
            if (distribucionFonasa != null && distribucionFonasa.containsKey(tramo)) {
                return distribucionFonasa.get(tramo);
            }
        } catch (Exception e) {
            log.warn("Error obteniendo FONASA tramo {}: {}", tramo, e.getMessage());
        }
        return 0;
    }

    private String generarTitulo(String periodo, Map<String, Object> datos) {
        String periodoTexto = periodo != null ? periodo : "Periodo Actual";
        int consultas = datos.get("consultasTotales") != null ? (Integer) datos.get("consultasTotales") : 0;
        return String.format("📊 Análisis Ejecutivo CESFAM - %s (%,d Consultas)", periodoTexto, consultas);
    }

    private String generarResumenTexto(Map<String, Object> datos, String periodo) {
        String periodoTexto = periodo != null ? periodo : "el periodo actual";
        int consultas = (Integer) datos.getOrDefault("consultasTotales", 0);
        int pacientes = (Integer) datos.getOrDefault("pacientesTotales", 0);
        int recetas = (Integer) datos.getOrDefault("recetasTotales", 0);
        int cronicos = (Integer) datos.getOrDefault("pacientesCronicos", 0);

        return String.format(
                "Durante %s, el CESFAM registró %,d consultas atendiendo a %,d pacientes únicos. " +
                        "Se emitieron %,d recetas médicas, atendiendo las necesidades de salud de la población. " +
                        "Del total de pacientes, %,d son crónicos, lo que representa el 15%% del total y requiere " +
                        "seguimiento especializado.",
                periodoTexto, consultas, pacientes, recetas, cronicos);
    }

    private List<IndicadorClaveDto> generarIndicadoresClave(Map<String, Object> datos) {
        List<IndicadorClaveDto> indicadores = new ArrayList<>();

        indicadores.add(new IndicadorClaveDto(
                "Consultas Mensuales",
                String.format("%,d", (Integer) datos.getOrDefault("consultasTotales", 0)),
                "neutral"));

        indicadores.add(new IndicadorClaveDto(
                "Pacientes Únicos Atendidos",
                String.format("%,d", (Integer) datos.getOrDefault("pacientesTotales", 0)),
                "neutral"));

        indicadores.add(new IndicadorClaveDto(
                "Recetas Emitidas",
                String.format("%,d", (Integer) datos.getOrDefault("recetasTotales", 0)),
                "neutral"));

        indicadores.add(new IndicadorClaveDto(
                "Pacientes Crónicos",
                String.format("%,d (15%%)", (Integer) datos.getOrDefault("pacientesCronicos", 0)),
                "neutral"));

        return indicadores;
    }

    private List<String> generarAlertas(Map<String, Object> datos) {
        List<String> alertas = new ArrayList<>();

        int cronicos = (Integer) datos.getOrDefault("pacientesCronicos", 0);
        if (cronicos > 2000) {
            alertas.add(
                    "ALERTA: Alto número de pacientes crónicos detectado. Se recomienda reforzar programas de seguimiento según requisito E6.");
        }

        alertas.add(
                "ALERTA: Se recomienda monitorear distribución por edad según requisito E3 para optimizar recursos.");
        alertas.add("ALERTA: Revisar distribución FONASA según requisito E11 para planificación de recursos.");

        return alertas;
    }

    private List<String> generarRecomendaciones(Map<String, Object> datos) {
        List<String> recomendaciones = new ArrayList<>();

        recomendaciones.add("Implementar programas de prevención según distribución etaria (requisito E3)");
        recomendaciones.add("Optimizar gestión de medicamentos según análisis de prescripciones (requisito E8)");
        recomendaciones.add("Fortalecer seguimiento de pacientes crónicos (requisito E6)");
        recomendaciones.add("Ajustar recursos según distribución FONASA (requisito E11)");

        return recomendaciones;
    }

    @SuppressWarnings("unchecked")
    private List<MedicamentoTopDto> generarTopMedicamentos(Map<String, Object> datos) {
        List<MedicamentoTopDto> medicamentos = new ArrayList<>();

        try {
            List<Map<String, Object>> topMeds = (List<Map<String, Object>>) datos.get("topMedicamentos");
            if (topMeds != null) {
                for (Map<String, Object> med : topMeds) {
                    medicamentos.add(new MedicamentoTopDto(
                            (String) med.get("nombre"),
                            (Integer) med.get("recetas")));
                }
            }
        } catch (Exception e) {
            log.warn("Error generando top medicamentos: {}", e.getMessage());
        }

        return medicamentos;
    }

    private String calcularPrediccionSemanal(Map<String, Object> datos) {
        try {
            int consultasMensuales = (Integer) datos.getOrDefault("consultasTotales", 0);
            int prediccion = (int) (consultasMensuales * 1.05 / 4);
            return String.format("%,d", prediccion);
        } catch (Exception e) {
            log.warn("Error calculando predicción semanal: {}", e.getMessage());
            return "N/A";
        }
    }

    @SuppressWarnings("unchecked")
    private String llamarGroqAPI(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", groqModel,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", prompt)),
                    "temperature", 0.7,
                    "max_tokens", 3000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    groqApiUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> body = response.getBody();
            if (body != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            throw new RuntimeException("Respuesta inválida de Groq API");

        } catch (Exception e) {
            log.error("Error llamando a Groq API", e);
            throw new RuntimeException("Error en Groq API service", e);
        }
    }

    private IAResumenDto generarResumenConDatosReales(Map<String, Object> datos, String periodo) {
        String titulo = generarTitulo(periodo, datos);
        String resumen = generarResumenTexto(datos, periodo);
        List<IndicadorClaveDto> indicadores = generarIndicadoresClave(datos);
        List<String> alertas = generarAlertas(datos);
        List<String> recomendaciones = generarRecomendaciones(datos);
        List<MedicamentoTopDto> medicamentosTop = generarTopMedicamentos(datos);
        String prediccion = calcularPrediccionSemanal(datos);

        return new IAResumenDto(
                titulo,
                resumen,
                indicadores,
                alertas,
                recomendaciones,
                LocalDateTime.now(),
                new MetricasDto(
                        String.valueOf(datos.get("recetasTotales")),
                        String.valueOf(datos.get("pacientesTotales")),
                        "4.8"),
                medicamentosTop,
                new PrediccionesDto(prediccion));
    }


}


