package com.example.backend.payment.service;

import com.example.backend.payment.dto.PaymentConfirmRequest;
import com.example.backend.payment.dto.PaymentConfirmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    // 토스페이먼츠 공식 오픈 테스트 시크릿 키
    @Value("${toss.secret-key:test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6}")
    private String tossSecretKey;
    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private final PaymentCompleteService paymentCompleteService;
    private final RestClient restClient = RestClient.builder().build();

    /**
     * 토스페이먼츠 Server-to-Server 최종 결제 승인 API
     * DB 커넥션 풀 고갈 방지를 위해 이 메서드에는 @Transactional을 절대 붙이지 않습니다.
     */
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        log.info("[토스 본사 승인 요청 시작] orderId={}, amount={}", request.orderId(), request.amount());

        // Basic Auth: Base64(secretKey + ":")
        String authValue = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = Map.of(
                "paymentKey", request.paymentKey(),
                "orderId", request.orderId(),
                "amount", request.amount()
        );

        try {
            var response = restClient.post()
                    .uri(TOSS_CONFIRM_URL)
                    .header("Authorization", "Basic " + authValue)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String errorBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.error("[토스 승인 실패] status={}, body={}", res.getStatusCode(), errorBody);
                        throw new IllegalStateException("토스 결제 승인에 실패했습니다: " + errorBody);
                    })
                    .toEntity(String.class);

            log.info("[토스 본사 승인 완료] HTTP status={}", response.getStatusCode());

            // 결제 승인 후 예약 및 좌석 상태 확정
            return paymentCompleteService.completeReservationAndSeats(
                    request.orderId(),
                    request.paymentKey(),
                    request.amount()
            );

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("[토스 통신 중 예외 발생]", e);
            throw new IllegalStateException("결제 승인 통신 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
