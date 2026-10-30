package com.bank.overdraft.enums;

public enum BankType {
    HDFC("HDFC"),
    AXIS("AXIS"),
    SBI("SBI0"),
    ICICI("ICIC"),
    KOTAK("KOTK"),
    NBFC("NBFC");

    private final String code;

    BankType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
