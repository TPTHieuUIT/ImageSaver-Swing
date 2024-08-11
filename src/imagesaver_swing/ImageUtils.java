package imagesaver_swing;

import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

public class ImageUtils {
    public static final int MAX_WIDTH = 413;
    public static final int MAX_HEIGHT = 169;

    public static BufferedImage resizeImage(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate the scaling factor to fit within MAX_WIDTH and MAX_HEIGHT
        double scale = Math.min((double) MAX_WIDTH / originalWidth, (double) MAX_HEIGHT / originalHeight);

        // New width and height
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // Create a new BufferedImage with the calculated dimensions
        Image resultingImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);

        return outputImage;
    }
}