package ru.tsvetikov.warehouse.router.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/tsd/login")
    public String tsdLogin() {
        return "tsd/login-tsd";
    }
}
