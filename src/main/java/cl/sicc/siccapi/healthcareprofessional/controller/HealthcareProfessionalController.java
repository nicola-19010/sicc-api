package cl.sicc.siccapi.healthcareprofessional.controller;

import org.springframework.web.bind.annotation.*;
import cl.sicc.siccapi.healthcareprofessional.service.HealthcareProfessionalService;
import cl.sicc.siccapi.healthcareprofessional.domain.HealthcareProfessional;
import java.util.List;

@RestController
@RequestMapping("/api/healthcare-professionals")
public class HealthcareProfessionalController {

    private final HealthcareProfessionalService service;

    public HealthcareProfessionalController(HealthcareProfessionalService service) {
        this.service = service;
    }

    @GetMapping
    public List<HealthcareProfessional> getAll() {
        return service.findAll();
    }

    @PostMapping
    public HealthcareProfessional create(@RequestBody HealthcareProfessional professional) {
        return service.save(professional);
    }
}
