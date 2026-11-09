package com.donation.app.examplePayPal.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paypal")
public class PayPalClientController {

    @PostMapping("/create-order")
    public void createOrder(){

    }

    @PostMapping("orders/id/capture")
    public void captureOrder(){

    }

    @GetMapping("/orders/id")
    public void getOrderDetails(){

    }
    
}
