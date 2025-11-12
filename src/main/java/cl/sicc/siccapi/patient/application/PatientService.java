package cl.sicc.siccapi.patient.application;

import cl.sicc.siccapi.patient.api.dto.PatientCreateDto;
import cl.sicc.siccapi.patient.api.dto.PatientDto;
import cl.sicc.siccapi.patient.api.dto.PatientUpdateDto;
import cl.sicc.siccapi.patient.domain.Patient;
import cl.sicc.siccapi.patient.infrastructure.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository repository;

    public Page<PatientDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public PatientDto findById(Long id) {
        return repository.findById(id).map(this::toDto).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public PatientDto create(PatientCreateDto dto) {
        if (repository.findByRut(dto.getRut()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paciente con ese RUT ya existe");
        }
        Patient p = new Patient();
        p.setRut(dto.getRut());
        p.setName(dto.getName());
        p.setBirthDate(dto.getBirthDate());
        p.setSex(dto.getSex());
        p.setResidentialSector(dto.getResidentialSector());
        if (dto.getFonasaTier() != null) p.setFonasaTier(Patient.FonasaTier.valueOf(dto.getFonasaTier()));
        Patient saved = repository.save(p);
        return toDto(saved);
    }

    public PatientDto update(Long id, PatientUpdateDto dto) {
        Patient p = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.setName(dto.getName());
        p.setBirthDate(dto.getBirthDate());
        p.setSex(dto.getSex());
        p.setResidentialSector(dto.getResidentialSector());
        if (dto.getFonasaTier() != null) p.setFonasaTier(Patient.FonasaTier.valueOf(dto.getFonasaTier()));
        return toDto(repository.save(p));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repository.deleteById(id);
    }

    public Optional<PatientDto> findByRut(String rut) {
        return repository.findByRut(rut).map(this::toDto);
    }

    private PatientDto toDto(Patient p) {
        PatientDto d = new PatientDto();
        d.setId(p.getId());
        d.setRut(p.getRut());
        d.setName(p.getName());
        d.setBirthDate(p.getBirthDate());
        d.setAge(p.getAge());
        d.setSex(p.getSex());
        d.setResidentialSector(p.getResidentialSector());
        d.setFonasaTier(p.getFonasaTier() != null ? p.getFonasaTier().name() : null);
        return d;
    }
}

