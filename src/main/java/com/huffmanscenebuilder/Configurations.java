package com.huffmanscenebuilder;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;

public class Configurations {
    public static File compressFile;
    public static File deCompressFile;

    public static void showAlert(Alert.AlertType alertType, String title, String content) {
        // Step 1: Create a new Alert object with the specified alert type
        Alert alert = new Alert(alertType);

        // Step 2: Set the title of the alert dialog
        alert.setTitle(title);

        // Step 3: Disable the header text for a cleaner appearance
        alert.setHeaderText(null);

        // Step 4: Set the main content of the alert dialog with the provided message
        alert.setContentText(content);

        // Step 5: Display the alert and wait for the user to respond
        alert.showAndWait();
    }

    public static String replaceExtension(String fileName, String newExtension){
        // Find the last dot (.) position to locate the extension.
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            // If there's no extension, simply append the new extension.
            return fileName + "." + newExtension;
        }
        // Replace the old extension with the new extension.
        return fileName.substring(0, lastDotIndex) + "." + newExtension;
    }

    public static void openFolder(String path) {
        try {
            // Step 1: Get the Desktop object to interact with the system's desktop environment
            Desktop desktop = Desktop.getDesktop();

            // Step 2: Create a File object pointing to the specified path
            File directory = new File(path);

            // Step 3: Check if the directory exists and is indeed a directory
            if (directory.exists() || directory.isDirectory()) {
                // Use the Desktop API to open the directory in the file explorer
                desktop.open(directory);
            } else {
                // Show an error alert if the directory does not exist or is not a directory
                showAlert(Alert.AlertType.ERROR, "Error", "The specified folder does not exist.");
            }
        } catch (Exception e) {
            // Handle exceptions such as unsupported platforms or access issues
            showAlert(
                    Alert.AlertType.ERROR, // Type of alert: ERROR
                    "Error",               // Alert title
                    "Can't open the folder. Copy the path and open it manually." // Detailed error message
            );
        }
    }

    public static void handleShowHeader(StringBuilder headerToShow) {
        // Step 1: Copy the original header string for processing
        StringBuilder header = new StringBuilder(headerToShow);

        // Step 2: Create a ScrollPane for displaying content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true); // Ensure the content adjusts to the scroll pane's width

        // Step 3: Create a VBox layout container with spacing and modern styling
        javafx.scene.layout.VBox contentContainer = new javafx.scene.layout.VBox(20); // Spacing of 20px between elements
        contentContainer.setPadding(new Insets(20)); // Add padding around the container
        contentContainer.setStyle("-fx-background-color: #1e1e2f; " +
                "-fx-border-color: #4caf50; " +
                "-fx-border-width: 2px; " +
                "-fx-padding: 10px;");

        // Step 4: Create separate sections for statistics and binary tree traversal

        // Statistics Section
        javafx.scene.layout.VBox statisticsContainer = new javafx.scene.layout.VBox(15);
        statisticsContainer.setStyle("-fx-background-color: #2e3440; -fx-padding: 15px; -fx-border-radius: 5px; -fx-background-radius: 5px;");

        javafx.scene.control.Label statisticsLabel = new javafx.scene.control.Label("Header Statistics");
        statisticsLabel.setStyle("-fx-text-fill: #81a1c1; -fx-font-size: 20px; -fx-font-weight: bold;");

        javafx.scene.control.Label extensionBitsLabel = new javafx.scene.control.Label("Extension in Bits:");
        extensionBitsLabel.setStyle("-fx-text-fill: #eceff4; -fx-font-size: 16px;");

        javafx.scene.control.TextField extensionBitsField = new javafx.scene.control.TextField();
        extensionBitsField.setEditable(false);

        javafx.scene.control.Label extensionCharsLabel = new javafx.scene.control.Label("Extension in Characters:");
        extensionCharsLabel.setStyle("-fx-text-fill: #eceff4; -fx-font-size: 16px;");

        javafx.scene.control.TextField extensionCharsField = new javafx.scene.control.TextField();
        extensionCharsField.setEditable(false);

        javafx.scene.control.Label sizeBitsLabel = new javafx.scene.control.Label("Size of Header in Bits:");
        sizeBitsLabel.setStyle("-fx-text-fill: #eceff4; -fx-font-size: 16px;");

        javafx.scene.control.TextField sizeBitsField = new javafx.scene.control.TextField();
        sizeBitsField.setEditable(false);

        javafx.scene.control.Label sizeIntLabel = new javafx.scene.control.Label("Size of Header in Integer:");
        sizeIntLabel.setStyle("-fx-text-fill: #eceff4; -fx-font-size: 16px;");

        javafx.scene.control.TextField sizeIntField = new javafx.scene.control.TextField();
        sizeIntField.setEditable(false);

        String extensionInbitString = ""; // Bits representing the file extension
        String extensionInCharString = ""; // Characters representing the file extension
        for (int i = 0; i < 8; i++) {
            extensionInCharString += (char) Integer.parseInt(header.substring(i * 8, (i + 1) * 8), 2);
            extensionInbitString += header.substring(i * 8, (i + 1) * 8) + " ";
        }
        header.delete(0, 64); // Remove the processed bits from the header

        extensionBitsField.setText(extensionInbitString);
        extensionCharsField.setText(extensionInCharString);

        String sizeBits = header.substring(0, 32);
        int sizeOfHeader = Integer.parseInt(sizeBits, 2);
        header.delete(0, 32); // Remove the processed size bits

        sizeBitsField.setText(sizeBits);
        sizeIntField.setText(String.valueOf(sizeOfHeader));

        statisticsContainer.getChildren().addAll(
                statisticsLabel,
                extensionBitsLabel, extensionBitsField,
                extensionCharsLabel, extensionCharsField,
                sizeBitsLabel, sizeBitsField,
                sizeIntLabel, sizeIntField
        );

        // Binary Tree Section
        javafx.scene.layout.VBox treeContainer = new javafx.scene.layout.VBox(15);
        treeContainer.setStyle("-fx-background-color: #3b4252; -fx-padding: 15px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        treeContainer.setPrefWidth(1200); // Enlarged width to take the left part of the stage

        javafx.scene.control.Label treeLabel = new javafx.scene.control.Label("Binary Tree Traversal");
        treeLabel.setStyle("-fx-text-fill: #88c0d0; -fx-font-size: 20px; -fx-font-weight: bold;");

        javafx.scene.control.TextArea treeDetailsArea = new javafx.scene.control.TextArea();
        treeDetailsArea.setEditable(false);
        treeDetailsArea.setStyle("-fx-text-fill: #2e3440; -fx-font-size: 16px; -fx-font-family: 'Consolas'; -fx-font-weight: bold;"); // Dark text for clarity
        treeDetailsArea.setWrapText(true);
        treeDetailsArea.setPrefHeight(385);

        int counter = 0, numberOfExtraBit = 8 - sizeOfHeader % 8;
        while (counter < sizeOfHeader) {
            if (header.charAt(counter) == '0') {
                counter++;
                treeDetailsArea.appendText("Leaf Node: 0 " + header.substring(counter, counter + 8) + "\n");
                counter += 8;
            } else {
                counter++;
                treeDetailsArea.appendText("Internal Node: 1\n");
            }
        }

        if (numberOfExtraBit == 8) {
            treeDetailsArea.appendText("The size of the header is divisible by 8. No extra bit added.\n");
        } else {
            treeDetailsArea.appendText("Extra bit: " + header.substring(counter, counter + numberOfExtraBit) + "\n");
        }

        treeContainer.getChildren().addAll(treeLabel, treeDetailsArea);

        // Add both sections to the main container
        contentContainer.getChildren().addAll(statisticsContainer, treeContainer);

        // Step 5: Add the content container to the ScrollPane
        scrollPane.setContent(contentContainer);

        // Step 6: Create and display the scene
        Scene scene = new Scene(scrollPane, 1400, 1000); // Create a new scene with specified dimensions
        Stage newStage = new Stage(); // Create a new stage (window)
        newStage.setScene(scene); // Set the scene for the stage
        newStage.setTitle("Header Viewer"); // Set the title of the window
        newStage.show(); // Display the window
    }



    public static String byteToBinaryString(byte b) {
        // Create a StringBuilder to construct the binary string
        StringBuilder binaryString = new StringBuilder();

        // Iterate from the most significant bit (7) to the least significant bit (0)
        for (int i = 7; i >= 0; i--) {
            // Right shift the byte `i` positions and mask the least significant bit with 1
            int bit = (b >> i) & 1;
            // Append the extracted bit to the binary string
            binaryString.append(bit);
        }
        // Return the constructed binary string
        return binaryString.toString();
    }

    private static TextFlow creatTextFlow(String label, String content, String color) {
        // Create a Text object for the label with bold white text
        Text labelText = new Text(label);
        labelText.setStyle("-fx-fill: #ffffff; -fx-font-weight: bold;"); // White text with bold font
        labelText.setFont(Font.font("Arial", 18)); // Font style and size

        // Create a Text object for the content with dynamic color
        Text contentText = new Text(content);
        contentText.setStyle("-fx-fill: " + color + ";"); // Use the specified color for text
        contentText.setFont(Font.font("Arial", 18)); // Font style and size

        // Combine the label and content into a TextFlow and return it
        return new TextFlow(labelText, contentText);
    }
}
