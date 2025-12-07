package cl.sicc.siccapi.stats.controller;

import cl.sicc.siccapi.stats.dto.*;
import cl.sicc.siccapi.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        System.out.println("🔷 [STATS-CONTROLLER] GET /api/stats/dashboard - Solicitud recibida");
        DashboardStatsDto result = statsService.getDashboardStats();
        System.out.println("✅ [STATS-CONTROLLER] GET /api/stats/dashboard - Respuesta enviada");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/weekly-trend")
    public ResponseEntity<List<WeeklyTrendDto>> getWeeklyTrend() {
        return ResponseEntity.ok(statsService.getWeeklyTrend());
    }

    @GetMapping("/medication")
    public ResponseEntity<MedicationStatsDto> getMedicationStats() {
        return ResponseEntity.ok(statsService.getMedicationStats());
    }

    @GetMapping("/patient")
    public ResponseEntity<PatientStatsDto> getPatientStats() {
        return ResponseEntity.ok(statsService.getPatientStats());
    }

    @GetMapping("/diagnosis")
    public ResponseEntity<DiagnosisStatsDto> getDiagnosisStats() {
        return ResponseEntity.ok(statsService.getDiagnosisStats());
    }

    @GetMapping("/specialty")
    public ResponseEntity<List<SpecialtyCountDto>> getConsultationsBySpecialty() {
        return ResponseEntity.ok(statsService.getConsultationsBySpecialty());
    }

    @GetMapping("/patient/chronic-summary")
    public ResponseEntity<ChronicSummaryDto> getChronicSummary() {
        return ResponseEntity.ok(statsService.getChronicSummary());
    }

    @GetMapping("/patient/chronic-list")
    public ResponseEntity<List<ChronicPatientDto>> getChronicPatients(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(statsService.getChronicPatients(limit));
    }

    @GetMapping("/patient/diseases-by-age")
    public ResponseEntity<List<DiseaseByAgeGroupDto>> getDiseasesByAgeGroup() {
        return ResponseEntity.ok(statsService.getDiseasesByAgeGroup());
    }

    @GetMapping("/patient/diseases-by-fonasa")
    public ResponseEntity<List<DiseaseByFonasaDto>> getDiseasesByFonasa() {
        return ResponseEntity.ok(statsService.getDiseasesByFonasa());
    }

    /**
     * Distribución de pacientes por grupo de edad y sexo
     * Usado por pacientes-perfil.component.ts para la tabla edad/sexo
     */
    @GetMapping("/patient/by-age-and-sex")
    public ResponseEntity<List<AgeGroupBySexDto>> getPatientsByAgeAndSex() {
        return ResponseEntity.ok(statsService.getPatientsByAgeAndSex());
    }

    @GetMapping("/prescription/details")
    public ResponseEntity<List<PrescriptionDetailDto>> getPrescriptionDetails(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(statsService.getPrescriptionDetails(limit));
    }

    @GetMapping("/medication/by-age-group")
    public ResponseEntity<List<MedicationByAgeGroupDto>> getMedicationsByAgeGroup() {
        return ResponseEntity.ok(statsService.getMedicationsByAgeGroup());
    }

    @GetMapping("/diagnosis/emerging")
    public ResponseEntity<List<EmergingDiagnosisDto>> getEmergingDiagnoses() {
        return ResponseEntity.ok(statsService.getEmergingDiagnoses());
    }

    @GetMapping("/diagnosis/by-sex")
    public ResponseEntity<List<DiagnosisBySexDto>> getDiagnosesBySex() {
        return ResponseEntity.ok(statsService.getDiagnosesBySex());
    }

    @GetMapping("/diagnosis/by-fonasa")
    public ResponseEntity<List<DiseaseByFonasaDto>> getDiagnosesByFonasa() {
        return ResponseEntity.ok(statsService.getDiagnosesByFonasa());
    }

    @GetMapping("/diagnosis/by-specialty")
    public ResponseEntity<List<SpecialtyDiagnosisDto>> getDiagnosesBySpecialty() {
        return ResponseEntity.ok(statsService.getDiagnosesBySpecialty());
    }

    @GetMapping("/medication/forecast")
    public ResponseEntity<List<MedicationForecastDto>> getMedicationForecast() {
        return ResponseEntity.ok(statsService.getMedicationForecast());
    }

    @GetMapping("/consultations/daily")
    public ResponseEntity<List<DailyConsultationDto>> getDailyConsultations(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(statsService.getDailyConsultations(days));
    }

    @GetMapping("/workload/professional")
    public ResponseEntity<List<ProfessionalWorkloadDto>> getProfessionalWorkload() {
        System.out.println("🔷 [STATS-CONTROLLER] GET /api/stats/workload/professional - Solicitud recibida");
        List<ProfessionalWorkloadDto> result = statsService.getProfessionalWorkload();
        System.out.println("✅ [STATS-CONTROLLER] GET /api/stats/workload/professional - Devolviendo " + result.size() + " registros");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/workload/specialty")
    public ResponseEntity<List<SpecialtyWorkloadDto>> getSpecialtyWorkload() {
        System.out.println("🔷 [STATS-CONTROLLER] GET /api/stats/workload/specialty - Solicitud recibida");
        List<SpecialtyWorkloadDto> result = statsService.getSpecialtyWorkload();
        System.out.println("✅ [STATS-CONTROLLER] GET /api/stats/workload/specialty - Devolviendo " + result.size() + " registros");
        return ResponseEntity.ok(result);
    }

    // ========== NUEVOS ENDPOINTS PARA REQUISITOS PENDIENTES ==========

    /**
     * E13: Gráfico anual de derivaciones a especialistas
     */
    @GetMapping("/referrals")
    public ResponseEntity<ReferralStatsDto> getReferralStats() {
        return ResponseEntity.ok(statsService.getReferralStats());
    }

    /**
     * T7/O8: Consultas por tipo (urgentes vs generales)
     */
    @GetMapping("/consultations/by-type")
    public ResponseEntity<List<ConsultationTypeStatsDto>> getConsultationsByType() {
        return ResponseEntity.ok(statsService.getConsultationsByTypeDetailed());
    }

    /**
     * E6: Tendencia detallada de enfermedades respiratorias
     */
    @GetMapping("/diagnosis/respiratory-trend")
    public ResponseEntity<RespiratoryTrendDto> getRespiratoryTrendDetailed() {
        System.out.println("🔷 [STATS-CONTROLLER] GET /api/stats/diagnosis/respiratory-trend - Solicitud recibida");
        RespiratoryTrendDto result = statsService.getRespiratoryTrendDetailed();
        System.out.println("✅ [STATS-CONTROLLER] GET /api/stats/diagnosis/respiratory-trend - Respuesta enviada");
        return ResponseEntity.ok(result);
    }

    /**
     * T14: Recetas frecuentes por grupo etario
     */
    @GetMapping("/prescription/by-age-group")
    public ResponseEntity<List<PrescriptionsByAgeGroupDto>> getPrescriptionsByAgeGroup() {
        return ResponseEntity.ok(statsService.getPrescriptionsByAgeGroupDetailed());
    }

    @GetMapping("/demand/prediction")
    public ResponseEntity<DemandPredictionDto> getDemandPrediction() {
        return ResponseEntity.ok(statsService.getDemandPrediction());
    }

    @GetMapping("/demand/ai-analysis")
    public ResponseEntity<AIAnalysisDto> getAIAnalysis() {
        return ResponseEntity.ok(statsService.getAIAnalysis());
    }

    @GetMapping("/prescription/weekly-trend")
    public ResponseEntity<List<WeeklyTrendDto>> getPrescriptionWeeklyTrend() {
        return ResponseEntity.ok(statsService.getPrescriptionWeeklyTrend());
    }
}

