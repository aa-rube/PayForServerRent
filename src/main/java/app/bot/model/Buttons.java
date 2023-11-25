package app.bot.model;

public enum Buttons {
    SENT_TO_ADMIN("0");

    private final String number;

    Buttons(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }
}
