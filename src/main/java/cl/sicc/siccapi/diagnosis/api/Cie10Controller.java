package cl.sicc.siccapi.diagnosis.api;

import cl.sicc.siccapi.diagnosis.api.dto.Cie10Dto;
import cl.sicc.siccapi.diagnosis.application.Cie10Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cie10")
@RequiredArgsConstructor
@Validated
public class Cie10Controller {
    private final Cie10Service service;

    @GetMapping
    public ResponseEntity<Page<Cie10Dto>> list(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Cie10Dto> get(@PathVariable String code) {
        return ResponseEntity.ok(service.findById(code));
    }

    @PostMapping
    public ResponseEntity<Cie10Dto> create(@Valid @RequestBody Cie10Dto dto) {
        Cie10Dto created = service.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{code}")
    public ResponseEntity<Cie10Dto> update(@PathVariable String code, @Valid @RequestBody Cie10Dto dto) {
        return ResponseEntity.ok(service.update(code, dto));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }
}

