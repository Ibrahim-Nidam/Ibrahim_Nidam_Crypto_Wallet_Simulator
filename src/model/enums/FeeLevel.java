package model.enums;

public enum FeeLevel {
    ECONOMIQUE(1, "Transaction lente, frais faible"),
    STANDARD(2, "Transaction normale, frais moyens"),
    RAPID(3, "Transaction rapide, frais élevés");

    private final int multiplier;
    private final String description;

    FeeLevel(int multiplier, String description){
        this.multiplier = multiplier;
        this.description = description;
    }

    public int getMultiplier(){ return multiplier; }
    public String getDescription(){ return description; }
}
