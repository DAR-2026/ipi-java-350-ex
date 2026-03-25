package com.ipiecoles.java.java350.service;

import com.ipiecoles.java.java350.exception.EmployeException;
import com.ipiecoles.java.java350.model.Employe;
import com.ipiecoles.java.java350.model.Entreprise;
import com.ipiecoles.java.java350.model.NiveauEtude;
import com.ipiecoles.java.java350.model.Poste;
import com.ipiecoles.java.java350.repository.EmployeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import java.time.LocalDate;

@Service
public class EmployeService {

    @Autowired
    private EmployeRepository employeRepository;

    /**
     * Méthode enregistrant un nouvel employé
     */
    public void embaucheEmploye(String nom, String prenom, Poste poste, NiveauEtude niveauEtude, Double tempsPartiel) throws EmployeException, EntityExistsException {
        String matricule = genererNouveauMatricule(poste);

        if(employeRepository.findByMatricule(matricule) != null){
            throw new EntityExistsException("L'employé de matricule " + matricule + " existe déjà en BDD");
        }

        Double salaire = calculerSalaireEmbauche(niveauEtude, tempsPartiel);

        Employe employe = new Employe(nom, prenom, matricule, LocalDate.now(), salaire, Entreprise.PERFORMANCE_BASE, tempsPartiel);
        employeRepository.save(employe);
    }

    private String genererNouveauMatricule(Poste poste) throws EmployeException {
        String typeEmploye = poste.name().substring(0,1);
        String lastMatricule = employeRepository.findLastMatricule();
        if(lastMatricule == null){
            lastMatricule = Entreprise.MATRICULE_INITIAL;
        }
        Integer numeroMatricule = Integer.parseInt(lastMatricule) + 1;
        if(numeroMatricule >= 100000){
            throw new EmployeException("Limite des 100000 matricules atteinte !");
        }
        return typeEmploye + String.format("%05d", numeroMatricule);
    }

    private Double calculerSalaireEmbauche(NiveauEtude niveauEtude, Double tempsPartiel) {
        Double salaire = Entreprise.COEFF_SALAIRE_ETUDES.get(niveauEtude) * Entreprise.SALAIRE_BASE;
        if(tempsPartiel != null){
            salaire = salaire * tempsPartiel;
        }
        return salaire;
    }

    /**
     * Méthode calculant la performance d'un commercial
     */
    public void calculPerformanceCommercial(String matricule, Long caTraite, Long objectifCa) throws EmployeException {
        
        // 1. Validation des entrées (CORRECTIF COMPLEXITÉ)
        validerEntreesPerformance(matricule, caTraite, objectifCa);

        // 2. Recherche de l'employé
        Employe employe = employeRepository.findByMatricule(matricule);
        if(employe == null){
            throw new EmployeException("Le matricule " + matricule + " n'existe pas !");
        }

        // 3. Calcul de la performance de base (CORRECTIF COMPLEXITÉ)
        Integer performance = calculerPalierPerformance(caTraite, objectifCa, employe.getPerformance());

        // 4. Ajustement selon la moyenne
        Double performanceMoyenne = employeRepository.avgPerformanceWhereMatriculeStartsWith("C");
        if(performanceMoyenne != null && performance > performanceMoyenne){
            performance++;
        }

        employe.setPerformance(performance);
        employeRepository.save(employe);
    }

    private void validerEntreesPerformance(String matricule, Long caTraite, Long objectifCa) throws EmployeException {
        if(caTraite == null || caTraite < 0 || objectifCa == null || objectifCa < 0){
            throw new EmployeException("Le chiffre d'affaire ou l'objectif ne peut être négatif ou null !");
        }
        if(matricule == null || !matricule.startsWith("C")){
            throw new EmployeException("Le matricule ne peut être null et doit commencer par un C !");
        }
    }

    private Integer calculerPalierPerformance(Long ca, Long obj, Integer perfActuelle) {
        if(ca >= obj * 0.8 && ca < obj * 0.95) return Math.max(Entreprise.PERFORMANCE_BASE, perfActuelle - 2);
        if(ca >= obj * 0.95 && ca <= obj * 1.05) return Math.max(Entreprise.PERFORMANCE_BASE, perfActuelle);
        if(ca > obj * 1.05 && ca <= obj * 1.2) return perfActuelle + 1;
        if(ca > obj * 1.2) return perfActuelle + 4;
        return Entreprise.PERFORMANCE_BASE;
    }
}
