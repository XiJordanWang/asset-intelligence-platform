package io.github.xijordanwang.assetintel.common;

/** A request that is well formed but breaks a business rule, such as approving a rejected request. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
