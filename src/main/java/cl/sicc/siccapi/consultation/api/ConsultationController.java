package cl.sicc.siccapi.consultation.api;

import cl.sicc.siccapi.consultation.api.dto.ConsultationDto;
import cl.sicc.siccapi.consultation.application.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@Validated
public class ConsultationController {
    private final ConsultationService service;

    @GetMapping
    public ResponseEntity<Page<ConsultationDto>> list(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultationDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ConsultationDto> create(@Valid @RequestBody ConsultationDto dto) {
        ConsultationDto created = service.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsultationDto> update(@PathVariable Long id, @Valid @RequestBody ConsultationDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

