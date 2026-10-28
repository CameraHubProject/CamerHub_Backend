package vn.camerahub.marketplace.util;

import vn.camerahub.marketplace.exception.*;

import java.util.List;

import org.springframework.http.HttpStatus;


public final class CameraBrands {
    public static final List<String> NAMES = List.of(
            "Canon", "Sony", "Nikon", "Fujifilm", "Panasonic", "Olympus", "OM System", "Pentax",
            "Ricoh", "Leica", "Kodak", "Casio", "Samsung", "Minolta", "Hasselblad", "Sigma",
            "GoPro", "DJI", "Insta360", "Polaroid", "Phase One", "Mamiya", "Yashica", "Konica",
            "Rollei", "Voigtländer", "Lomography", "Blackmagic Design", "RED", "ARRI", "Z CAM",
            "Kinefinity", "AKASO", "SJCAM", "Khác");

    private CameraBrands() {
    }

    public static String resolve(String selected, String custom) {
        return resolve(selected, custom, NAMES);
    }

    public static String resolve(String selected, String custom, List<String> available) {
        if (selected == null || (!"Khác".equals(selected) && !available.contains(selected)))
            throw new BrandException(MarketplaceErrorCode.BRAND_LIST_CHANGED);
        if (!"Khác".equals(selected)) {
            if (custom != null && !custom.isBlank())
                throw new BrandException(MarketplaceErrorCode.CUSTOM_BRAND_NOT_ALLOWED);
            return selected;
        }
        String name = normalizeName(custom);
        return available.stream().filter(brand -> !brand.equals("Khác") && brand.equalsIgnoreCase(name)).findFirst().orElse(name);
    }

    public static String normalizeName(String value) {
        String name = value == null ? "" : value.replaceAll("[\\p{Z}\\s]+", " ").strip();
        if (name.isBlank() || "Khác".equalsIgnoreCase(name))
            throw new BrandException(MarketplaceErrorCode.CUSTOM_BRAND_REQUIRED);
        if (name.length() > 100) throw new BrandException(MarketplaceErrorCode.BRAND_NAME_TOO_LONG);
        if (name.codePoints().anyMatch(Character::isISOControl))
            throw new BrandException(MarketplaceErrorCode.INVALID_BRAND_NAME);
        return name;
    }

    public static int rank(String name) {
        int index = NAMES.indexOf(name);
        return index < 0 ? NAMES.size() : index;
    }
}

