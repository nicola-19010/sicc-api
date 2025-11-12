package cl.sicc.siccapi.prescription.application;

import cl.sicc.siccapi.prescription.api.dto.MedicationDto;
import cl.sicc.siccapi.prescription.domain.Medication;
import cl.sicc.siccapi.prescription.infrastructure.MedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MedicationService {
    private final MedicationRepository repository;

    public Page<MedicationDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public MedicationDto findById(Long id) {
        return repository.findById(id).map(this::toDto).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public MedicationDto create(MedicationDto dto) {
        Medication m = new Medication();
        m.setName(dto.getName());
        m.setDosage(dto.getDosage());
        // pharmaceutical form association omitted here; can be set via separate endpoint
        return toDto(repository.save(m));
    }

    public MedicationDto update(Long id, MedicationDto dto) {
        Medication m = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        m.setName(dto.getName());
        m.setDosage(dto.getDosage());
        return toDto(repository.save(m));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repository.deleteById(id);
    }

    private MedicationDto toDto(Medication m) {
        MedicationDto d = new MedicationDto();
        d.setId(m.getId());
        d.setName(m.getName());
        d.setDosage(m.getDosage());
        d.setPharmaceuticalFormName(m.getPharmaceuticalForm() != null ? m.getPharmaceuticalForm().getName() : null);
        return d;
    }
}

