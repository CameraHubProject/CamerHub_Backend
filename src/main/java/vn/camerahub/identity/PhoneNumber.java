package vn.camerahub.identity;

/** Canonical Vietnamese mobile number used for sign-in, not proof of ownership. */
public final class PhoneNumber {
    private PhoneNumber() {}
    public static String normalize(String value) {
        if (value == null) return null;
        String number = value.replaceAll("[\\s().-]", "");
        if (number.matches("0[35789][0-9]{8}")) return "+84" + number.substring(1);
        if (number.matches("84[35789][0-9]{8}")) return "+" + number;
        if (number.matches("\\+84[35789][0-9]{8}")) return number;
        return null;
    }
}
