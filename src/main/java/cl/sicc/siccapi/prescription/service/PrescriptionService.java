package cl.sicc.siccapi.prescription.service;

import cl.sicc.siccapi.prescription.dto.PrescriptionDto;
import cl.sicc.siccapi.prescription.dto.PrescriptionMedicationDto;
import cl.sicc.siccapi.prescription.domain.Prescription;
import cl.sicc.siccapi.prescription.domain.PrescriptionMedication;
import cl.sicc.siccapi.prescription.domain.Medication;
import cl.sicc.siccapi.prescription.repository.PrescriptionRepository;
import cl.sicc.siccapi.consultation.repository.ConsultationRepository;
import cl.sicc.siccapi.prescription.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {
    private final PrescriptionRepository repository;
    private final ConsultationRepository consultationRepository;
    private final MedicationRepository medicationRepository;

    public Page<PrescriptionDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    public PrescriptionDto findById(Long id) {
        return repository.findById(id).map(this::toDto).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public PrescriptionDto create(PrescriptionDto dto) {
        Prescription p = new Prescription();
        p.setDate(dto.getDate());
        p.setConsultation(consultationRepository.findById(dto.getConsultationId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulta no encontrada")));

        if (dto.getMedications() != null) {
            List<PrescriptionMedication> list = new ArrayList<>();
            for (PrescriptionMedicationDto md : dto.getMedications()) {
                Medication m = medicationRepository.findById(md.getMedicationId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Medicamento no encontrado: " + md.getMedicationId()));
                PrescriptionMedication pm = new PrescriptionMedication();
                // No asignamos el EmbeddedId manualmente; dejamos que JPA lo rellene a partir de las asociaciones (@MapsId)
                pm.setMedication(m);
                pm.setQuantity(md.getQuantity());
                pm.setInstructions(md.getInstructions());
                pm.setPrescription(p);
                list.add(pm);
            }
            p.setPrescriptionMedications(list);
        }

        Prescription saved = repository.save(p);
        return toDto(saved);
    }

    public PrescriptionDto update(Long id, PrescriptionDto dto) {
        Prescription p = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.setDate(dto.getDate());
        if (dto.getConsultationId() != null) p.setConsultation(consultationRepository.findById(dto.getConsultationId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulta no encontrada")));

        if (dto.getMedications() != null) {
            p.getPrescriptionMedications().clear();
            List<PrescriptionMedication> list = new ArrayList<>();
            for (PrescriptionMedicationDto md : dto.getMedications()) {
                Medication m = medicationRepository.findById(md.getMedicationId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Medicamento no encontrado: " + md.getMedicationId()));
                PrescriptionMedication pm = new PrescriptionMedication();
                pm.setMedication(m);
                pm.setQuantity(md.getQuantity());
                pm.setInstructions(md.getInstructions());
                pm.setPrescription(p);
                list.add(pm);
            }
            p.getPrescriptionMedications().addAll(list);
        }

        return toDto(repository.save(p));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repository.deleteById(id);
    }

    private PrescriptionDto toDto(Prescription p) {
        PrescriptionDto d = new PrescriptionDto();
        d.setId(p.getId());
        d.setDate(p.getDate());
        d.setConsultationId(p.getConsultation() != null ? p.getConsultation().getId() : null);
        if (p.getPrescriptionMedications() != null) {
            List<PrescriptionMedicationDto> list = new ArrayList<>();
            for (PrescriptionMedication pm : p.getPrescriptionMedications()) {
                PrescriptionMedicationDto md = new PrescriptionMedicationDto();
                md.setMedicationId(pm.getMedication() != null ? pm.getMedication().getId() : null);
                md.setMedicationName(pm.getMedication() != null ? pm.getMedication().getName() : null);
                md.setQuantity(pm.getQuantity());
                md.setInstructions(pm.getInstructions());
                list.add(md);
            }
            d.setMedications(list);
        }
        return d;
    }
}
