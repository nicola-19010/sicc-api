package cl.sicc.siccapi.stats.service;

import cl.sicc.siccapi.stats.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    @PersistenceContext
    private EntityManager entityManager;

    public DashboardStatsDto getDashboardStats() {
        long totalConsultations = getTotalConsultations();
        long totalPatients = getTotalPatients();
        long totalPrescriptions = getTotalPrescriptions();
        double averageConsultationsPerDay = totalConsultations > 0 ? (double) totalConsultations / 30 : 0; // Aproximado
        Map<String, Long> consultationsByType = getConsultationsByType();
        Map<String, Long> consultationsByMonth = getConsultationsByMonth();

        return new DashboardStatsDto(
            totalConsultations,
            totalPatients,
            totalPrescriptions,
            averageConsultationsPerDay,
            consultationsByType,
            consultationsByMonth
        );
    }

    public List<WeeklyTrendDto> getWeeklyTrend() {
        String sql = """
            SELECT
                CASE EXTRACT(DOW FROM c.date)
                    WHEN 0 THEN 'Dom'
                    WHEN 1 THEN 'Lun'
                    WHEN 2 THEN 'Mar'
                    WHEN 3 THEN 'Mié'
                    WHEN 4 THEN 'Jue'
                    WHEN 5 THEN 'Vie'
                    WHEN 6 THEN 'Sáb'
                END as day,
                COUNT(*) as consultations,
                EXTRACT(DOW FROM c.date) as dayOfWeek
            FROM consultation c
            WHERE c.date >= CURRENT_DATE - INTERVAL '7 days'
            GROUP BY EXTRACT(DOW FROM c.date)
            ORDER BY EXTRACT(DOW FROM c.date)
            """;

        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new WeeklyTrendDto(
                (String) row[0],
                ((Number) row[1]).longValue(),
                ((Number) row[2]).intValue()
            ))
            .collect(Collectors.toList());
    }

    public MedicationStatsDto getMedicationStats() {
        List<MedicationCountDto> topMedications = getTopMedications();
        List<MonthlyCountDto> monthlyPrescriptions = getMonthlyPrescriptions();

        return new MedicationStatsDto(topMedications, monthlyPrescriptions);
    }

    public PatientStatsDto getPatientStats() {
        List<AgeGroupCountDto> byAge = getPatientsByAge();
        List<GenderCountDto> bySex = getPatientsBySex();
        List<FonasaCountDto> byFonasa = getPatientsByFonasa();
        List<SectorCountDto> bySector = getPatientsBySector();

        return new PatientStatsDto(byAge, bySex, byFonasa, bySector);
    }

    public DiagnosisStatsDto getDiagnosisStats() {
        List<DiagnosisCountDto> topDiagnoses = getTopDiagnoses();
        List<SpecialtyDiagnosisDto> diagnosisBySpecialty = getDiagnosisBySpecialty();
        List<MonthlyCountDto> respiratoryTrend = getRespiratoryTrend();

        return new DiagnosisStatsDto(topDiagnoses, diagnosisBySpecialty, respiratoryTrend);
    }

    public List<SpecialtyCountDto> getConsultationsBySpecialty() {
        return getConsultationsBySpecialtyPrivate();
    }

    private long getTotalConsultations() {
        Query query = entityManager.createQuery("SELECT COUNT(c) FROM Consultation c");
        return (Long) query.getSingleResult();
    }

    private long getTotalPatients() {
        Query query = entityManager.createQuery("SELECT COUNT(p) FROM Patient p");
        return (Long) query.getSingleResult();
    }

    private long getTotalPrescriptions() {
        Query query = entityManager.createQuery("SELECT COUNT(p) FROM Prescription p");
        return (Long) query.getSingleResult();
    }

    private Map<String, Long> getConsultationsByType() {
        String sql = "SELECT c.type, COUNT(*) FROM consultation c GROUP BY c.type";
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> ((Number) row[1]).longValue()
            ));
    }

    private Map<String, Long> getConsultationsByMonth() {
        String sql = """
            SELECT
                TO_CHAR(c.date, 'YYYY-MM') as month,
                COUNT(*) as count
            FROM consultation c
            GROUP BY TO_CHAR(c.date, 'YYYY-MM')
            ORDER BY TO_CHAR(c.date, 'YYYY-MM')
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> ((Number) row[1]).longValue()
            ));
    }

    private List<MedicationCountDto> getTopMedications() {
        String sql = """
            SELECT m.name, COUNT(pm) as count
            FROM medication m
            JOIN prescription_medication pm ON m.id = pm.medication_id
            GROUP BY m.id, m.name
            ORDER BY COUNT(pm) DESC
            LIMIT 10
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new MedicationCountDto(
                (String) row[0],
                ((Number) row[1]).longValue()
            ))
            .collect(Collectors.toList());
    }

    private List<MonthlyCountDto> getMonthlyPrescriptions() {
        String sql = """
            SELECT
                TO_CHAR(p.date, 'YYYY-MM') as month,
                COUNT(*) as total
            FROM prescription p
            GROUP BY TO_CHAR(p.date, 'YYYY-MM')
            ORDER BY TO_CHAR(p.date, 'YYYY-MM')
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new MonthlyCountDto(
                (String) row[0],
                ((Number) row[1]).longValue()
            ))
            .collect(Collectors.toList());
    }

    private List<AgeGroupCountDto> getPatientsByAge() {
        String sql = """
            SELECT age_group, count
            FROM (
                SELECT
                    CASE
                        WHEN p.birth_date IS NULL THEN 'Desconocido'
                        WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 18 THEN '0-17'
                        WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 30 THEN '18-29'
                        WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 50 THEN '30-49'
                        WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 65 THEN '50-64'
                        ELSE '65+'
                    END as age_group,
                    COUNT(*) as count
                FROM patient p
                GROUP BY
                    CASE
                        WHEN p.birth_date IS NULL THEN 'Desconocido'
                        WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 18 THEN '0-17'
                        WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 30 THEN '18-29'
                        WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 50 THEN '30-49'
                        WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 65 THEN '50-64'
                        ELSE '65+'
                    END
            ) sub
            ORDER BY
                CASE
                    WHEN age_group = 'Desconocido' THEN 1
                    WHEN age_group = '0-17' THEN 2
                    WHEN age_group = '18-29' THEN 3
                    WHEN age_group = '30-49' THEN 4
                    WHEN age_group = '50-64' THEN 5
                    ELSE 6
                END
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new AgeGroupCountDto(
                (String) row[0],
                ((Number) row[1]).longValue()
            ))
            .collect(Collectors.toList());
    }

    private List<GenderCountDto> getPatientsBySex() {
        String sql = "SELECT p.sex, COUNT(*) FROM patient p GROUP BY p.sex";
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> {
                String sex = row[0] != null ? String.valueOf(row[0]) : "Desconocido";
                return new GenderCountDto(sex, ((Number) row[1]).longValue());
            })
            .collect(Collectors.toList());
    }

    private List<FonasaCountDto> getPatientsByFonasa() {
        String sql = "SELECT CAST(p.fonasa_tier AS VARCHAR) as fonasa_tier, COUNT(*) FROM patient p GROUP BY p.fonasa_tier";
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new FonasaCountDto(
                String.valueOf(row[0]),
                ((Number) row[1]).longValue()
            ))
            .collect(Collectors.toList());
    }

    private List<SectorCountDto> getPatientsBySector() {
        String sql = "SELECT CAST(p.residential_sector AS VARCHAR) as residential_sector, COUNT(*) FROM patient p GROUP BY p.residential_sector";
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new SectorCountDto(
                String.valueOf(row[0]),
                ((Number) row[1]).longValue()
            ))
            .collect(Collectors.toList());
    }

    private List<DiagnosisCountDto> getTopDiagnoses() {
        String sql = """
            SELECT COALESCE(d.description, c.name) as diagnosis_name, COUNT(*) as count
            FROM diagnosis d
            JOIN cie10 c ON d.cie10_code = c.code
            GROUP BY COALESCE(d.description, c.name)
            ORDER BY COUNT(*) DESC
            LIMIT 10
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new DiagnosisCountDto(
                (String) row[0],
                ((Number) row[1]).longValue()
            ))
            .collect(Collectors.toList());
    }

    private List<SpecialtyDiagnosisDto> getDiagnosisBySpecialty() {
        return getDiagnosesBySpecialty();
    }

    private List<MonthlyCountDto> getRespiratoryTrend() {
        // Tendencia de diagnósticos respiratorios
        String sql = """
            SELECT
                TO_CHAR(c.date, 'YYYY-MM') as month,
                COUNT(*) as count
            FROM consultation c
            JOIN diagnosis d ON c.id = d.consultation_id
            JOIN cie10 cie ON d.cie10_code = cie.code
            WHERE LOWER(COALESCE(d.description, cie.name)) LIKE '%respirat%'
            GROUP BY TO_CHAR(c.date, 'YYYY-MM')
            ORDER BY TO_CHAR(c.date, 'YYYY-MM')
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new MonthlyCountDto(
                (String) row[0],
                ((Number) row[1]).longValue()
            ))
            .collect(Collectors.toList());
    }

    private List<SpecialtyCountDto> getConsultationsBySpecialtyPrivate() {
        String sql = """
            SELECT COALESCE(hp.specialty, 'Sin especialidad') as specialty, COUNT(*) as count
            FROM consultation c
            LEFT JOIN healthcare_professional hp ON c.professional_id = hp.id
            GROUP BY COALESCE(hp.specialty, 'Sin especialidad')
            ORDER BY COUNT(*) DESC
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new SpecialtyCountDto(
                (String) row[0],
                ((Number) row[1]).longValue()
            ))
            .collect(Collectors.toList());
    }

    // ========== CHRONIC PATIENTS STATS ==========

    public ChronicSummaryDto getChronicSummary() {
        long totalPatients = getTotalPatients();

        // Pacientes con enfermedades crónicas (basado en diagnósticos frecuentes)
        String chronicCountSql = """
            SELECT COUNT(DISTINCT p.id)
            FROM patient p
            JOIN consultation c ON p.id = c.patient_id
            JOIN diagnosis d ON c.id = d.consultation_id
            JOIN cie10 cie ON d.cie10_code = cie.code
            WHERE LOWER(COALESCE(d.description, cie.name)) LIKE ANY (ARRAY[
                '%diabetes%', '%hipertens%', '%asma%', '%epoc%',
                '%artritis%', '%hipotiroidismo%', '%insuficiencia card%'
            ])
            """;
        Query chronicQuery = entityManager.createNativeQuery(chronicCountSql);
        Long totalCronicos = ((Number) chronicQuery.getSingleResult()).longValue();

        // Pacientes en tratamiento (con recetas en los últimos 3 meses)
        String treatmentCountSql = """
            SELECT COUNT(DISTINCT p.id)
            FROM patient p
            JOIN prescription pr ON p.id = pr.patient_id
            WHERE pr.date >= CURRENT_DATE - INTERVAL '3 months'
            """;
        Query treatmentQuery = entityManager.createNativeQuery(treatmentCountSql);
        Long totalEnTratamiento = ((Number) treatmentQuery.getSingleResult()).longValue();

        Double porcentajeCronicos = totalPatients > 0 ?
            (totalCronicos.doubleValue() / totalPatients) * 100 : 0.0;

        // Distribución por patología
        String distributionSql = """
            SELECT 
                CASE
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%diabetes%' THEN 'Diabetes'
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%hipertens%' THEN 'Hipertensión'
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%asma%' THEN 'Asma'
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%epoc%' THEN 'EPOC'
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%artritis%' THEN 'Artritis'
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%hipotiroidismo%' THEN 'Hipotiroidismo'
                    ELSE 'Otras crónicas'
                END as patologia,
                COUNT(DISTINCT p.id) as cantidad
            FROM patient p
            JOIN consultation c ON p.id = c.patient_id
            JOIN diagnosis d ON c.id = d.consultation_id
            JOIN cie10 cie ON d.cie10_code = cie.code
            WHERE LOWER(COALESCE(d.description, cie.name)) LIKE ANY (ARRAY[
                '%diabetes%', '%hipertens%', '%asma%', '%epoc%',
                '%artritis%', '%hipotiroidismo%', '%insuficiencia card%'
            ])
            GROUP BY 
                CASE
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%diabetes%' THEN 'Diabetes'
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%hipertens%' THEN 'Hipertensión'
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%asma%' THEN 'Asma'
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%epoc%' THEN 'EPOC'
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%artritis%' THEN 'Artritis'
                    WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%hipotiroidismo%' THEN 'Hipotiroidismo'
                    ELSE 'Otras crónicas'
                END
            ORDER BY cantidad DESC
            """;
        Query distributionQuery = entityManager.createNativeQuery(distributionSql);
        @SuppressWarnings("unchecked")
        List<Object[]> distributionResults = distributionQuery.getResultList();

        long totalDistribution = distributionResults.stream()
            .mapToLong(row -> ((Number) row[1]).longValue())
            .sum();

        List<PathologyDistributionDto> distribucion = distributionResults.stream()
            .map(row -> new PathologyDistributionDto(
                (String) row[0],
                ((Number) row[1]).longValue(),
                totalDistribution > 0 ?
                    (((Number) row[1]).doubleValue() / totalDistribution) * 100 : 0.0
            ))
            .collect(Collectors.toList());

        return new ChronicSummaryDto(
            totalCronicos,
            totalEnTratamiento,
            porcentajeCronicos,
            distribucion
        );
    }

    public List<ChronicPatientDto> getChronicPatients(int limit) {
        String sql = """
            SELECT DISTINCT
                p.name as patient_name,
                p.id as patient_id,
                MAX(c.date) as last_consultation
            FROM patient p
            JOIN consultation c ON p.id = c.patient_id
            JOIN diagnosis d ON c.id = d.consultation_id
            JOIN cie10 cie ON d.cie10_code = cie.code
            WHERE LOWER(COALESCE(d.description, cie.name)) LIKE ANY (ARRAY[
                '%diabetes%', '%hipertens%', '%asma%', '%epoc%',
                '%artritis%', '%hipotiroidismo%', '%insuficiencia card%'
            ])
            GROUP BY p.name, p.id
            ORDER BY MAX(c.date) DESC
            LIMIT :limit
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> {
                String patientName = (String) row[0];
                Long patientId = ((Number) row[1]).longValue();
                java.sql.Date lastConsultationDate = (java.sql.Date) row[2];

                // Obtener patologías del paciente
                String pathologiesSql = """
                    SELECT DISTINCT
                        CASE
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%diabetes%' THEN 'Diabetes'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%hipertens%' THEN 'Hipertensión'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%asma%' THEN 'Asma'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%epoc%' THEN 'EPOC'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%artritis%' THEN 'Artritis'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%hipotiroidismo%' THEN 'Hipotiroidismo'
                            ELSE 'Otra crónica'
                        END as patologia
                    FROM diagnosis d
                    JOIN consultation c ON d.consultation_id = c.id
                    JOIN cie10 cie ON d.cie10_code = cie.code
                    WHERE c.patient_id = :patientId
                    AND LOWER(COALESCE(d.description, cie.name)) LIKE ANY (ARRAY[
                        '%diabetes%', '%hipertens%', '%asma%', '%epoc%',
                        '%artritis%', '%hipotiroidismo%', '%insuficiencia card%'
                    ])
                    """;
                Query pathologiesQuery = entityManager.createNativeQuery(pathologiesSql);
                pathologiesQuery.setParameter("patientId", patientId);
                @SuppressWarnings("unchecked")
                List<String> patologias = pathologiesQuery.getResultList();

                return new ChronicPatientDto(
                    patientName,
                    patologias,
                    lastConsultationDate.toLocalDate()
                );
            })
            .collect(Collectors.toList());
    }

    public List<PrescriptionDetailDto> getPrescriptionDetails(int limit) {
        String sql = """
            SELECT
                pr.id,
                pr.date,
                p.name as patient_name,
                hp.name as professional_name
            FROM prescription pr
            JOIN patient p ON pr.patient_id = p.id
            JOIN consultation c ON pr.consultation_id = c.id
            LEFT JOIN healthcare_professional hp ON c.professional_id = hp.id
            ORDER BY pr.date DESC
            LIMIT :limit
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> {
                Long prescriptionId = ((Number) row[0]).longValue();
                java.sql.Date sqlDate = (java.sql.Date) row[1];
                LocalDate fecha = sqlDate.toLocalDate();
                String patientName = (String) row[2];
                String professionalName = row[3] != null ? (String) row[3] : "Sin información";

                // Obtener medicamentos de la receta
                String medicationsSql = """
                    SELECT m.name
                    FROM prescription_medication pm
                    JOIN medication m ON pm.medication_id = m.id
                    WHERE pm.prescription_id = :prescriptionId
                    """;
                Query medicationsQuery = entityManager.createNativeQuery(medicationsSql);
                medicationsQuery.setParameter("prescriptionId", prescriptionId);
                @SuppressWarnings("unchecked")
                List<String> medications = medicationsQuery.getResultList();

                return new PrescriptionDetailDto(
                    prescriptionId,
                    fecha,
                    patientName,
                    medications,
                    professionalName
                );
            })
            .collect(Collectors.toList());
    }

    public List<EmergingDiagnosisDto> getEmergingDiagnoses() {
        String sql = """
            WITH current_month_stats AS (
                SELECT
                    COALESCE(d.description, cie.name) as diagnosis_name,
                    COUNT(*) as count
                FROM diagnosis d
                JOIN cie10 cie ON d.cie10_code = cie.code
                JOIN consultation c ON d.consultation_id = c.id
                WHERE TO_CHAR(c.date, 'YYYY-MM') = TO_CHAR(CURRENT_DATE, 'YYYY-MM')
                GROUP BY COALESCE(d.description, cie.name)
            ),
            previous_month_stats AS (
                SELECT
                    COALESCE(d.description, cie.name) as diagnosis_name,
                    COUNT(*) as count
                FROM diagnosis d
                JOIN cie10 cie ON d.cie10_code = cie.code
                JOIN consultation c ON d.consultation_id = c.id
                WHERE TO_CHAR(c.date, 'YYYY-MM') = TO_CHAR(CURRENT_DATE - INTERVAL '1 month', 'YYYY-MM')
                GROUP BY COALESCE(d.description, cie.name)
            )
            SELECT
                COALESCE(cm.diagnosis_name, pm.diagnosis_name) as diagnosis,
                COALESCE(cm.count, 0) as current_count,
                COALESCE(pm.count, 0) as previous_count,
                CASE
                    WHEN COALESCE(pm.count, 0) = 0 THEN
                        CASE WHEN COALESCE(cm.count, 0) > 0 THEN 100.0 ELSE 0.0 END
                    ELSE
                        ((COALESCE(cm.count, 0)::FLOAT - pm.count) / pm.count) * 100
                END as variation
            FROM current_month_stats cm
            FULL OUTER JOIN previous_month_stats pm ON cm.diagnosis_name = pm.diagnosis_name
            WHERE COALESCE(cm.count, 0) > 0
            ORDER BY variation DESC
            LIMIT 20
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new EmergingDiagnosisDto(
                (String) row[0],
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                ((Number) row[3]).doubleValue()
            ))
            .collect(Collectors.toList());
    }

    public List<DiseaseByFonasaDto> getDiseasesByFonasa() {
        String sql = """
            SELECT
                COALESCE(CAST(p.fonasa_tier AS VARCHAR), 'Sin información') as fonasa,
                COALESCE(d.description, cie.name) as disease,
                COUNT(*) as count
            FROM diagnosis d
            JOIN cie10 cie ON d.cie10_code = cie.code
            JOIN consultation c ON d.consultation_id = c.id
            JOIN patient p ON c.patient_id = p.id
            GROUP BY p.fonasa_tier, COALESCE(d.description, cie.name)
            ORDER BY COUNT(*) DESC
            LIMIT 50
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new DiseaseByFonasaDto(
                (String) row[0],
                (String) row[1],
                ((Number) row[2]).longValue()
            ))
            .collect(Collectors.toList());
    }

    public List<MedicationByAgeGroupDto> getMedicationsByAgeGroup() {
        String sql = """
            SELECT
                CASE
                    WHEN p.birth_date IS NULL THEN 'Desconocido'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 18 THEN '0-17'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 30 THEN '18-29'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 50 THEN '30-49'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 65 THEN '50-64'
                    ELSE '65+'
                END as age_group,
                m.name as medication,
                COUNT(*) as count
            FROM prescription pr
            JOIN patient p ON pr.patient_id = p.id
            JOIN prescription_medication pm ON pr.id = pm.prescription_id
            JOIN medication m ON pm.medication_id = m.id
            GROUP BY
                CASE
                    WHEN p.birth_date IS NULL THEN 'Desconocido'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 18 THEN '0-17'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 30 THEN '18-29'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 50 THEN '30-49'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 65 THEN '50-64'
                    ELSE '65+'
                END,
                m.name
            ORDER BY COUNT(*) DESC
            LIMIT 100
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new MedicationByAgeGroupDto(
                (String) row[0],
                (String) row[1],
                ((Number) row[2]).longValue()
            ))
            .collect(Collectors.toList());
    }

    public List<DiagnosisBySexDto> getDiagnosesBySex() {
        String sql = """
            SELECT
                COALESCE(d.description, cie.name) as diagnosis,
                COUNT(CASE WHEN p.sex = 'M' THEN 1 END) as male_count,
                COUNT(CASE WHEN p.sex = 'F' THEN 1 END) as female_count
            FROM diagnosis d
            JOIN cie10 cie ON d.cie10_code = cie.code
            JOIN consultation c ON d.consultation_id = c.id
            JOIN patient p ON c.patient_id = p.id
            GROUP BY COALESCE(d.description, cie.name)
            ORDER BY (COUNT(CASE WHEN p.sex = 'M' THEN 1 END) + COUNT(CASE WHEN p.sex = 'F' THEN 1 END)) DESC
            LIMIT 20
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new DiagnosisBySexDto(
                (String) row[0],
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue()
            ))
            .collect(Collectors.toList());
    }

    public List<DiseaseByFonasaDto> getDiagnosesByFonasa() {
        String sql = """
            SELECT
                COALESCE(CAST(p.fonasa_tier AS VARCHAR), 'Sin información') as fonasa,
                COALESCE(d.description, cie.name) as diagnosis,
                COUNT(*) as count
            FROM diagnosis d
            JOIN cie10 cie ON d.cie10_code = cie.code
            JOIN consultation c ON d.consultation_id = c.id
            JOIN patient p ON c.patient_id = p.id
            GROUP BY p.fonasa_tier, COALESCE(d.description, cie.name)
            ORDER BY COUNT(*) DESC
            LIMIT 50
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new DiseaseByFonasaDto(
                (String) row[0],
                (String) row[1],
                ((Number) row[2]).longValue()
            ))
            .collect(Collectors.toList());
    }

    public List<SpecialtyDiagnosisDto> getDiagnosesBySpecialty() {
        String sql = """
            SELECT
                COALESCE(hp.specialty, 'Sin especialidad') as specialty,
                COALESCE(d.description, cie.name) as diagnosis,
                COUNT(*) as count
            FROM diagnosis d
            JOIN cie10 cie ON d.cie10_code = cie.code
            JOIN consultation c ON d.consultation_id = c.id
            LEFT JOIN healthcare_professional hp ON c.professional_id = hp.id
            GROUP BY hp.specialty, COALESCE(d.description, cie.name)
            ORDER BY COUNT(*) DESC
            LIMIT 50
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new SpecialtyDiagnosisDto(
                (String) row[0],
                (String) row[1],
                ((Number) row[2]).longValue()
            ))
            .collect(Collectors.toList());
    }

    public List<DiseaseByAgeGroupDto> getDiseasesByAgeGroup() {
        String sql = """
            SELECT
                CASE
                    WHEN p.birth_date IS NULL THEN 'Desconocido'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 18 THEN '0-17'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 30 THEN '18-29'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 50 THEN '30-49'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 65 THEN '50-64'
                    ELSE '65+'
                END as age_group,
                COALESCE(d.description, cie.name) as disease,
                COUNT(*) as count
            FROM diagnosis d
            JOIN cie10 cie ON d.cie10_code = cie.code
            JOIN consultation c ON d.consultation_id = c.id
            JOIN patient p ON c.patient_id = p.id
            GROUP BY
                CASE
                    WHEN p.birth_date IS NULL THEN 'Desconocido'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 18 THEN '0-17'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 30 THEN '18-29'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 50 THEN '30-49'
                    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) < 65 THEN '50-64'
                    ELSE '65+'
                END,
                COALESCE(d.description, cie.name)
            ORDER BY COUNT(*) DESC
            LIMIT 100
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new DiseaseByAgeGroupDto(
                (String) row[0],
                (String) row[1],
                ((Number) row[2]).longValue()
            ))
            .collect(Collectors.toList());
    }

    public List<MedicationForecastDto> getMedicationForecast() {
        // Obtener uso de medicamentos en los últimos 3 meses vs 3 meses anteriores
        String sql = """
            WITH recent_usage AS (
                SELECT
                    m.id,
                    m.name,
                    COUNT(*) as recent_count
                FROM medication m
                JOIN prescription_medication pm ON m.id = pm.medication_id
                JOIN prescription p ON pm.prescription_id = p.id
                WHERE p.date >= CURRENT_DATE - INTERVAL '3 months'
                GROUP BY m.id, m.name
            ),
            previous_usage AS (
                SELECT
                    m.id,
                    m.name,
                    COUNT(*) as previous_count
                FROM medication m
                JOIN prescription_medication pm ON m.id = pm.medication_id
                JOIN prescription p ON pm.prescription_id = p.id
                WHERE p.date >= CURRENT_DATE - INTERVAL '6 months'
                  AND p.date < CURRENT_DATE - INTERVAL '3 months'
                GROUP BY m.id, m.name
            )
            SELECT
                COALESCE(r.name, p.name) as medication_name,
                COALESCE(r.recent_count, 0) as current_stock,
                COALESCE(p.previous_count, 0) as previous_count,
                COALESCE(r.recent_count, 0) as recent_count
            FROM recent_usage r
            FULL OUTER JOIN previous_usage p ON r.id = p.id
            WHERE COALESCE(r.recent_count, 0) > 0 OR COALESCE(p.previous_count, 0) > 0
            ORDER BY COALESCE(r.recent_count, 0) DESC
            LIMIT 20
            """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> {
                String medicationName = (String) row[0];
                Long currentStock = ((Number) row[1]).longValue();
                Long previousCount = ((Number) row[2]).longValue();
                Long recentCount = ((Number) row[3]).longValue();

                // Calcular tasa de crecimiento
                Double growthRate = 0.0;
                if (previousCount > 0) {
                    growthRate = ((recentCount - previousCount) * 100.0) / previousCount;
                }

                // Predicción simple: uso reciente + (uso reciente * tasa de crecimiento)
                Long predictedDemand = Math.round(recentCount * (1 + (growthRate / 100)));

                // Stock recomendado: demanda predicha + 20% de margen de seguridad
                Long recommendedStock = Math.round(predictedDemand * 1.2);

                return new MedicationForecastDto(
                    medicationName,
                    currentStock,
                    predictedDemand,
                    recommendedStock,
                    growthRate
                );
            })
            .collect(Collectors.toList());
    }
}
