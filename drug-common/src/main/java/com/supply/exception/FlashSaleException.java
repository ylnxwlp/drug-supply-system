package com.supply.exception;

public class FlashSaleException extends RuntimeException{
    public FlashSaleException() {
        super();
    }

    public FlashSaleException(String message) {
        super(message);
    }

    public FlashSaleException(String message, Throwable cause) {
        super(message, cause);
    }

    public FlashSaleException(Throwable cause) {
        super(cause);
    }

    protected FlashSaleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
