package cl.sicc.siccapi.healthcareprofessional.service;

import org.springframework.stereotype.Service;
import cl.sicc.siccapi.healthcareprofessional.repository.HealthcareProfessionalRepository;
import cl.sicc.siccapi.healthcareprofessional.domain.HealthcareProfessional;
import java.util.List;

@Service
public class HealthcareProfessionalService {

    private final HealthcareProfessionalRepository repository;

    public HealthcareProfessionalService(HealthcareProfessionalRepository repository) {
        this.repository = repository;
    }

    public List<HealthcareProfessional> findAll() {
        return repository.findAll();
    }

    public HealthcareProfessional save(HealthcareProfessional professional) {
        return repository.save(professional);
    }
}
