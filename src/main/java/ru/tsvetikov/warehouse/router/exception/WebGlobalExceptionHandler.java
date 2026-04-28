package ru.tsvetikov.warehouse.router.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class WebGlobalExceptionHandler {

    @ExceptionHandler(CommonBackendException.class)
    public String handleCommonBackendException(CommonBackendException e,
                                               HttpServletRequest request,
                                               RedirectAttributes redirectAttributes) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:" + referer;
        }
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/";
    }
}
