import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * Generates a stable RSA key pair once for the unified backend.
 */
public class GenerateJwtKeys {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path secret = directory.resolve("private.pem"), publicFile = directory.resolve("public.pem");
        var factory = KeyFactory.getInstance("RSA");
        RSAPublicKey publicKey;
        if (Files.exists(secret)) {
            var encoded = Files.readString(secret).replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
            var key = (RSAPrivateCrtKey) factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
            publicKey = (RSAPublicKey) factory.generatePublic(new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent()));
        } else {
            if (Files.exists(publicFile))
                throw new IllegalStateException("Private key missing; refusing to replace an existing public key");
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);
            var pair = generator.generateKeyPair();
            publicKey = (RSAPublicKey) pair.getPublic();
            Files.writeString(secret, pem("PRIVATE KEY", pair.getPrivate().getEncoded()), StandardOpenOption.CREATE_NEW);
        }
        if (Files.exists(publicFile)) {
            String encoded = Files.readString(publicFile).replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
            if (!java.util.Arrays.equals(Base64.getDecoder().decode(encoded), publicKey.getEncoded()))
                throw new IllegalStateException("JWT public/private keys do not match");
        } else Files.writeString(publicFile, pem("PUBLIC KEY", publicKey.getEncoded()), StandardOpenOption.CREATE_NEW);
        System.out.println("JWT RSA key pair ready.");
    }

    private static String pem(String label, byte[] data) {
        return "-----BEGIN " + label + "-----\n" + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(data) + "\n-----END " + label + "-----\n";
    }
}
