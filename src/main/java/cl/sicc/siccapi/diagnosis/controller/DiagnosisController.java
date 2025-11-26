package cl.sicc.siccapi.diagnosis.controller;

import cl.sicc.siccapi.diagnosis.dto.DiagnosisDto;
import cl.sicc.siccapi.diagnosis.service.DiagnosisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diagnoses")
@RequiredArgsConstructor
public class DiagnosisController {
    private final DiagnosisService service;

    @GetMapping
    public ResponseEntity<Page<DiagnosisDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<List<DiagnosisDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiagnosisDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/consultation/{consultationId}")
    public ResponseEntity<List<DiagnosisDto>> getByConsultation(@PathVariable Long consultationId) {
        return ResponseEntity.ok(service.findByConsultationId(consultationId));
    }

    @GetMapping("/cie10/{code}")
    public ResponseEntity<List<DiagnosisDto>> getByCie10Code(@PathVariable String code) {
        return ResponseEntity.ok(service.findByCie10Code(code));
    }

    @PostMapping
    public ResponseEntity<DiagnosisDto> create(@Valid @RequestBody DiagnosisDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiagnosisDto> update(@PathVariable Long id, @Valid @RequestBody DiagnosisDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
