package com.donation.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.donation.app.enums.CampaignStatus;
import com.donation.app.model.Campaign;
import com.donation.app.repository.CampaignRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final CampaignRepository campaignRepository;

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("campanaDestacada",
                campaignRepository.findFirstByStatusOrderByAmountCollectedDesc(CampaignStatus.ACTIVE).orElse(null));
        return "index";
    }

    @GetMapping("/proyectos")
    public String proyectos(Model model) {
        model.addAttribute("proyectos",
                campaignRepository.findByStatusOrderByCreateDateDesc(CampaignStatus.ACTIVE));
        return "proyectos";
    }

    @GetMapping("/proyectos/{id}")
    public String proyectoDetalle(@PathVariable Long id, Model model) {
        Campaign proyecto = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        model.addAttribute("proyecto", proyecto);
        return "proyecto-detalle";
    }

    @GetMapping("/donar")
    public String donar(@RequestParam(required = false) Long proyecto, Model model) {
        model.addAttribute("proyectos",
                campaignRepository.findByStatusOrderByCreateDateDesc(CampaignStatus.ACTIVE));
        model.addAttribute("proyectoSeleccionado", proyecto);
        return "donar";
    }

    @GetMapping("/nosotros")
    public String nosotros() {
        return "nosotros";
    }

    @GetMapping("/contacto")
    public String contacto() {
        return "contacto";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registro")
    public String registro() {
        return "registro";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
