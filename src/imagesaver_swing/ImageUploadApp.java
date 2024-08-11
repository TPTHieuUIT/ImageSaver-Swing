
import imagesaver_swing.FilesStorageService;
import imagesaver_swing.ImageLoaderService;
import imagesaver_swing.ImageUtils;
import imagesaver_swing.Utils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import sun.misc.Signal;

public class ImageUploadApp extends JFrame {

	private JPanel imageListPanel;
	ImageLoaderService imageLoaderService = new ImageLoaderService();
	private FilesStorageService storageService;
	private Stack<File> deletedFilesStack;  // Stack to store deleted files for undo

	public ImageUploadApp() {
		try {
			setTitle("Image Upload App");
			setSize(800, 600);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setLayout(new BorderLayout());

			this.storageService = new FilesStorageService();
			deletedFilesStack = new Stack<>();  // Initialize the stack

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

			JButton uploadButton = new JButton("Upload Image");
			uploadButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					uploadImage();
				}
			});
			JButton undoButton = new JButton("Undo");
			undoButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					undoDelete();
				}
			});

			buttonPanel.add(uploadButton);
			buttonPanel.add(undoButton);

			imageListPanel = new JPanel();
			imageListPanel.setLayout(new BoxLayout(imageListPanel, BoxLayout.Y_AXIS));

			JScrollPane scrollPane = new JScrollPane(imageListPanel);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

			add(buttonPanel, BorderLayout.NORTH);
			add(scrollPane, BorderLayout.CENTER);

// Load existing images at startup
			loadExistingImages();
			deletedFilesStack = new Stack<>();

// Ensure temp directory exists
			if (!Utils.tempDir.exists()) {
				Utils.tempDir.mkdir();
			}

