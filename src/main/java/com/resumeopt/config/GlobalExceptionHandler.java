package com.resumeopt.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Controller
@ControllerAdvice
public class GlobalExceptionHandler implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute("jakarta.servlet.error.status_code");
        String errorMessage = (String) request.getAttribute("jakarta.servlet.error.message");
        String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            model.addAttribute("status", statusCode);
            model.addAttribute("error", errorMessage != null ? errorMessage : "An error occurred");
            log.error("Error {} at {}: {}", statusCode, requestUri, errorMessage);
        } else {
            model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            model.addAttribute("error", "An unexpected error occurred");
        }
        
        return resolveViewForPath(requestUri != null ? requestUri : "");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSize(MaxUploadSizeExceededException ex, Model model, HttpServletRequest request) {
        model.addAttribute("error", "Upload too large. Please choose a smaller file.");
        return resolveViewForPath(request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, Model model, HttpServletRequest request) {
        log.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        model.addAttribute("error", "An error occurred while processing your request: " + ex.getMessage());
        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return resolveViewForPath(request.getRequestURI());
    }

    /**
     * Keep the user on the relevant page instead of always bouncing back to the home page.
     */
    private String resolveViewForPath(String path) {
        if (path == null) {
            return "index";
        }
        if (path.startsWith("/resume")) {
            return "resume";
        }
        if (path.startsWith("/jobs")) {
            return "jobs"; // Return to jobs page
        }
        return "index";
    }
}