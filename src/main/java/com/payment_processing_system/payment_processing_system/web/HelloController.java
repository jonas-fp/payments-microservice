package com.payment_processing_system.payment_processing_system.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping({ "/", "/hello" })
    public String hello() {
        return "Hello there, from payment-processing-system";
    }
}