// Add a shutdown hook to clean up the temporary directory
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					cleanupTempDirectory();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}));
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	private void loadExistingImages() {
		File uploadDir = new File(Utils.uploadDir);
		if (uploadDir.exists() && uploadDir.isDirectory()) {
			File[] files = uploadDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
			if (files != null) {
				for (File file : files) {
					try {
						ImageIcon icon = imageLoaderService.loadAndDecryptImage(file);
						JPanel imagePanel = createImagePanel(icon, file);
						imageListPanel.add(imagePanel);
						deletedFilesStack.add(file);
					} catch (Exception e) {
						e.printStackTrace();
						// Optionally, you can show an error message for each failed image load
						// JOptionPane.showMessageDialog(this, "Error loading image: " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
				imageListPanel.revalidate();
				imageListPanel.repaint();
			}
		}
	}

	private void cleanupTempDirectory() throws IOException {
		if (Utils.tempDir.exists() && Utils.tempDir.isDirectory()) {
			File[] files = Utils.tempDir.listFiles();
			if (files != null) {
				for (File file : files) {
					Files.delete(file.toPath());
				}
			}
			Files.delete(Utils.tempDir.toPath()); // Delete the directory itself
		}
	}

	private void modifyImage(File file, JLabel nameLabel, JLabel dateLabel) throws IOException, SQLException {
		// Prompt user for new image name
		String newName = JOptionPane.showInputDialog(this, "Enter new image name:", file.getName());

		if (newName != null && !newName.trim().isEmpty()) {
			// Get the file extension
			String fileExtension = getFileExtension(file);
			if (!newName.endsWith(fileExtension)) {
				newName += fileExtension;
			}

			// Rename the file on the filesystem
			File newFile = new File(file.getParent(), newName);
			if (file.renameTo(newFile)) {
				// Update the name label in the UI
				nameLabel.setText(newFile.getName());

				// Update the last modified date
				newFile.setLastModified(System.currentTimeMillis());
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				dateLabel.setText(sdf.format(new Date(newFile.lastModified())));

				// Update the record in the database
				storageService.updateImageRecord(file, newFile);

				// Update the reference to the file
				file = newFile;  // Update the reference to point to the renamed file
			} else {
				JOptionPane.showMessageDialog(this, "Failed to rename the file.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void uploadImage() {
		JFileChooser fileChooser = new JFileChooser();
		FileFilter filter = new FileNameExtensionFilter("jpg", "png", "svg", "gif", "jpeg");
		fileChooser.setFileFilter(filter);
		fileChooser.setMultiSelectionEnabled(true);
		int result = fileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			for (File selectedFile : fileChooser.getSelectedFiles()) {
				try {
					// Save and encrypt the image
					File savedFile = storageService.saveFile(selectedFile);
					deletedFilesStack.add(savedFile);

					// Load and decrypt the image
					ImageIcon icon = imageLoaderService.loadAndDecryptImage(savedFile);

					// Create a panel for this image and its buttons
					JPanel imagePanel = createImagePanel(icon, savedFile);

					// Add the new panel to the image list
					imageListPanel.add(imagePanel);
					imageListPanel.revalidate();
					imageListPanel.repaint();
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(this, "Error uploading image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	private JPanel createImagePanel(ImageIcon icon, File file) {
		try {
			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(1, 4, 1, 1)); // Updated to 4 columns for the delete button

			// Add the image
			JLabel imageLabel = new JLabel(icon);
			panel.add(imageLabel, BorderLayout.CENTER);

			JLabel nameLabel = new JLabel(file.getName());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			JLabel dateLabel = new JLabel(sdf.format(new Date(file.lastModified())));
			panel.add(nameLabel);
			panel.add(dateLabel);
			// Create a panel for buttons
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.insets = new Insets(5, 0, 5, 0);
			gbc.anchor = GridBagConstraints.CENTER;

			final File[] fileHolder = {file};
			// Add modify button
			JButton modifyButton = new JButton("Modify");
			modifyButton.addActionListener(e -> {
				try {
					modifyImage(fileHolder[0], nameLabel, dateLabel);
					fileHolder[0] = new File(fileHolder[0].getParent(), nameLabel.getText());
				} catch (IOException | SQLException ex) {
					ex.printStackTrace();
				}
			});
			modifyButton.setPreferredSize(new Dimension(100, 30));

			// Add delete button
			JButton deleteButton = new JButton("Delete");
			deleteButton.setMnemonic(KeyEvent.VK_H);
			deleteButton.addActionListener(e -> {
				try {
					deleteImage(fileHolder[0]);
					imageListPanel.remove(panel);
					imageListPanel.revalidate();
					imageListPanel.repaint();
				} catch (IOException | SQLException ex) {
					ex.printStackTrace();
				}
			});
			deleteButton.setPreferredSize(new Dimension(100, 30));
			buttonPanel.add(modifyButton, gbc);
			gbc.gridy = 1;
			buttonPanel.add(deleteButton, gbc);
			panel.add(buttonPanel, BorderLayout.SOUTH);

			return panel;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.err.println("Can not create image panel");
		Signal.raise(new Signal("SIGKILL"));
		return null;
	}

	private String getFileExtension(File file) {
		String fileName = file.getName();
		int dotIndex = fileName.lastIndexOf(".");
		if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
			return fileName.substring(dotIndex);
		}
		return "";
	}

	private void loadImages() {
		File[] files = storageService.loadAll();
		if (files != null) {
			for (File file : files) {
				addImageToPanel(file);
			}
		}
	}

	private void addImageToPanel(File file) {
		try {
			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(1, 4, 1, 1));

			BufferedImage image = ImageIO.read(file);
			Image scaledImage = ImageUtils.resizeImage(image);
			JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));

			JLabel nameLabel = new JLabel(file.getName());

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			JLabel dateLabel = new JLabel(sdf.format(new Date(file.lastModified())));

			// Use an array to hold the File reference
			final File[] fileHolder = {file};

			JPanel buttonPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.insets = new Insets(5, 0, 5, 0);
			gbc.anchor = GridBagConstraints.CENTER;

			JButton modifyButton = new JButton("Modify");
			modifyButton.setPreferredSize(new Dimension(100, 30));
			modifyButton.addActionListener(e -> {
				try {
					// Modify the image and update the file reference
					modifyImage(fileHolder[0], nameLabel, dateLabel);
					fileHolder[0] = new File(fileHolder[0].getParent(), nameLabel.getText());
				} catch (IOException | SQLException ex) {
					ex.printStackTrace();
				}
			});

			JButton deleteButton = new JButton("Delete");
			deleteButton.setPreferredSize(new Dimension(100, 30));
			deleteButton.addActionListener(e -> {
				try {
					// Use the updated file reference for deletion
					deleteImage(fileHolder[0]);
					imageListPanel.remove(panel);
					imageListPanel.revalidate();
					imageListPanel.repaint();
				} catch (IOException | SQLException ex) {
					ex.printStackTrace();
				}
			});

			buttonPanel.add(modifyButton, gbc);
			gbc.gridy = 1;
			buttonPanel.add(deleteButton, gbc);

			panel.add(imageLabel);
			panel.add(nameLabel);
			panel.add(dateLabel);
			panel.add(buttonPanel);

			imageListPanel.add(panel);
			imageListPanel.revalidate();
			imageListPanel.repaint();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void deleteImage(File file) throws IOException, SQLException {
		// Ensure temp directory exists
		if (!Utils.tempDir.exists()) {
			Utils.tempDir.mkdir();
		}

		// Move the file to the temporary directory instead of deleting it
		Path tempFilePath = new File(Utils.tempDir, file.getName()).toPath();
		Files.move(file.toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

		// Save deleted file info for undo
		deletedFilesStack.push(tempFilePath.toFile());

		// Delete record from database
		storageService.deleteImageRecord(file);
	}

	private void undoDelete() {
		if (!deletedFilesStack.isEmpty()) {
			File lastDeletedFile = deletedFilesStack.pop();
			File restoredFile = new File(Utils.uploadDir, lastDeletedFile.getName());

			try {
				// Move the file back from the temporary directory
				Files.move(lastDeletedFile.toPath(), restoredFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

				// Re-add the image to the database and panel
				storageService.saveFile(restoredFile);
				addImageToPanel(restoredFile);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			JOptionPane.showMessageDialog(this, "No file to undo!", "Undo", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			ImageUploadApp app = new ImageUploadApp();
			app.setVisible(true);
		});
	}
}
