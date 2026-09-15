package com.donaciones.Proyecto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // Renderiza los archivos HTML dentro de src/main/resources/templates
public class ViewController {

    @GetMapping("/")
    public String loginRedirect() {
        return "login"; // Redirige a la página de login como la principal
    }

    @GetMapping("/home")
    public String home() {
        return "index"; // La página de inicio ahora está en /home
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // Muestra templates/login.html
    }

    @GetMapping("/registro")
    public String registro() {
        return "registro"; // Muestra templates/registro.html
    }

    @GetMapping("/donar")
    public String donar() {
        return "donar"; // Muestra templates/donar.html
    }

    @GetMapping("/acerca")
    public String acerca() {
        return "acerca"; // Muestra templates/acerca.html
    }

    @GetMapping("/recuperar")
    public String recuperar() {
        return "recuperar"; // Muestra templates/recuperar.html
    }

    @GetMapping("/reset-contrasenia")
    public String resetContrasenia() {
        return "reset-contrasenia"; // Muestra templates/reset-contrasenia.html
    }

    @GetMapping("/perfil")
    public String perfil() {
        return "perfil"; // Muestra la nueva página de perfil
    }

    @GetMapping("/campanias")
    public String campanias() {
        return "campanias"; // Muestra la lista de campañas
    }

    @GetMapping("/crear-campania")
    public String crearCampania() {
        return "crear-campania"; // Muestra el formulario para crear una campaña
    }
}
