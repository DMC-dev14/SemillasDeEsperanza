package com.donation.app.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.donation.app.dto.response.CampaignResponse;
import com.donation.app.dto.response.DonationResponse;
import com.donation.app.dto.response.UserResponse;
import com.donation.app.enums.CampaignStatus;
import com.donation.app.enums.PaymentStatus;
import com.donation.app.enums.RolNombre;
import com.donation.app.model.Campaign;
import com.donation.app.repository.CampaignRepository;
import com.donation.app.security.CustomUserDetailsService.CustomUserDetails;
import com.donation.app.service.CampaignService;
import com.donation.app.service.DonationService;
import com.donation.app.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final CampaignRepository campaignRepository;
    private final CampaignService campaignService;
    private final DonationService donationService;
    private final UserService userService;

    @ModelAttribute
    public void addGlobalUserAttributes(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        if (principal != null && principal.getUser() != null) {
            UserResponse userResp = UserService.mapToUserResponse(principal.getUser());
            model.addAttribute("currentUser", userResp);
            model.addAttribute("currentRole", userResp.rolName() != null ? userResp.rolName() : RolNombre.USUARIO.name());
        } else {
            model.addAttribute("currentUser", null);
            model.addAttribute("currentRole", "VISITANTE");
        }
    }

    private boolean isAdmin(CustomUserDetails principal) {
        return principal != null
                && principal.getUser() != null
                && principal.getUser().getRol() != null
                && RolNombre.ADMINISTRADOR.name().equals(principal.getUser().getRol().getName());
    }

    private boolean isClient(CustomUserDetails principal) {
        return principal != null
                && principal.getUser() != null
                && principal.getUser().getRol() != null
                && RolNombre.USUARIO.name().equals(principal.getUser().getRol().getName());
    }

    @GetMapping({"/", "/index"})
    public String index(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        if (isAdmin(principal)) {
            return "redirect:/admin";
        }
        model.addAttribute("campanaDestacada",
                campaignRepository.findFirstByStatusOrderByAmountCollectedDesc(CampaignStatus.ACTIVE).orElse(null));
        return "index";
    }

    @GetMapping("/proyectos")
    public String proyectos(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        if (isAdmin(principal)) {
            return "redirect:/admin";
        }
        model.addAttribute("proyectos",
                campaignRepository.findByStatusOrderByCreateDateDesc(CampaignStatus.ACTIVE));
        return "proyectos";
    }

    @GetMapping("/proyectos/{id}")
    public String proyectoDetalle(@PathVariable Long id,
                                  @AuthenticationPrincipal CustomUserDetails principal,
                                  Model model) {
        if (isAdmin(principal)) {
            return "redirect:/admin";
        }
        Campaign proyecto = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        model.addAttribute("proyecto", proyecto);
        return "proyecto-detalle";
    }

    @GetMapping("/donar")
    public String donar(@RequestParam(required = false) Long proyecto,
                        @AuthenticationPrincipal CustomUserDetails principal,
                        Model model) {
        if (isAdmin(principal)) {
            return "redirect:/admin";
        }
        List<Campaign> activos = campaignRepository.findByStatusOrderByCreateDateDesc(CampaignStatus.ACTIVE);
        model.addAttribute("proyectos", activos);
        Long seleccionado = proyecto;
        if (seleccionado == null && !activos.isEmpty()) {
            seleccionado = activos.get(0).getId();
        }
        model.addAttribute("proyectoSeleccionado", seleccionado);
        return "donar";
    }

    @GetMapping("/nosotros")
    public String nosotros(@AuthenticationPrincipal CustomUserDetails principal) {
        if (isAdmin(principal)) {
            return "redirect:/admin";
        }
        return "nosotros";
    }

    @GetMapping("/contacto")
    public String contacto(@AuthenticationPrincipal CustomUserDetails principal) {
        if (isAdmin(principal)) {
            return "redirect:/admin";
        }
        return "contacto";
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal CustomUserDetails principal) {
        if (isAdmin(principal)) {
            return "redirect:/admin";
        }
        if (isClient(principal)) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/registro")
    public String registro(@AuthenticationPrincipal CustomUserDetails principal) {
        if (isAdmin(principal)) {
            return "redirect:/admin";
        }
        if (isClient(principal)) {
            return "redirect:/dashboard";
        }
        return "registro";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        if (isAdmin(principal)) {
            return "redirect:/admin";
        }
        UserResponse user = UserService.mapToUserResponse(principal.getUser());
        model.addAttribute("usuario", user);
        model.addAttribute("resumen", donationService.getClientDashboardStats(user.id()));
        model.addAttribute("historialDonaciones", donationService.getMyDonations(user.id()));
        model.addAttribute("topDonadores", donationService.getTopDonors());
        model.addAttribute("donacionesRecientes", donationService.getRecentCompletedDonations());
        return "dashboard";
    }

    @GetMapping({"/admin", "/admin/**"})
    public String adminDashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        if (!isAdmin(principal)) {
            return "redirect:/dashboard";
        }
        List<CampaignResponse> campanas = campaignService.getAll();
        List<DonationResponse> donaciones = donationService.getAllDonations();
        List<UserResponse> usuarios = userService.getAll();

        BigDecimal totalRecaudado = campanas.stream()
                .map(CampaignResponse::amountCollected)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long campanasActivas = campanas.stream()
                .filter(c -> c.status() == CampaignStatus.ACTIVE)
                .count();

        long donacionesCompletadas = donaciones.stream()
                .filter(d -> d.status() == PaymentStatus.COMPLETED)
                .count();

        model.addAttribute("adminUser", UserService.mapToUserResponse(principal.getUser()));
        model.addAttribute("campanas", campanas);
        model.addAttribute("donaciones", donaciones);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("totalRecaudadoGlobal", totalRecaudado);
        model.addAttribute("totalCampanas", campanas.size());
        model.addAttribute("campanasActivas", campanasActivas);
        model.addAttribute("totalDonacionesGlobal", donaciones.size());
        model.addAttribute("donacionesCompletadas", donacionesCompletadas);
        model.addAttribute("totalUsuariosGlobal", usuarios.size());

        return "admin";
    }
}

