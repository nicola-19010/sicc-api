package cl.sicc.siccapi.diagnosis.service;

import cl.sicc.siccapi.consultation.domain.Consultation;
import cl.sicc.siccapi.consultation.repository.ConsultationRepository;
import cl.sicc.siccapi.diagnosis.domain.Cie10;
import cl.sicc.siccapi.diagnosis.domain.Diagnosis;
import cl.sicc.siccapi.diagnosis.dto.DiagnosisDto;
import cl.sicc.siccapi.diagnosis.repository.Cie10Repository;
import cl.sicc.siccapi.diagnosis.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosisService {
    private final DiagnosisRepository repository;
    private final ConsultationRepository consultationRepository;
    private final Cie10Repository cie10Repository;

    public Page<DiagnosisDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public List<DiagnosisDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public DiagnosisDto findById(Long id) {
        return repository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<DiagnosisDto> findByConsultationId(Long consultationId) {
        return repository.findByConsultationId(consultationId).stream()
                .map(this::toDto).toList();
    }

    public List<DiagnosisDto> findByCie10Code(String code) {
        return repository.findByCie10Code(code).stream()
                .map(this::toDto).toList();
    }

    public DiagnosisDto create(DiagnosisDto dto) {
        Diagnosis d = new Diagnosis();
        d.setDescription(dto.getDescription());

        Consultation consultation = consultationRepository.findById(dto.getConsultationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulta no encontrada"));
        d.setConsultation(consultation);

        Cie10 cie10 = cie10Repository.findById(dto.getCie10Code())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "CIE-10 no encontrado: " + dto.getCie10Code()));
        d.setCie10(cie10);

        Diagnosis saved = repository.save(d);
        return toDto(saved);
    }

    public DiagnosisDto update(Long id, DiagnosisDto dto) {
        Diagnosis d = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        d.setDescription(dto.getDescription());

        if (dto.getConsultationId() != null) {
            Consultation consultation = consultationRepository.findById(dto.getConsultationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulta no encontrada"));
            d.setConsultation(consultation);
        }

        if (dto.getCie10Code() != null) {
            Cie10 cie10 = cie10Repository.findById(dto.getCie10Code())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "CIE-10 no encontrado: " + dto.getCie10Code()));
            d.setCie10(cie10);
        }

        return toDto(repository.save(d));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repository.deleteById(id);
    }

    private DiagnosisDto toDto(Diagnosis d) {
        DiagnosisDto dto = new DiagnosisDto();
        dto.setId(d.getId());
        dto.setConsultationId(d.getConsultation() != null ? d.getConsultation().getId() : null);
        dto.setCie10Code(d.getCie10() != null ? d.getCie10().getCode() : null);
        dto.setCie10Description(d.getCie10() != null ? d.getCie10().getName() : null);
        dto.setDescription(d.getDescription());
        return dto;
    }
}