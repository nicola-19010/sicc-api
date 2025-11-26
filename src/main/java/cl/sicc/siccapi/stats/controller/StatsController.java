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
        return ResponseEntity.ok(statsService.getDashboardStats());
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
}
