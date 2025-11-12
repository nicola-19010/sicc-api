package cl.sicc.siccapi.prescription.service;

import cl.sicc.siccapi.prescription.domain.PharmaceuticalForm;
import cl.sicc.siccapi.prescription.repository.PharmaceuticalFormRepository;
import cl.sicc.siccapi.prescription.dto.PharmaceuticalFormDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PharmaceuticalFormService {
    private final PharmaceuticalFormRepository repository;

    public Page<PharmaceuticalFormDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public PharmaceuticalFormDto findById(Long id) {
        return repository.findById(id).map(this::toDto).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public PharmaceuticalFormDto create(PharmaceuticalFormDto dto) {
        PharmaceuticalForm p = new PharmaceuticalForm();
        p.setName(dto.getName());
        return toDto(repository.save(p));
    }

    public PharmaceuticalFormDto update(Long id, PharmaceuticalFormDto dto) {
        PharmaceuticalForm p = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.setName(dto.getName());
        return toDto(repository.save(p));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repository.deleteById(id);
    }

    private PharmaceuticalFormDto toDto(PharmaceuticalForm p) {
        PharmaceuticalFormDto d = new PharmaceuticalFormDto();
        d.setId(p.getId());
        d.setName(p.getName());
        return d;
    }
}
