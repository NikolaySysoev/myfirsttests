package models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApiError {
    DEPOSIT_LOWER_BOUNDARY("Deposit amount must be at least 0.01"),
    DEPOSIT_HIGHER_BOUNDARY("Deposit amount cannot exceed 5000"),
    DEPOSIT_FORBIDDEN("Unauthorized access to account"),
    TRANSFER_LOWER_BOUNDARY("Transfer amount must be at least 0.01"),
    TRANSFER_HIGHER_BOUNDARY("Transfer amount cannot exceed 10000"),
    TRANSFER_INSUFFICIENT_FUNDS("Invalid transfer: insufficient funds or invalid accounts"),
    CHANGE_NAME_ERROR("Name must contain two words with letters only")
    ;

    private final String message;

}
