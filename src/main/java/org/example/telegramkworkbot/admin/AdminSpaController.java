package org.example.telegramkworkbot.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Клиентский роутинг SPA: любые пути /admin/** без расширения отдаём index.html. */
@Controller
public class AdminSpaController {

    @GetMapping({"/", "/admin"})
    public String root() {
        return "redirect:/admin/";
    }

    @GetMapping({"/admin/", "/admin/{page:(?!api|assets)[^.]*}", "/admin/{page:(?!api|assets)[^.]*}/**"})
    public String spa() {
        return "forward:/admin/index.html";
    }
}
