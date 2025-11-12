package cl.sicc.siccapi.prescription.controller;

import cl.sicc.siccapi.prescription.dto.PrescriptionDto;
import cl.sicc.siccapi.prescription.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Validated
public class PrescriptionController {
    private final PrescriptionService service;

    @GetMapping
    public ResponseEntity<Page<PrescriptionDto>> list(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<PrescriptionDto> create(@Valid @RequestBody PrescriptionDto dto) {
        PrescriptionDto created = service.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionDto> update(@PathVariable Long id, @Valid @RequestBody PrescriptionDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
