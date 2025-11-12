package cl.sicc.siccapi.healthcareprofessional.api;

import cl.sicc.siccapi.healthcareprofessional.api.dto.HealthcareProfessionalCreateDto;
import cl.sicc.siccapi.healthcareprofessional.api.dto.HealthcareProfessionalDto;
import cl.sicc.siccapi.healthcareprofessional.api.dto.HealthcareProfessionalUpdateDto;
import cl.sicc.siccapi.healthcareprofessional.application.HealthcareProfessionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/healthcare-professionals")
@RequiredArgsConstructor
@Validated
public class HealthcareProfessionalController {
    private final HealthcareProfessionalService service;

    @GetMapping
    public ResponseEntity<Page<HealthcareProfessionalDto>> list(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthcareProfessionalDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<HealthcareProfessionalDto> create(@Valid @RequestBody HealthcareProfessionalCreateDto dto) {
        HealthcareProfessionalDto created = service.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HealthcareProfessionalDto> update(@PathVariable Long id, @Valid @RequestBody HealthcareProfessionalUpdateDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

