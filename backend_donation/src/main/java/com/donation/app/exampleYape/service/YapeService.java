package com.donation.app.exampleYape.service;

import com.donation.app.exampleYape.model.ChargePaymentRequestDTO;
import com.donation.app.exampleYape.model.ChargePaymentResponse;
import com.donation.app.exampleYape.model.YapeTokenRequestDTO;
import com.donation.app.exampleYape.model.YapeTokenResponse;

import reactor.core.publisher.Mono;

public interface YapeService {

    Mono<ChargePaymentResponse> chargePayment(ChargePaymentRequestDTO body);

    Mono<YapeTokenResponse> tokenize(YapeTokenRequestDTO body);
}