package imagesaver_swing;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;

public class EncryptionUtil {

	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

	// Generate a secret key for AES encryption
	public static SecretKey generateKey() throws Exception {
		KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
		keyGen.init(128);
		return keyGen.generateKey();
	}

	public static byte[] encrypt(byte[] input, SecretKey key) throws Exception {
		Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(input);
	}

	public static byte[] decrypt(byte[] input, SecretKey key) throws Exception {
		Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(input);
	}

	public static SecretKey loadKey(String keyFilePath) throws Exception {
		File keyFile = new File(keyFilePath);
		if (!keyFile.exists()) {
			// If the key file doesn't exist, generate a new key and save it
			SecretKey key = generateKey();
			saveKey(key, keyFilePath);
			return key;
		}

		try {
			byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
			String keyString = new String(keyBytes, StandardCharsets.UTF_8).trim();
			byte[] decodedKey = Base64.getDecoder().decode(keyString);
			return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
		} catch (IllegalArgumentException e) {
			// If decoding fails, generate a new key and save it
			SecretKey key = generateKey();
			saveKey(key, keyFilePath);
			return key;
		}
	}

	private static void saveKey(SecretKey key, String keyFilePath) throws IOException {
		byte[] keyBytes = key.getEncoded();
		String encodedKey = Base64.getEncoder().encodeToString(keyBytes);
		Files.write(Paths.get(keyFilePath), encodedKey.getBytes(StandardCharsets.UTF_8));
	}
}
