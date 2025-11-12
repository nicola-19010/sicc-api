package cl.sicc.siccapi.diagnosis.service;

import cl.sicc.siccapi.diagnosis.dto.Cie10Dto;
import cl.sicc.siccapi.diagnosis.domain.Cie10;
import cl.sicc.siccapi.diagnosis.repository.Cie10Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class Cie10Service {
    private final Cie10Repository repository;

    public Page<Cie10Dto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public Cie10Dto findById(String code) {
        return repository.findById(code).map(this::toDto).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Cie10Dto create(Cie10Dto dto) {
        if (repository.existsById(dto.getCode())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CIE10 ya existe");
        Cie10 c = new Cie10();
        c.setCode(dto.getCode());
        c.setName(dto.getName());
        return toDto(repository.save(c));
    }

    public Cie10Dto update(String code, Cie10Dto dto) {
        Cie10 c = repository.findById(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.setName(dto.getName());
        return toDto(repository.save(c));
    }

    public void delete(String code) {
        if (!repository.existsById(code)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repository.deleteById(code);
    }

    private Cie10Dto toDto(Cie10 c) {
        Cie10Dto d = new Cie10Dto();
        d.setCode(c.getCode());
        d.setName(c.getName());
        return d;
    }
}

