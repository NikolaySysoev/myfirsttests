package ui.pages;

import lombok.Getter;

@Getter
public enum BankAlerts {
    USER_DEPOSIT_SUCCESS("✅ Successfully deposited $%s to account %s!"),
    USER_DEPOSIT_FAIL("❌ Please deposit less or equal to 5000$."),
    USER_TRANSFER_SUCCESS("✅ Successfully transferred $%s to account %s!"),
    USER_TRANSFER_FAIL_EMPTY_FORM("❌ Please fill all fields and confirm."),
    USER_TRANSFER_FAIL_INVALID_ACCOUNT("❌ No user found with this account number."),
    USER_TRANSFER_FAIL_INVALID_AMOUNT_LOWER_001("❌ Error: Transfer amount must be at least 0.01"),
    USER_CHANGE_NAME_SUCCESS("✅ Name updated successfully!"),
    USER_CHANGE_NAME_FAIL("Name must contain two words with letters only");


    private final String message;

    BankAlerts(String message) {
        this.message = message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }
}
