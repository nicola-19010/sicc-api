package cl.sicc.siccapi.prescription.controller;

import cl.sicc.siccapi.prescription.api.dto.PharmaceuticalFormDto;
import cl.sicc.siccapi.prescription.service.PharmaceuticalFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pharmaceutical-forms")
@RequiredArgsConstructor
@Validated
public class PharmaceuticalFormController {
    private final PharmaceuticalFormService service;

    @GetMapping
    public ResponseEntity<Page<PharmaceuticalFormDto>> list(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PharmaceuticalFormDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<PharmaceuticalFormDto> create(@Valid @RequestBody PharmaceuticalFormDto dto) {
        PharmaceuticalFormDto created = service.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PharmaceuticalFormDto> update(@PathVariable Long id, @Valid @RequestBody PharmaceuticalFormDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

