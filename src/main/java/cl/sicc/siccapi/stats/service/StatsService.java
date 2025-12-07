package cl.sicc.siccapi.stats.service;

import cl.sicc.siccapi.stats.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    @PersistenceContext
    private EntityManager entityManager;

    public DashboardStatsDto getDashboardStats() {
        System.out.println("🔷 [STATS-SERVICE] getDashboardStats - Iniciando...");

        long totalConsultations = getTotalConsultations();
        System.out.println("📊 [STATS-SERVICE] Total consultas: " + totalConsultations);

        long totalPatients = getTotalPatients();
        System.out.println("📊 [STATS-SERVICE] Total pacientes: " + totalPatients);

        long totalPrescriptions = getTotalPrescriptions();
        System.out.println("📊 [STATS-SERVICE] Total recetas: " + totalPrescriptions);

        double averageConsultationsPerDay = totalConsultations > 0 ? (double) totalConsultations / 30 : 0; // Aproximado

        Map<String, Long> consultationsByType = getConsultationsByType();
        System.out.println("📊 [STATS-SERVICE] Consultas por tipo: " + consultationsByType);

        Map<String, Long> consultationsByMonth = getConsultationsByMonth();
        System.out.println("📊 [STATS-SERVICE] Consultas por mes (últimos meses): " + consultationsByMonth.size() + " registros");
        consultationsByMonth.forEach((mes, cantidad) ->
            System.out.println("  📅 " + mes + ": " + cantidad + " consultas")
        );

        DashboardStatsDto dto = new DashboardStatsDto(
                totalConsultations,
                totalPatients,
                totalPrescriptions,
                averageConsultationsPerDay,
                consultationsByType,
                consultationsByMonth);

        System.out.println("✅ [STATS-SERVICE] getDashboardStats - Completado");
        return dto;
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
                    EXTRACT(DOW FROM c.date) as dayOfWeek,
                    COUNT(CASE WHEN UPPER(c.type) IN ('URGENCIA', 'URGENT') THEN 1 END) as urgentCount,
                    COUNT(CASE WHEN UPPER(c.type) NOT IN ('URGENCIA', 'URGENT') OR c.type IS NULL THEN 1 END) as generalCount
                FROM consultation c
                WHERE c.date >= (SELECT MAX(date) - INTERVAL '7 days' FROM consultation)
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
                        ((Number) row[2]).intValue(),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()))
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

    public long getTotalConsultations() {
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
                        row -> ((Number) row[1]).longValue()));
    }

    public Map<String, Long> getConsultationsByMonth() {
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
                        row -> ((Number) row[1]).longValue()));
    }

    public List<MedicationCountDto> getTopMedications() {
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
                        ((Number) row[1]).longValue()))
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
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private List<AgeGroupCountDto> getPatientsByAge() {
        String sql = """
                SELECT age_group, count
                FROM (
                    SELECT
                        CASE
                            WHEN p.birth_date IS NULL THEN 'Desconocido'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 18 THEN '0-17'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 30 THEN '18-29'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 50 THEN '30-49'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 65 THEN '50-64'
                            ELSE '65+'
                        END as age_group,
                        COUNT(*) as count
                    FROM patient p
                    GROUP BY
                        CASE
                            WHEN p.birth_date IS NULL THEN 'Desconocido'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 18 THEN '0-17'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 30 THEN '18-29'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 50 THEN '30-49'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 65 THEN '50-64'
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
                        ((Number) row[1]).longValue()))
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

    public List<FonasaCountDto> getPatientsByFonasa() {
        String sql = "SELECT CAST(p.fonasa_tier AS VARCHAR) as fonasa_tier, COUNT(*) FROM patient p GROUP BY p.fonasa_tier";
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new FonasaCountDto(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue()))
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
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    /**
     * Distribución de pacientes por grupo de edad y sexo
     * Resuelve el problema en pacientes-perfil.component.ts
     */
    public List<AgeGroupBySexDto> getPatientsByAgeAndSex() {
        String sql = """
                SELECT
                    age_group,
                    SUM(CASE WHEN sex = 'M' THEN 1 ELSE 0 END) as hombres,
                    SUM(CASE WHEN sex = 'F' THEN 1 ELSE 0 END) as mujeres,
                    COUNT(*) as total
                FROM (
                    SELECT
                        CASE
                            WHEN p.birth_date IS NULL THEN 'Desconocido'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 11 THEN '0-10'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 21 THEN '11-20'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 31 THEN '21-30'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 41 THEN '31-40'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 51 THEN '41-50'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 61 THEN '51-60'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 71 THEN '61-70'
                            WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 81 THEN '71-80'
                            ELSE '81+'
                        END as age_group,
                        p.sex
                    FROM patient p
                ) sub
                GROUP BY age_group
                ORDER BY
                    CASE age_group
                        WHEN '0-10' THEN 1
                        WHEN '11-20' THEN 2
                        WHEN '21-30' THEN 3
                        WHEN '31-40' THEN 4
                        WHEN '41-50' THEN 5
                        WHEN '51-60' THEN 6
                        WHEN '61-70' THEN 7
                        WHEN '71-80' THEN 8
                        WHEN '81+' THEN 9
                        ELSE 10
                    END
                """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        long grandTotal = results.stream()
                .mapToLong(row -> ((Number) row[3]).longValue())
                .sum();

        return results.stream()
                .map(row -> new AgeGroupBySexDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        grandTotal > 0 ? (((Number) row[3]).doubleValue() / grandTotal) * 100 : 0.0))
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
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private List<SpecialtyDiagnosisDto> getDiagnosisBySpecialty() {
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

        // Agrupar por especialidad
        Map<String, List<DiagnosisCountDto>> specialtyMap = new HashMap<>();
        for (Object[] row : results) {
            String specialty = (String) row[0];
            String diagnosis = (String) row[1];
            Long count = ((Number) row[2]).longValue();

            specialtyMap.computeIfAbsent(specialty, k -> new ArrayList<>())
                    .add(new DiagnosisCountDto(diagnosis, count));
        }

        return specialtyMap.entrySet().stream()
                .map(entry -> new SpecialtyDiagnosisDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
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
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    public List<SpecialtyCountDto> getConsultationsBySpecialtyPrivate() {
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
                        ((Number) row[1]).longValue()))
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
        // prescription -> consultation -> patient
        String treatmentCountSql = """
                SELECT COUNT(DISTINCT p.id)
                FROM patient p
                JOIN consultation c ON p.id = c.patient_id
                JOIN prescription pr ON c.id = pr.consultation_id
                WHERE pr.date >= (SELECT MAX(date) - INTERVAL '3 months' FROM consultation)
                """;
        Query treatmentQuery = entityManager.createNativeQuery(treatmentCountSql);
        Long totalEnTratamiento = ((Number) treatmentQuery.getSingleResult()).longValue();

        Double porcentajeCronicos = totalPatients > 0 ? (totalCronicos.doubleValue() / totalPatients) * 100 : 0.0;

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
                        totalDistribution > 0 ? (((Number) row[1]).doubleValue() / totalDistribution) * 100 : 0.0))
                .collect(Collectors.toList());

        return new ChronicSummaryDto(
                totalCronicos,
                totalEnTratamiento,
                porcentajeCronicos,
                distribucion);
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
                            lastConsultationDate.toLocalDate());
                })
                .collect(Collectors.toList());
    }

    public List<PrescriptionDetailDto> getPrescriptionDetails(int limit) {
        String sql = """
                SELECT
                    pr.id,
                    pr.date,
                    p.name as patient_name,
                    hp.name as professional_name,
                    p.rut,
                    EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date))::INTEGER as age,
                    p.fonasa_tier,
                    c.type,
                    (SELECT d.description FROM diagnosis d WHERE d.consultation_id = c.id LIMIT 1) as diagnosis
                FROM prescription pr
                JOIN consultation c ON pr.consultation_id = c.id
                JOIN patient p ON c.patient_id = p.id
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
                    String rut = (String) row[4];
                    Integer age = row[5] != null ? ((Number) row[5]).intValue() : null;
                    String fonasa = row[6] != null ? row[6].toString() : null;
                    String type = row[7] != null ? row[7].toString() : null;
                    String diagnosis = row[8] != null ? (String) row[8] : null;

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
                            professionalName,
                            rut,
                            age,
                            fonasa,
                            diagnosis,
                            type);
                })
                .collect(Collectors.toList());
    }

    public List<EmergingDiagnosisDto> getEmergingDiagnoses() {
        System.out.println("🔷 [STATS-SERVICE] getEmergingDiagnoses - Iniciando...");

        String sql = """
                WITH current_month_stats AS (
                    SELECT
                        COALESCE(d.description, cie.name) as diagnosis_name,
                        COUNT(*) as count
                    FROM diagnosis d
                    JOIN cie10 cie ON d.cie10_code = cie.code
                    JOIN consultation c ON d.consultation_id = c.id
                    WHERE TO_CHAR(c.date, 'YYYY-MM') = TO_CHAR((SELECT MAX(date) FROM consultation), 'YYYY-MM')
                    GROUP BY COALESCE(d.description, cie.name)
                ),
                previous_month_stats AS (
                    SELECT
                        COALESCE(d.description, cie.name) as diagnosis_name,
                        COUNT(*) as count
                    FROM diagnosis d
                    JOIN cie10 cie ON d.cie10_code = cie.code
                    JOIN consultation c ON d.consultation_id = c.id
                    WHERE TO_CHAR(c.date, 'YYYY-MM') = TO_CHAR((SELECT MAX(date) - INTERVAL '1 month' FROM consultation), 'YYYY-MM')
                    GROUP BY COALESCE(d.description, cie.name)
                )
                SELECT
                    COALESCE(cm.diagnosis_name, pm.diagnosis_name) as diagnosis_name,
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

        System.out.println("✅ [STATS-SERVICE] getEmergingDiagnoses - Resultados: " + results.size() + " diagnósticos");

        return results.stream()
                .map(row -> new EmergingDiagnosisDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).doubleValue()))
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
                        ((Number) row[2]).longValue()))
                .collect(Collectors.toList());
    }

    public List<MedicationByAgeGroupDto> getMedicationsByAgeGroup() {
        String sql = """
                SELECT
                    CASE
                        WHEN p.birth_date IS NULL THEN 'Desconocido'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 18 THEN '0-17'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 30 THEN '18-29'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 50 THEN '30-49'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 65 THEN '50-64'
                        ELSE '65+'
                    END as age_group,
                    m.name as medication,
                    COUNT(*) as count
                FROM prescription pr
                JOIN consultation c ON pr.consultation_id = c.id
                JOIN patient p ON c.patient_id = p.id
                JOIN prescription_medication pm ON pr.id = pm.prescription_id
                JOIN medication m ON pm.medication_id = m.id
                GROUP BY
                    CASE
                        WHEN p.birth_date IS NULL THEN 'Desconocido'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 18 THEN '0-17'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 30 THEN '18-29'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 50 THEN '30-49'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 65 THEN '50-64'
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
                        ((Number) row[2]).longValue()))
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
                        ((Number) row[2]).longValue()))
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
                        ((Number) row[2]).longValue()))
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

        // Agrupar por especialidad
        Map<String, List<DiagnosisCountDto>> specialtyMap = new HashMap<>();
        for (Object[] row : results) {
            String specialty = (String) row[0];
            String diagnosis = (String) row[1];
            Long count = ((Number) row[2]).longValue();

            specialtyMap.computeIfAbsent(specialty, k -> new ArrayList<>())
                    .add(new DiagnosisCountDto(diagnosis, count));
        }

        return specialtyMap.entrySet().stream()
                .map(entry -> new SpecialtyDiagnosisDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public List<DiseaseByAgeGroupDto> getDiseasesByAgeGroup() {
        String sql = """
                SELECT
                    CASE
                        WHEN p.birth_date IS NULL THEN 'Desconocido'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 18 THEN '0-17'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 30 THEN '18-29'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 50 THEN '30-49'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 65 THEN '50-64'
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
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 18 THEN '0-17'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 30 THEN '18-29'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 50 THEN '30-49'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 65 THEN '50-64'
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
                        ((Number) row[2]).longValue()))
                .collect(Collectors.toList());
    }

    public List<MedicationForecastDto> getMedicationForecast() {
        System.out.println("🔷 [STATS-SERVICE] getMedicationForecast - Iniciando...");

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
                    WHERE p.date >= (SELECT MAX(date) - INTERVAL '3 months' FROM consultation)
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
                    WHERE p.date >= (SELECT MAX(date) - INTERVAL '6 months' FROM consultation)
                      AND p.date < (SELECT MAX(date) - INTERVAL '3 months' FROM consultation)
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

        System.out.println("📊 [STATS-SERVICE] Resultados de consulta: " + results.size() + " medicamentos");

        List<MedicationForecastDto> forecasts = results.stream()
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
                            growthRate);
                })
                .collect(Collectors.toList());

        System.out.println("✅ [STATS-SERVICE] getMedicationForecast - Completado con " + forecasts.size() + " predicciones");

        return forecasts;
    }

    // ========== DEMAND ANALYSIS METHODS ==========

    public List<DailyConsultationDto> getDailyConsultations(int days) {
        // Calcular capacidad diaria base: Total Profesionales * 12 cupos/día
        int dailyCapacity = 60; // Default (5 * 12)
        try {
            Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) FROM healthcare_professional");
            long totalProfessionals = ((Number) countQuery.getSingleResult()).longValue();
            if (totalProfessionals > 0) {
                dailyCapacity = (int) (totalProfessionals * 12);
            }
        } catch (Exception e) {
            // Fallback silencioso
        }

        String sql = """
                SELECT
                    c.date,
                    COUNT(*) as total_consultations,
                    COUNT(CASE WHEN EXTRACT(HOUR FROM c.date) < 13 THEN 1 END) as morning_consultations,
                    COUNT(CASE WHEN EXTRACT(HOUR FROM c.date) >= 13 THEN 1 END) as afternoon_consultations,
                    COUNT(CASE WHEN c.type = 'Urgencia' THEN 1 END) as emergency_consultations
                FROM consultation c
                WHERE c.date >= CURRENT_DATE - INTERVAL '%d days'
                GROUP BY c.date
                ORDER BY c.date DESC
                """.formatted(days);
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        final int capacityFinal = dailyCapacity;

        return results.stream()
                .map(row -> new DailyConsultationDto(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue(),
                        ((Number) row[3]).intValue(),
                        ((Number) row[4]).intValue(),
                        capacityFinal))
                .collect(Collectors.toList());
    }

    public List<ProfessionalWorkloadDto> getProfessionalWorkload() {
        System.out.println("🔷 [STATS-SERVICE] getProfessionalWorkload - Iniciando consulta...");

        String sql = """
                SELECT
                    COALESCE(hp.name, 'Sin nombre') as professional_name,
                    COALESCE(hp.specialty, 'Sin especialidad') as specialty,
                    COUNT(c.id) as total_consultations,
                    ROUND(COUNT(c.id)::numeric / 30, 1) as average_per_day,
                    COUNT(CASE WHEN c.date = (SELECT MAX(date) FROM consultation) THEN 1 END) as patients_today,
                    0 as pending_appointments -- TODO: implementar cuando haya tabla de citas
                FROM healthcare_professional hp
                LEFT JOIN consultation c ON hp.id = c.professional_id
                    AND c.date >= (SELECT MAX(date) - INTERVAL '30 days' FROM consultation)
                GROUP BY hp.id, hp.name, hp.specialty
                HAVING COUNT(c.id) > 0
                ORDER BY total_consultations DESC
                """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        System.out.println("✅ [STATS-SERVICE] getProfessionalWorkload - Resultados obtenidos: " + results.size() + " registros");

        List<ProfessionalWorkloadDto> dtos = results.stream()
                .map(row -> {
                    ProfessionalWorkloadDto dto = new ProfessionalWorkloadDto(
                            (String) row[0],
                            (String) row[1],
                            ((Number) row[2]).intValue(),
                            ((Number) row[3]).doubleValue(),
                            ((Number) row[4]).intValue(),
                            ((Number) row[5]).intValue());
                    System.out.println("📊 [STATS-SERVICE] Profesional: " + dto.getProfessionalName() +
                            ", Consultas: " + dto.getTotalConsultations());
                    return dto;
                })
                .collect(Collectors.toList());

        System.out.println("🔚 [STATS-SERVICE] getProfessionalWorkload - Devolviendo " + dtos.size() + " DTOs");
        return dtos;
    }

    public List<SpecialtyWorkloadDto> getSpecialtyWorkload() {
        System.out.println("🔷 [STATS-SERVICE] getSpecialtyWorkload - Iniciando consulta...");

        String sql = """
                SELECT
                    COALESCE(hp.specialty, 'Sin especialidad') as specialty,
                    COUNT(DISTINCT hp.id) as total_professionals,
                    COUNT(c.id) as total_consultations,
                    ROUND(COUNT(c.id)::numeric / NULLIF(COUNT(DISTINCT hp.id), 0), 1) as average_per_professional,
                    CASE
                        WHEN COUNT(c.id)::numeric / NULLIF(COUNT(DISTINCT hp.id), 0) > 25 THEN 95
                        WHEN COUNT(c.id)::numeric / NULLIF(COUNT(DISTINCT hp.id), 0) > 20 THEN 85
                        WHEN COUNT(c.id)::numeric / COUNT(DISTINCT hp.id) > 15 THEN 75
                        ELSE 60
                    END as capacity_utilization,
                    CASE
                        WHEN COUNT(c.id)::numeric / COUNT(DISTINCT hp.id) > 25 THEN 'Sobrecargado'
                        WHEN COUNT(c.id)::numeric / COUNT(DISTINCT hp.id) > 20 THEN 'Alta demanda'
                        ELSE 'Normal'
                    END as status
                FROM healthcare_professional hp
                LEFT JOIN consultation c ON hp.id = c.professional_id
                    AND c.date >= (SELECT MAX(date) - INTERVAL '30 days' FROM consultation)
                GROUP BY hp.specialty
                ORDER BY total_consultations DESC
                """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        System.out.println("✅ [STATS-SERVICE] getSpecialtyWorkload - Resultados obtenidos: " + results.size() + " registros");

        List<SpecialtyWorkloadDto> dtos = results.stream()
                .map(row -> {
                    SpecialtyWorkloadDto dto = new SpecialtyWorkloadDto(
                            (String) row[0],
                            ((Number) row[1]).intValue(),
                            ((Number) row[2]).intValue(),
                            ((Number) row[3]).doubleValue(),
                            ((Number) row[4]).intValue(),
                            (String) row[5]);
                    System.out.println("📊 [STATS-SERVICE] Especialidad: " + dto.getSpecialty() +
                            ", Profesionales: " + dto.getTotalProfessionals() +
                            ", Consultas: " + dto.getTotalConsultations());
                    return dto;
                })
                .collect(Collectors.toList());

        System.out.println("🔚 [STATS-SERVICE] getSpecialtyWorkload - Devolviendo " + dtos.size() + " DTOs");
        return dtos;
    }

    /**
     * Calcula la confianza de predicción basada en la varianza de los datos.
     * Menor varianza = mayor confianza
     */
    private double calcularConfianzaPrediccion(Map<String, Integer> predicciones) {
        if (predicciones.isEmpty())
            return 0.5;

        double[] valores = predicciones.values().stream().mapToDouble(Integer::doubleValue).toArray();
        double promedio = Arrays.stream(valores).average().orElse(0);

        if (promedio == 0)
            return 0.5;

        // Calcular coeficiente de variación (CV)
        double varianza = Arrays.stream(valores)
                .map(v -> Math.pow(v - promedio, 2))
                .average()
                .orElse(0);
        double desviacionEstandar = Math.sqrt(varianza);
        double coeficienteVariacion = desviacionEstandar / promedio;

        // Convertir CV a confianza: menor CV = mayor confianza
        // CV de 0 = confianza 0.95, CV de 1+ = confianza 0.5
        double confianza = Math.max(0.5, Math.min(0.95, 0.95 - (coeficienteVariacion * 0.45)));
        return Math.round(confianza * 100) / 100.0; // Redondear a 2 decimales
    }

    /**
     * Calcula patrones estacionales basados en datos históricos de consultas por
     * estación
     */
    private Map<String, Integer> calcularPatronesEstacionales() {
        String sql = """
                SELECT
                    CASE
                        WHEN EXTRACT(MONTH FROM date) IN (6, 7, 8) THEN 'Invierno'
                        WHEN EXTRACT(MONTH FROM date) IN (9, 10, 11) THEN 'Primavera'
                        WHEN EXTRACT(MONTH FROM date) IN (12, 1, 2) THEN 'Verano'
                        ELSE 'Otoño'
                    END as estacion,
                    COUNT(*) as consultas
                FROM consultation
                WHERE date >= (SELECT MAX(date) - INTERVAL '1 year' FROM consultation)
                GROUP BY
                    CASE
                        WHEN EXTRACT(MONTH FROM date) IN (6, 7, 8) THEN 'Invierno'
                        WHEN EXTRACT(MONTH FROM date) IN (9, 10, 11) THEN 'Primavera'
                        WHEN EXTRACT(MONTH FROM date) IN (12, 1, 2) THEN 'Verano'
                        ELSE 'Otoño'
                    END
                """;

        try {
            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            if (results.isEmpty()) {
                // Retornar valores por defecto si no hay datos
                return Map.of("Invierno", 80, "Primavera", 70, "Verano", 60, "Otoño", 75);
            }

            // Encontrar el máximo para normalizar a porcentaje
            long maxConsultas = results.stream()
                    .mapToLong(row -> ((Number) row[1]).longValue())
                    .max()
                    .orElse(1);

            Map<String, Integer> patrones = new HashMap<>();
            for (Object[] row : results) {
                String estacion = (String) row[0];
                long consultas = ((Number) row[1]).longValue();
                // Normalizar a porcentaje relativo al máximo (máximo = 100)
                int porcentaje = (int) Math.round((consultas * 100.0) / maxConsultas);
                patrones.put(estacion, porcentaje);
            }
            return patrones;
        } catch (Exception e) {
            // En caso de error, retornar valores por defecto
            return Map.of("Invierno", 80, "Primavera", 70, "Verano", 60, "Otoño", 75);
        }
    }

    /**
     * Calcula la confianza del modelo basada en la cantidad de datos históricos
     * disponibles
     */
    private double calcularConfianzaModelo(int cantidadDatosHistoricos) {
        // Más datos históricos = mayor confianza
        // 0 meses = 0.5, 6+ meses = 0.9
        if (cantidadDatosHistoricos <= 0)
            return 0.5;
        double confianza = Math.min(0.9, 0.5 + (cantidadDatosHistoricos * 0.067));
        return Math.round(confianza * 100) / 100.0;
    }

    /**
     * Genera insights dinámicos basados en tendencias y datos de la BD
     */
    private List<String> generarInsightsDinamicos(Map<String, Double> tendencias) {
        List<String> insights = new ArrayList<>();

        // Insight basado en crecimiento mensual
        Double crecimiento = tendencias.get("Crecimiento mensual");
        if (crecimiento != null) {
            if (crecimiento > 10) {
                insights.add(String.format("Demanda creciente: %.1f%% de aumento en el último mes", crecimiento));
            } else if (crecimiento < -5) {
                insights.add(String.format("Disminución de demanda: %.1f%% en el último mes", Math.abs(crecimiento)));
            } else {
                insights.add("Demanda estable en el último mes");
            }
        }

        // Insight basado en especialidades más consultadas
        try {
            List<SpecialtyCountDto> especialidades = getConsultationsBySpecialtyPrivate();
            if (!especialidades.isEmpty()) {
                String topEspecialidad = especialidades.get(0).specialty();
                insights.add("Mayor demanda en: " + topEspecialidad);
            }
        } catch (Exception e) {
            // Ignorar si falla la consulta
        }

        // Si no hay insights generados, agregar uno por defecto
        if (insights.isEmpty()) {
            insights.add("Análisis de datos en proceso");
        }

        return insights;
    }

    /**
     * Obtiene predicciones de demanda basadas en datos históricos
     */
    public DemandPredictionDto getDemandPrediction() {
        // Calcular tasa de crecimiento (últimos 30 días vs 30 días anteriores)
        String growthSql = """
                SELECT
                    COUNT(CASE WHEN date >= (SELECT MAX(date) - INTERVAL '30 days' FROM consultation) THEN 1 END) as current_period,
                    COUNT(CASE WHEN date >= (SELECT MAX(date) - INTERVAL '60 days' FROM consultation) AND date < (SELECT MAX(date) - INTERVAL '30 days' FROM consultation) THEN 1 END) as previous_period
                FROM consultation
                """;
        Query growthQuery = entityManager.createNativeQuery(growthSql);
        Object[] growthResult = (Object[]) growthQuery.getSingleResult();
        long currentCount = ((Number) growthResult[0]).longValue();
        long prevCount = ((Number) growthResult[1]).longValue();

        double growthFactor = 1.05; // Fallback por defecto
        if (prevCount > 0) {
            double rate = (double) (currentCount - prevCount) / prevCount;
            // Limitamos el factor de crecimiento para evitar predicciones locas (entre -50%
            // y +50% + base)
            growthFactor = 1.0 + Math.max(-0.5, Math.min(0.5, rate));
        }

        String historicalSql = """
                SELECT
                    EXTRACT(DOW FROM date) as day_of_week,
                    COUNT(*) as consultations
                FROM consultation
                WHERE date >= (SELECT MAX(date) - INTERVAL '30 days' FROM consultation)
                GROUP BY EXTRACT(DOW FROM date)
                ORDER BY day_of_week
                """;
        Query historicalQuery = entityManager.createNativeQuery(historicalSql);
        @SuppressWarnings("unchecked")
        List<Object[]> historicalResults = historicalQuery.getResultList();

        // Calcular promedios por día de la semana
        Map<String, Integer> predictionsByDay = new HashMap<>();
        String[] dayNames = { "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado" };

        for (Object[] row : historicalResults) {
            int dayOfWeek = ((Number) row[0]).intValue();
            int consultations = ((Number) row[1]).intValue();
            // Promedio semanal aproximado aplicado con el factor de crecimiento
            int predicted = (int) Math.round(consultations * growthFactor);
            predictionsByDay.put(dayNames[dayOfWeek], predicted);
        }

        // Calcular día siguiente
        int tomorrow = (java.time.LocalDate.now().getDayOfWeek().getValue() + 1) % 7;
        int predictedTomorrow = predictionsByDay.getOrDefault(dayNames[tomorrow], 0);

        // Calcular semana siguiente
        int predictedWeek = predictionsByDay.values().stream().mapToInt(Integer::intValue).sum();

        // Calcular días de alta demanda dinámicamente (los 2 días con más consultas)
        List<String> highDemandDays = predictionsByDay.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Recomendaciones basadas en días de alta demanda
        List<String> recommendations = List.of(
                "Reforzar staffing los días " + String.join(" y ", highDemandDays),
                "Preparar protocolos de urgencia para picos de demanda",
                "Considerar extensión de horarios en días críticos");

        // TODO: Calcular confianza real basada en varianza de predicciones históricas
        // Por ahora se usa valor estimado que debería calcularse con modelo ML
        double confianza = calcularConfianzaPrediccion(predictionsByDay);

        return new DemandPredictionDto(
                predictedTomorrow,
                predictedWeek,
                predictionsByDay,
                highDemandDays,
                recommendations,
                confianza);
    }

    public AIAnalysisDto getAIAnalysis() {
        // Análisis de tendencias usando datos históricos
        String trendSql = """
                SELECT
                    TO_CHAR(date, 'YYYY-MM') as month,
                    COUNT(*) as consultations
                FROM consultation
                WHERE date >= (SELECT MAX(date) - INTERVAL '6 months' FROM consultation)
                GROUP BY TO_CHAR(date, 'YYYY-MM')
                ORDER BY TO_CHAR(date, 'YYYY-MM')
                """;
        Query trendQuery = entityManager.createNativeQuery(trendSql);
        @SuppressWarnings("unchecked")
        List<Object[]> trendResults = trendQuery.getResultList();

        // Calcular tendencias
        Map<String, Double> trendPredictions = new HashMap<>();
        if (!trendResults.isEmpty()) {
            Object[] lastMonth = trendResults.get(trendResults.size() - 1);
            Object[] previousMonth = trendResults.size() > 1 ? trendResults.get(trendResults.size() - 2) : lastMonth;

            double lastCount = ((Number) lastMonth[1]).doubleValue();
            double prevCount = ((Number) previousMonth[1]).doubleValue();

            double monthlyGrowth = prevCount > 0 ? ((lastCount - prevCount) / prevCount) * 100 : 0;
            trendPredictions.put("Crecimiento mensual", monthlyGrowth);
            trendPredictions.put("Proyección 3 meses", monthlyGrowth * 3);
            trendPredictions.put("Proyección 6 meses", monthlyGrowth * 6);
        }

        // Insights clave generados dinámicamente basados en datos
        List<String> keyInsights = generarInsightsDinamicos(trendPredictions);

        // Factores de riesgo basados en análisis de carga y capacidad
        List<String> riskFactors = generarFactoresRiesgo();

        // Sugerencias de optimización basadas en métricas actuales
        List<String> optimizationSuggestions = generarSugerenciasOptimizacion();

        // Patrones estacionales calculados dinámicamente
        Map<String, Integer> seasonalPatterns = calcularPatronesEstacionales();

        // Calcular confianza del modelo basada en cantidad de datos históricos
        double modelConfidence = calcularConfianzaModelo(trendResults.size());

        return new AIAnalysisDto(
                java.time.LocalDate.now().toString(),
                keyInsights,
                trendPredictions,
                riskFactors,
                optimizationSuggestions,
                seasonalPatterns,
                modelConfidence);
    }

    // ========== NUEVOS MÉTODOS PARA REQUISITOS PENDIENTES ==========

    /**
     * E13: Gráfico anual de derivaciones a especialistas
     */
    public ReferralStatsDto getReferralStats() {
        // Consultas que son derivaciones (tipo REFERRAL o derivadas a otra
        // especialidad)
        String totalSql = """
                SELECT COUNT(*) as total
                FROM consultation c
                WHERE c.type = 'REFERRAL'
                OR c.referral_specialist IS NOT NULL
                """;
        Query totalQuery = entityManager.createNativeQuery(totalSql);
        Long totalReferrals = 0L;
        try {
            totalReferrals = ((Number) totalQuery.getSingleResult()).longValue();
        } catch (Exception e) {
            // Si la columna no existe, usamos consultas por tipo
            String altSql = "SELECT COUNT(*) FROM consultation WHERE type LIKE '%derivaci%' OR type LIKE '%referral%'";
            Query altQuery = entityManager.createNativeQuery(altSql);
            try {
                totalReferrals = ((Number) altQuery.getSingleResult()).longValue();
            } catch (Exception ex) {
                totalReferrals = 0L;
            }
        }

        // Tendencia mensual de derivaciones
        String monthlySql = """
                SELECT
                    TO_CHAR(c.date, 'YYYY-MM') as month,
                    COUNT(*) as count
                FROM consultation c
                LEFT JOIN healthcare_professional hp ON c.professional_id = hp.id
                WHERE hp.specialty IS NOT NULL
                GROUP BY TO_CHAR(c.date, 'YYYY-MM')
                ORDER BY TO_CHAR(c.date, 'YYYY-MM')
                """;
        Query monthlyQuery = entityManager.createNativeQuery(monthlySql);
        @SuppressWarnings("unchecked")
        List<Object[]> monthlyResults = monthlyQuery.getResultList();

        List<MonthlyReferralDto> monthlyTrend = monthlyResults.stream()
                .map(row -> new MonthlyReferralDto(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());

        // Derivaciones por especialidad
        String specialtySql = """
                SELECT
                    COALESCE(hp.specialty, 'Sin especialidad') as specialty,
                    COUNT(*) as count
                FROM consultation c
                LEFT JOIN healthcare_professional hp ON c.professional_id = hp.id
                WHERE hp.specialty IS NOT NULL AND hp.specialty != 'Medicina General'
                GROUP BY hp.specialty
                ORDER BY COUNT(*) DESC
                LIMIT 10
                """;
        Query specialtyQuery = entityManager.createNativeQuery(specialtySql);
        @SuppressWarnings("unchecked")
        List<Object[]> specialtyResults = specialtyQuery.getResultList();

        long totalSpecialty = specialtyResults.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        List<SpecialtyReferralDto> bySpecialty = specialtyResults.stream()
                .map(row -> new SpecialtyReferralDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        totalSpecialty > 0 ? (((Number) row[1]).doubleValue() / totalSpecialty) * 100 : 0.0))
                .collect(Collectors.toList());

        return new ReferralStatsDto(totalReferrals, monthlyTrend, bySpecialty);
    }

    /**
     * T7/O8: Consultas por tipo (urgentes vs generales) con detalle
     */
    public List<ConsultationTypeStatsDto> getConsultationsByTypeDetailed() {
        String sql = """
                SELECT
                    COALESCE(c.type, 'General') as type,
                    COUNT(*) as count
                FROM consultation c
                GROUP BY c.type
                ORDER BY COUNT(*) DESC
                """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        long total = results.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        return results.stream()
                .map(row -> {
                    String type = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    Double percentage = total > 0 ? (count.doubleValue() / total) * 100 : 0.0;

                    // Mapear tipos a nombres legibles
                    String displayType = switch (type.toUpperCase()) {
                        case "URGENT", "URGENCIA" -> "Urgente";
                        case "GENERAL" -> "General";
                        case "CHRONIC_CONTROL", "CONTROL_CRONICO" -> "Control Crónico";
                        case "PREVENTIVE", "PREVENTIVA" -> "Preventiva";
                        default -> type;
                    };

                    return new ConsultationTypeStatsDto(displayType, count, percentage);
                })
                .collect(Collectors.toList());
    }

    /**
     * E6: Tendencia detallada de enfermedades respiratorias
     */
    public RespiratoryTrendDto getRespiratoryTrendDetailed() {
        System.out.println("🔷 [STATS-SERVICE] getRespiratoryTrendDetailed - Iniciando...");

        // Total de casos respiratorios actual y anterior
        String totalSql = """
                SELECT
                    COUNT(CASE WHEN c.date >= (SELECT MAX(date) - INTERVAL '1 month' FROM consultation) THEN 1 END) as current_month,
                    COUNT(CASE WHEN c.date >= (SELECT MAX(date) - INTERVAL '2 months' FROM consultation)
                               AND c.date < (SELECT MAX(date) - INTERVAL '1 month' FROM consultation) THEN 1 END) as previous_month
                FROM consultation c
                JOIN diagnosis d ON c.id = d.consultation_id
                JOIN cie10 cie ON d.cie10_code = cie.code
                WHERE LOWER(COALESCE(d.description, cie.name)) SIMILAR TO
                    '%(bronquitis|resfri|neumon|faringitis|sinusitis|respiratori|gripe|influenza|asma|tos)%'
                """;
        Query totalQuery = entityManager.createNativeQuery(totalSql);
        Object[] totalResult = (Object[]) totalQuery.getSingleResult();
        Long currentTotal = ((Number) totalResult[0]).longValue();
        Long previousTotal = ((Number) totalResult[1]).longValue();
        Double variation = previousTotal > 0 ? ((currentTotal - previousTotal) * 100.0) / previousTotal : 0.0;

        System.out.println("📊 [STATS-SERVICE] Casos respiratorios - Mes actual: " + currentTotal + ", Mes anterior: " + previousTotal);
        System.out.println("📊 [STATS-SERVICE] Variación: " + variation + "%");

        // Tendencia mensual por enfermedad
        String trendSql = """
                SELECT
                    TO_CHAR(c.date, 'YYYY-MM') as month,
                    COUNT(CASE WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%bronquitis%' THEN 1 END) as bronquitis,
                    COUNT(CASE WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%resfri%' THEN 1 END) as resfriado,
                    COUNT(CASE WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%neumon%' THEN 1 END) as neumonia,
                    COUNT(CASE WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%faringitis%' THEN 1 END) as faringitis,
                    COUNT(CASE WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%sinusitis%' THEN 1 END) as sinusitis,
                    COUNT(*) as total
                FROM consultation c
                JOIN diagnosis d ON c.id = d.consultation_id
                JOIN cie10 cie ON d.cie10_code = cie.code
                WHERE LOWER(COALESCE(d.description, cie.name)) SIMILAR TO
                    '%(bronquitis|resfri|neumon|faringitis|sinusitis|respiratori|gripe|influenza|asma|tos)%'
                AND c.date >= (SELECT MAX(date) - INTERVAL '12 months' FROM consultation)
                GROUP BY TO_CHAR(c.date, 'YYYY-MM')
                ORDER BY month
                """;
        Query trendQuery = entityManager.createNativeQuery(trendSql);
        @SuppressWarnings("unchecked")
        List<Object[]> trendResults = trendQuery.getResultList();

        List<MonthlyRespiratoryDto> monthlyTrend = trendResults.stream()
                .map(row -> new MonthlyRespiratoryDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue(),
                        ((Number) row[5]).longValue(),
                        ((Number) row[6]).longValue()))
                .collect(Collectors.toList());

        System.out.println("📊 [STATS-SERVICE] Tendencia mensual: " + monthlyTrend.size() + " meses");

        // Detalle por enfermedad con variación
        String diseaseSql = """
                WITH current_month AS (
                    SELECT
                        CASE
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%bronquitis%' THEN 'Bronquitis aguda'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%resfri%' THEN 'Resfriado común'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%neumon%' THEN 'Neumonía'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%faringitis%' THEN 'Faringitis'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%sinusitis%' THEN 'Sinusitis'
                            ELSE 'Otra respiratoria'
                        END as disease,
                        COUNT(*) as current_count
                    FROM diagnosis d
                    JOIN cie10 cie ON d.cie10_code = cie.code
                    JOIN consultation c ON d.consultation_id = c.id
                    WHERE c.date >= (SELECT MAX(date) - INTERVAL '1 month' FROM consultation)
                    AND LOWER(COALESCE(d.description, cie.name)) SIMILAR TO
                        '%(bronquitis|resfri|neumon|faringitis|sinusitis|respiratori|gripe|influenza|asma|tos)%'
                    GROUP BY 1
                ),
                previous_month AS (
                    SELECT
                        CASE
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%bronquitis%' THEN 'Bronquitis aguda'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%resfri%' THEN 'Resfriado común'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%neumon%' THEN 'Neumonía'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%faringitis%' THEN 'Faringitis'
                            WHEN LOWER(COALESCE(d.description, cie.name)) LIKE '%sinusitis%' THEN 'Sinusitis'
                            ELSE 'Otra respiratoria'
                        END as disease,
                        COUNT(*) as previous_count
                    FROM diagnosis d
                    JOIN cie10 cie ON d.cie10_code = cie.code
                    JOIN consultation c ON d.consultation_id = c.id
                    WHERE c.date >= (SELECT MAX(date) - INTERVAL '2 months' FROM consultation)
                    AND c.date < (SELECT MAX(date) - INTERVAL '1 month' FROM consultation)
                    AND LOWER(COALESCE(d.description, cie.name)) SIMILAR TO
                        '%(bronquitis|resfri|neumon|faringitis|sinusitis|respiratori|gripe|influenza|asma|tos)%'
                    GROUP BY 1
                )
                SELECT
                    COALESCE(c.disease, p.disease) as disease,
                    COALESCE(c.current_count, 0) as current_count,
                    COALESCE(p.previous_count, 0) as previous_count
                FROM current_month c
                FULL OUTER JOIN previous_month p ON c.disease = p.disease
                ORDER BY COALESCE(c.current_count, 0) DESC
                """;
        Query diseaseQuery = entityManager.createNativeQuery(diseaseSql);
        @SuppressWarnings("unchecked")
        List<Object[]> diseaseResults = diseaseQuery.getResultList();

        List<RespiratoryDiseaseDto> byDisease = diseaseResults.stream()
                .map(row -> {
                    String disease = (String) row[0];
                    Long currentCount = ((Number) row[1]).longValue();
                    Long previousCount = ((Number) row[2]).longValue();
                    Double diseaseVariation = previousCount > 0
                            ? ((currentCount - previousCount) * 100.0) / previousCount
                            : (currentCount > 0 ? 100.0 : 0.0);
                    String status = diseaseVariation > 15 ? "Alerta" : "Normal";

                    return new RespiratoryDiseaseDto(disease, currentCount, previousCount, diseaseVariation, status);
                })
                .collect(Collectors.toList());

        System.out.println("📊 [STATS-SERVICE] Enfermedades por tipo: " + byDisease.size() + " tipos");
        System.out.println("✅ [STATS-SERVICE] getRespiratoryTrendDetailed - Completado");

        return new RespiratoryTrendDto(currentTotal, variation, monthlyTrend, byDisease);
    }

    /**
     * T14: Recetas frecuentes por grupo etario
     */
    public List<PrescriptionsByAgeGroupDto> getPrescriptionsByAgeGroupDetailed() {
        String sql = """
                SELECT
                    m.name as medication,
                    CASE
                        WHEN p.birth_date IS NULL THEN 'UNKNOWN'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 18 THEN 'KIDS'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 30 THEN 'YOUNG'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 50 THEN 'ADULTS'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 65 THEN 'SENIORS'
                        ELSE 'ELDERLY'
                    END as age_group,
                    COUNT(*) as count
                FROM prescription pr
                JOIN consultation c ON pr.consultation_id = c.id
                JOIN patient p ON c.patient_id = p.id
                JOIN prescription_medication pm ON pr.id = pm.prescription_id
                JOIN medication m ON pm.medication_id = m.id
                GROUP BY m.name,
                    CASE
                        WHEN p.birth_date IS NULL THEN 'UNKNOWN'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 18 THEN 'KIDS'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 30 THEN 'YOUNG'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 50 THEN 'ADULTS'
                        WHEN EXTRACT(YEAR FROM AGE((SELECT MAX(date) FROM consultation), p.birth_date)) < 65 THEN 'SENIORS'
                        ELSE 'ELDERLY'
                    END
                ORDER BY m.name, COUNT(*) DESC
                """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Agrupar por medicamento
        Map<String, Map<String, Long>> medicationMap = new LinkedHashMap<>();
        Map<String, Long> medicationTotals = new HashMap<>();

        for (Object[] row : results) {
            String medication = (String) row[0];
            String ageGroup = (String) row[1];
            Long count = ((Number) row[2]).longValue();

            medicationMap.computeIfAbsent(medication, k -> new HashMap<>())
                    .put(ageGroup, count);
            medicationTotals.merge(medication, count, Long::sum);
        }

        // Ordenar por total y tomar top 15
        return medicationTotals.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(15)
                .map(entry -> new PrescriptionsByAgeGroupDto(
                        entry.getKey(),
                        medicationMap.get(entry.getKey()),
                        entry.getValue()))
                .collect(Collectors.toList());
    }

    public List<WeeklyTrendDto> getPrescriptionWeeklyTrend() {
        String sql = """
                SELECT
                    TO_CHAR(date, 'Day') as day_name,
                    COUNT(*) as count,
                    EXTRACT(DOW FROM date) as day_of_week
                FROM prescription
                WHERE date >= (SELECT MAX(date) - INTERVAL '7 days' FROM consultation)
                GROUP BY TO_CHAR(date, 'Day'), EXTRACT(DOW FROM date)
                ORDER BY day_of_week
                """;
        Query query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new WeeklyTrendDto(
                        ((String) row[0]).trim(),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).intValue(),
                        0,
                        0))
                .collect(Collectors.toList());
    }

    // ========== MÉTODOS AUXILIARES PARA ANÁLISIS DE IA ==========

    /**
     * Genera factores de riesgo basados en análisis de carga actual
     */
    private List<String> generarFactoresRiesgo() {
        List<String> riskFactors = new ArrayList<>();

        // Verificar si hay sobrecarga en alguna especialidad
        try {
            String sql = """
                    SELECT hp.specialty, COUNT(*) as count
                    FROM consultation c
                    JOIN healthcare_professional hp ON c.professional_id = hp.id
                    WHERE c.date >= (SELECT MAX(date) - INTERVAL '7 days' FROM consultation)
                    AND hp.specialty IS NOT NULL
                    GROUP BY hp.specialty
                    ORDER BY count DESC
                    LIMIT 3
                    """;
            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            if (!results.isEmpty() && results.size() >= 2) {
                long topCount = ((Number) results.get(0)[1]).longValue();
                long secondCount = ((Number) results.get(1)[1]).longValue();

                if (topCount > secondCount * 2) {
                    String specialty = (String) results.get(0)[0];
                    riskFactors.add("Alta concentración de demanda en " + specialty + " - posible sobrecarga");
                }
            }
        } catch (Exception e) {
            // Ignorar errores
        }

        // Verificar estacionalidad actual
        int month = java.time.LocalDate.now().getMonthValue();
        if (month >= 5 && month <= 8) { // Invierno en Chile
            riskFactors.add("Temporada de alta demanda estacional (invierno) - preparar recursos adicionales");
        }

        if (riskFactors.isEmpty()) {
            riskFactors.add("No se detectan factores de riesgo significativos actualmente");
        }

        return riskFactors;
    }

    /**
     * Genera sugerencias de optimización basadas en métricas actuales
     */
    private List<String> generarSugerenciasOptimizacion() {
        List<String> suggestions = new ArrayList<>();

        // Sugerencia basada en estacionalidad
        int month = java.time.LocalDate.now().getMonthValue();
        if (month >= 5 && month <= 8) { // Invierno
            suggestions.add("Considerar refuerzo temporal de personal para temporada de alta demanda");
        }

        // Sugerencia basada en distribución de consultas
        try {
            String sql = """
                    SELECT
                        EXTRACT(DOW FROM date) as day_of_week,
                        COUNT(*) as count
                    FROM consultation
                    WHERE date >= (SELECT MAX(date) - INTERVAL '30 days' FROM consultation)
                    GROUP BY EXTRACT(DOW FROM date)
                    ORDER BY count DESC
                    LIMIT 2
                    """;
            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            if (!results.isEmpty()) {
                String[] dayNames = { "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado" };
                int busyDay = ((Number) results.get(0)[0]).intValue();
                suggestions.add("Optimizar recursos para los días de mayor demanda (" + dayNames[busyDay] + ")");
            }
        } catch (Exception e) {
            // Ignorar errores
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Mantener monitoreo continuo de indicadores de demanda");
        }

        return suggestions;
    }
}

