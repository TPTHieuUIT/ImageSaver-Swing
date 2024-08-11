/*
 * Copyright (c) 2012-2024 by Zalo Group.
 * All Rights Reserved.
 */
package imagesaver_swing;

import java.io.File;
import javax.crypto.SecretKey;
import javax.swing.ImageIcon;
import imagesaver_swing.EncryptionUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import javax.imageio.ImageIO;
/**
 *
 * @author lap14604
 */
public class ImageLoaderService {
    public ImageIcon loadAndDecryptImage(File file) throws Exception {
        // Load the encryption key
        SecretKey key = EncryptionUtil.loadKey(Utils.encryptionFile);

        // Read the encrypted image data
        byte[] encryptedData = Files.readAllBytes(file.toPath());

        // Decrypt the image data
        byte[] decryptedData = EncryptionUtil.decrypt(encryptedData, key);

        // Convert to ImageIcon
        try (ByteArrayInputStream bis = new ByteArrayInputStream(decryptedData)) {
            BufferedImage bufferedImage = ImageIO.read(bis);
            if (bufferedImage == null) {
                throw new IOException("Failed to read image data");
            }
            return new ImageIcon(bufferedImage);
        }
    }
}
