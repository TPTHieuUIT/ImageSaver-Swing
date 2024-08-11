package imagesaver_swing;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import imagesaver_swing.Utils;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Date;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class FilesStorageService {

	private SecretKey secretKey;

	public FilesStorageService() throws SQLException {
		// Load the secret key from the database
		this.secretKey = loadSecretKeyFromDB();
//		setupEncryptionKey();
		File directory = new File(Utils.uploadDir);
		if (!directory.exists()) {
			directory.mkdir();
		}
	}
	public void setupEncryptionKey() {
		try {
			// Generate the encryption key
			SecretKey secretKey = EncryptionUtil.generateKey();

			// Convert the key to Base64 for storage
			String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

			// Store the encoded key in your database
			saveSecretKeyToDB(encodedKey);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private SecretKey loadSecretKeyFromDB() throws SQLException {
		SecretKey key = null;
		try (Connection conn = DatabaseConnection.getConnection()) {
			String sql = "SELECT secret_key FROM encryption_keys WHERE key_name = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, "my_secret_key");  // or some specific key name
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						byte[] keyBytes = rs.getBytes("secret_key");
						System.out.println(keyBytes);
						key = new SecretKeySpec(keyBytes, "AES");  // Assuming AES key
					}
				}
			}
		}
		return key;
	}

	public void saveSecretKeyToDB(String secretKey) throws SQLException {
		try (Connection conn = DatabaseConnection.getConnection()) {
			String sql = "INSERT INTO encryption_keys (key_name, secret_key) VALUES (?, ?) ON DUPLICATE KEY UPDATE secret_key = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, "my_secret_key");
				stmt.setString(2, secretKey);
				stmt.setString(3, secretKey);
				stmt.executeUpdate();
			}
		}
	}

    public File saveFile(File file) throws IOException, SQLException {
        try {
            SecretKey key = EncryptionUtil.loadKey(Utils.encryptionFile);
            
            // Read the image file
            BufferedImage originalImage = ImageIO.read(file);
            if (originalImage == null) {
                throw new IOException("Failed to read image file");
            }
            
            // Resize the image
            BufferedImage resizedImage = ImageUtils.resizeImage(originalImage);
            
            // Convert the resized image to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);
            byte[] imageData = baos.toByteArray();
            
            // Encrypt the image data
            byte[] encryptedData = EncryptionUtil.encrypt(imageData, key);
            
            // Generate a unique filename
            String fileName = generateUniqueFileName(file.getName());
            File targetFile = new File(Utils.uploadDir, fileName);
            
            // Ensure the upload directory exists
            targetFile.getParentFile().mkdirs();
            
            // Write the encrypted data to the new file
            Files.write(targetFile.toPath(), encryptedData);
            
            // Save file information to database
            saveFileInfoToDabatase(targetFile);
            
            return targetFile;
        } catch (Exception e) {
            throw new IOException("Failed to save and encrypt file", e);
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String baseName = getBaseName(originalFileName);
        String extension = getExtension(originalFileName);
        String timestamp = String.valueOf(System.currentTimeMillis());
        return baseName + "_" + timestamp + "." + extension;
    }

    private String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

	public void saveFileInfoToDabatase(File file) {
		try (Connection conn = DatabaseConnection.getConnection()) {
			String sql = "INSERT INTO images (name, path) VALUES (?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, file.getName());
				stmt.setString(2, file.getPath());
				stmt.executeUpdate();
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
	
	public void deleteImageRecord(File file) throws SQLException {
		try (Connection conn = DatabaseConnection.getConnection()) {
			String sql = "DELETE FROM images WHERE path = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, file.getPath());
				stmt.executeUpdate();
			}
		}
	}

	public void updateImageRecord(File oldFile, File newFile) throws SQLException {
		try (Connection conn = DatabaseConnection.getConnection()) {
			String sql = "UPDATE images SET name = ?, path = ?, last_modified = ? WHERE path = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setString(1, newFile.getName());
				stmt.setString(2, newFile.getPath());
				stmt.setTimestamp(3, new java.sql.Timestamp(newFile.lastModified()));
				stmt.setString(4, oldFile.getPath());
				stmt.executeUpdate();
			}
		}
	}

	public File[] loadAll() {
		File directory = new File(Utils.uploadDir);
		return directory.listFiles();
	}
}
