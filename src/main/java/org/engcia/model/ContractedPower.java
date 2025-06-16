package org.engcia.model;

public enum ContractedPower {
    VERY_LOW(1.15),
    LOW(2.3),
    MEDIUM(3.45),
    MEDIUM_HIGH(4.6),
    HIGH(5.75),
    VERY_HIGH(6.9),
    HIGHEST(10.35);
    private final Double value;
    private ContractedPower(Double value) {
        this.value=value;
    }
    public Double getValue(){
        return value;
    }
}
