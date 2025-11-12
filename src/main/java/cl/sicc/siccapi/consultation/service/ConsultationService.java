package cl.sicc.siccapi.consultation.service;

import cl.sicc.siccapi.consultation.dto.ConsultationDto;
import cl.sicc.siccapi.consultation.dto.DiagnosisDto;
import cl.sicc.siccapi.consultation.domain.Consultation;
import cl.sicc.siccapi.diagnosis.domain.Diagnosis;
import cl.sicc.siccapi.diagnosis.domain.Cie10;
import cl.sicc.siccapi.consultation.repository.ConsultationRepository;
import cl.sicc.siccapi.patient.repository.PatientRepository;
import cl.sicc.siccapi.healthcareprofessional.repository.HealthcareProfessionalRepository;
import cl.sicc.siccapi.diagnosis.repository.Cie10Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationService {
    private final ConsultationRepository repository;
    private final PatientRepository patientRepository;
    private final HealthcareProfessionalRepository professionalRepository;
    private final Cie10Repository cie10Repository;

    public Page<ConsultationDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public ConsultationDto findById(Long id) {
        return repository.findById(id).map(this::toDto).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public ConsultationDto create(ConsultationDto dto) {
        Consultation c = new Consultation();
        c.setDate(dto.getDate());
        if (dto.getType() != null) c.setType(Consultation.Type.valueOf(dto.getType()));
        c.setPatient(patientRepository.findById(dto.getPatientId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paciente no encontrado")));
        c.setProfessional(professionalRepository.findById(dto.getProfessionalId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profesional no encontrado")));

        if (dto.getDiagnoses() != null) {
            List<Diagnosis> list = new ArrayList<>();
            for (DiagnosisDto dd : dto.getDiagnoses()) {
                Diagnosis diag = new Diagnosis();
                Cie10 cie = cie10Repository.findById(dd.getCie10Code()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "CIE10 no existe: " + dd.getCie10Code()));
                diag.setCie10(cie);
                diag.setDescription(dd.getDescription());
                diag.setConsultation(c);
                list.add(diag);
            }
            c.setDiagnoses(list);
        }

        Consultation saved = repository.save(c);
        return toDto(saved);
    }

    public ConsultationDto update(Long id, ConsultationDto dto) {
        Consultation c = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.setDate(dto.getDate());
        if (dto.getType() != null) c.setType(Consultation.Type.valueOf(dto.getType()));
        if (dto.getPatientId() != null) c.setPatient(patientRepository.findById(dto.getPatientId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paciente no encontrado")));
        if (dto.getProfessionalId() != null) c.setProfessional(professionalRepository.findById(dto.getProfessionalId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profesional no encontrado")));

        if (dto.getDiagnoses() != null) {
            List<Diagnosis> list = new ArrayList<>();
            for (DiagnosisDto dd : dto.getDiagnoses()) {
                Diagnosis diag = new Diagnosis();
                Cie10 cie = cie10Repository.findById(dd.getCie10Code()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "CIE10 no existe: " + dd.getCie10Code()));
                diag.setCie10(cie);
                diag.setDescription(dd.getDescription());
                diag.setConsultation(c);
                list.add(diag);
            }
            c.getDiagnoses().clear();
            c.getDiagnoses().addAll(list);
        }

        return toDto(repository.save(c));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repository.deleteById(id);
    }

    private ConsultationDto toDto(Consultation c) {
        ConsultationDto d = new ConsultationDto();
        d.setId(c.getId());
        d.setDate(c.getDate());
        d.setType(c.getType() != null ? c.getType().name() : null);
        d.setPatientId(c.getPatient() != null ? c.getPatient().getId() : null);
        d.setProfessionalId(c.getProfessional() != null ? c.getProfessional().getId() : null);
        if (c.getDiagnoses() != null) {
            List<DiagnosisDto> list = new ArrayList<>();
            for (Diagnosis diag : c.getDiagnoses()) {
                DiagnosisDto dd = new DiagnosisDto();
                dd.setId(diag.getId());
                dd.setCie10Code(diag.getCie10() != null ? diag.getCie10().getCode() : null);
                dd.setDescription(diag.getDescription());
                list.add(dd);
            }
            d.setDiagnoses(list);
        }
        return d;
    }
}

