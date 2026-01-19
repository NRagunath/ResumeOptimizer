package com.resumeopt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    @GetMapping("/company-management")
    public String companyManagement() {
        return "company-management";
    }
    

}