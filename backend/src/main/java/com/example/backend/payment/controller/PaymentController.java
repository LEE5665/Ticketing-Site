package com.example.backend.payment.controller;

import com.example.backend.payment.dto.PaymentConfirmRequest;
import com.example.backend.payment.dto.PaymentConfirmResponse;
import com.example.backend.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 토스페이먼츠 결제 승인 API
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }
}
