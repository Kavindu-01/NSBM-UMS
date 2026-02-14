package lk.nsbm.university.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("errorMessage", "You do not have permission to access this resource.");
        log.warn("Access denied: {}", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler({NoSuchElementException.class, IllegalArgumentException.class})
    public String handleNotFound(RuntimeException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        log.error("Resource error", ex);
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
        log.error("Unexpected error", ex);
        return "error/500";
    }
}
