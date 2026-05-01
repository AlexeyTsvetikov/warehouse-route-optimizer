package ru.tsvetikov.warehouse.router.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(basePackages = "ru.tsvetikov.warehouse.router.controllers.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebGlobalExceptionHandler {

    @ExceptionHandler(CommonBackendException.class)
    public String handleCommonBackendException(CommonBackendException e,
                                               HttpServletRequest request,
                                               RedirectAttributes redirectAttributes) {
        String referer = request.getHeader("Referer");


        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(requestedWith)) {
            throw e;
        }

        if (referer != null && !referer.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:" + referer;
        }
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/";
    }
}
