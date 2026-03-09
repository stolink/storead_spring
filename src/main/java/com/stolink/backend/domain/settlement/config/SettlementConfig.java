package com.stolink.backend.domain.settlement.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SettlementConfig {

    @Value("${settlement.platform-fee-rate:0.3}")
    private double platformFeeRate;

    @Value("${settlement.minimum-amount:100}")
    private long minimumSettlementAmount;
}
