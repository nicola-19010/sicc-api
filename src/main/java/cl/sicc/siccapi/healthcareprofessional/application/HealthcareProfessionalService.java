package cl.sicc.siccapi.healthcareprofessional.application;

import cl.sicc.siccapi.healthcareprofessional.dto.HealthcareProfessionalCreateDto;
import cl.sicc.siccapi.healthcareprofessional.dto.HealthcareProfessionalDto;
import cl.sicc.siccapi.healthcareprofessional.dto.HealthcareProfessionalUpdateDto;
import cl.sicc.siccapi.healthcareprofessional.domain.HealthcareProfessional;
import cl.sicc.siccapi.healthcareprofessional.infrastructure.HealthcareProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class HealthcareProfessionalService {
    private final HealthcareProfessionalRepository repository;

    public Page<HealthcareProfessionalDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public HealthcareProfessionalDto findById(Long id) {
        return repository.findById(id).map(this::toDto).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public HealthcareProfessionalDto create(HealthcareProfessionalCreateDto dto) {
        if (repository.findByRut(dto.getRut()).isPresent()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profesional ya existe");
        HealthcareProfessional hp = new HealthcareProfessional();
        hp.setRut(dto.getRut());
        hp.setName(dto.getName());
        hp.setSpecialty(dto.getSpecialty());
        return toDto(repository.save(hp));
    }

    public HealthcareProfessionalDto update(Long id, HealthcareProfessionalUpdateDto dto) {
        HealthcareProfessional hp = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        hp.setName(dto.getName());
        hp.setSpecialty(dto.getSpecialty());
        return toDto(repository.save(hp));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repository.deleteById(id);
    }

    private HealthcareProfessionalDto toDto(HealthcareProfessional hp) {
        HealthcareProfessionalDto d = new HealthcareProfessionalDto();
        d.setId(hp.getId());
        d.setRut(hp.getRut());
        d.setName(hp.getName());
        d.setSpecialty(hp.getSpecialty());
        return d;
    }
}

