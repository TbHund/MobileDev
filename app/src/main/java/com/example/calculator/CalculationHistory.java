package com.example.calculator;

public class CalculationHistory {
    private String expression;
    private String result;
    private long timestamp;

    public CalculationHistory() {}

    public CalculationHistory(String expression, String result, long timestamp) {
        this.expression = expression;
        this.result = result;
        this.timestamp = timestamp;
    }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
