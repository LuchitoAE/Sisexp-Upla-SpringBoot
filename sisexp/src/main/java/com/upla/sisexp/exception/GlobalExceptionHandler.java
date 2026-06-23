package com.upla.sisexp.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/");
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException ex, RedirectAttributes redirect,
            HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage(), "timestamp", LocalDateTime.now().toString()));
        }
        redirect.addFlashAttribute("error", ex.getMessage());
        String referer = request.getHeader("Referer");
        return (referer != null) ? "redirect:" + referer : "redirect:/dashboard";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex,
            RedirectAttributes redirect, HttpServletRequest request) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Error de validacion");
        if (isApiRequest(request)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", msg, "timestamp", LocalDateTime.now().toString()));
        }
        redirect.addFlashAttribute("error", msg);
        String referer = request.getHeader("Referer");
        return (referer != null) ? "redirect:" + referer : "redirect:/dashboard";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNotFound(NoResourceFoundException ex, Model model) {
        model.addAttribute("status", 404);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneral(Exception ex, Model model, HttpServletRequest request) {
        log.error("Error interno: {}", ex.getMessage(), ex);
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor",
                            "timestamp", LocalDateTime.now().toString()));
        }
        model.addAttribute("status", 500);
        return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "El archivo excede el tamano maximo permitido (20 MB)",
                        "timestamp", LocalDateTime.now().toString()));
    }
}
