package com.example.crypto_payment_system.domain.exchangeRate;

/**
 * Model class representing an exchange rate from the server
 */
public class ExchangeRate {
    private Long id;
    private double eurUsd;
    private Long lastUpdated;

    public ExchangeRate() {
    }

    public ExchangeRate(Long id, double eurUsd, Long lastUpdated) {
        this.id = id;
        this.eurUsd = eurUsd;
        this.lastUpdated = lastUpdated;
    }

    public double getEurUsd() {
        return eurUsd;
    }

    public void setEurUsd(double eurUsd) {
        this.eurUsd = eurUsd;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

} 