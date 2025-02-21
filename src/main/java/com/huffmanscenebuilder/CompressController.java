package com.huffmanscenebuilder;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CompressController {

    @FXML
    private Label waitLabel; // Label to show compression progress messages

    @FXML
    private VBox resultsVbox; // Container to display compression results

    @FXML
    private Label fSizeBLbl; // Label to show the original file size

    @FXML
    private Label fSizeALbl; // Label to show the compressed file size

    @FXML
    private Label compressRatioLbl; // Label to show the compression ratio

    @FXML
    private Button openFolderBt; // Button to open the folder containing the compressed file

    @FXML
    private Button showHeadBt; // Button to display the header information of the compressed file

    @FXML
    private Button showTableBt; // Button to display the Huffman table

    @FXML
    private Button saveButton;

    @FXML
    private Button mainPageBt; // Button to navigate back to the main page

    @FXML
    private ProgressBar progressBar;

    @FXML
    private TextField txtCompressRatio;

    @FXML
    private TextField txtFileBSize;

    @FXML
    private TextField txtFileASize;

    @FXML
    private Label selectedFileText;

    File originalFile = Configurations.compressFile; // Path of the file to be compressed
    private long lengthFB; // Size of the file before compression
    private long lengthFA; // Size of the file after compression
    StringBuilder headerToShow = new StringBuilder(); // Header information to display
    private StringBuilder nameOfCompressedFile;
    private String filePathAfterSave;
    private TreeNode[] bytes;

    @FXML
    private void initialize() {
        compress();
    }

    public void compress() {
        Task<Void> compressTask = createCompressTask();

        // Bind progress and message properties to UI elements
        progressBar.progressProperty().bind(compressTask.progressProperty());
        waitLabel.textProperty().bind(compressTask.messageProperty());

        compressTask.setOnSucceeded(event -> handleCompressionSuccess());
        compressTask.setOnFailed(event -> handleCompressionFailure(compressTask));

        // Start the Task
        new Thread(compressTask).start();
    }

    private Task<Void> createCompressTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Phase 1: Initialize Compression
                    updateMessage("Initializing compression...");
                    updateProgress(0, 1);
                    initializeCompression();

                    // Phase 2: Build Huffman Tree
                    updateMessage("Building Huffman tree...");
                    TreeNode rootTreeNode = buildHuffmanTree();

                    // Phase 3: Create Compressed File
                    updateMessage("Creating compressed file...");
                    createCompressedFile(rootTreeNode);

                    //Phase 4: Compression Completed
                    updateMessage("Compression completed.");
                    updateProgress(1, 1);
                    return null;
                } catch (Exception e) { // Handling any error that might occur
                    updateMessage("Error: " + e.getMessage());
                    throw e;
                }
            }
        };
    }

    private void handleCompressionSuccess() {
        // Unbind the waitLabel property and update its text to indicate success
        waitLabel.textProperty().unbind();
        waitLabel.setText("Compression complete!");

        // Hide and un manage the progress bar
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        // Run UI updates on the JavaFX application thread
        Platform.runLater(() -> {
            // Make the results container visible and managed
            resultsVbox.setVisible(true);
            resultsVbox.setManaged(true);

            // Update and display the original file size
            fSizeBLbl.setVisible(true);
            fSizeBLbl.setText("Original Size:");
            txtFileBSize.setVisible(true);
            txtFileBSize.setText(formatFileSize(lengthFB));
            selectedFileText.setVisible(true);
            selectedFileText.setText("Selected File: " + originalFile.getName());

            // Update and display the compressed file size
            fSizeALbl.setVisible(true);
            fSizeALbl.setText("Compressed Size:");
            txtFileASize.setVisible(true);
            txtFileASize.setText(formatFileSize(lengthFA));

            // Calculate and display the compression ratio
            compressRatioLbl.setVisible(true);
            compressRatioLbl.setText("Compression Ratio:");
            txtCompressRatio.setVisible(true);
            txtCompressRatio.setText(String.format("%.3f%%", (1- (double) lengthFA / lengthFB) * 100));

            // Show buttons for additional actions
            openFolderBt.setVisible(true);
            openFolderBt.setManaged(true);

            showHeadBt.setVisible(true);
            showHeadBt.setManaged(true);

            showTableBt.setVisible(true);
            showTableBt.setManaged(true);

            // Show the save button for saving the compressed file
            saveButton.setVisible(true);
            saveButton.setManaged(true);

            // Set the default message indicating file saved to the project's directory
            waitLabel.setText("File saved to the project's directory by default.");
        });
    }

    private void handleCompressionFailure(Task<Void> compressTask) {
        // Unbind the waitLabel property to stop tracking task messages
        waitLabel.textProperty().unbind();
        // Update the label to indicate compression failure
        waitLabel.setText("Compression failed!");

        // Hide and un manage the progress bar as the task has ended
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        // Display an alert with the error message from the failed compression task
        Configurations.showAlert(Alert.AlertType.ERROR, "Compression Error", compressTask.getException().getMessage());
    }

    private void initializeCompression() throws IOException {
        // Initialize an array of tree nodes to store frequency data for all 256 possible byte values
        bytes = new TreeNode[256];
        for (int i = 0; i < 256; i++) {
            bytes[i] = new TreeNode((byte) i); // Create a TreeNode for each byte value
        }

        // Record the size of the file before compression
        lengthFB = originalFile.length();
        // Check if the file is empty and throw an exception if it is
        if (lengthFB == 0) {
            throw new IllegalArgumentException("File is empty."); // File must not be empty
        }

        // Initialize a small buffer to read the file in chunks (8 bytes at a time)
        byte[] bufferIn = new byte[8];

        // Use try-with-resources to automatically close the FileInputStream after use
        try (FileInputStream in = new FileInputStream(originalFile)) {
            int numBytesInBuffer; // Variable to track the number of bytes read in each iteration
            // Read the file in chunks until all data is processed
            while ((numBytesInBuffer = in.read(bufferIn)) != -1) {
                for (int i = 0; i < numBytesInBuffer; i++) {
                    // Increment the frequency for each byte
                    if (bufferIn[i] < 0)
                        bytes[bufferIn[i] + 256].increment(); // Handle signed bytes (convert to unsigned)
                    else
                        bytes[bufferIn[i]].increment(); // Handle unsigned bytes directly
                }
            }
        }
    }


    private TreeNode buildHuffmanTree() {
        // Create a heap with a capacity of 256 for storing TreeNodes
        Heap heap = new Heap(256);

        // Add all TreeNodes with a non-zero frequency to the heap
        for (TreeNode byteNode : bytes) {
            if (byteNode.getFrequency() != 0) {
                heap.insert(byteNode); // Insert nodes into the heap
            }
        }

        // Build the Huffman tree by combining the two nodes with the smallest frequencies
        while (heap.getSize() > 1) {
            // Remove the two nodes with the smallest frequencies
            TreeNode x = heap.remove();
            TreeNode y = heap.remove();

            // Create a new parent node with the combined frequency of the two nodes
            TreeNode z = new TreeNode(x.getFrequency() + y.getFrequency());
            z.setLeft(x); // Set the left child to the first node
            z.setRight(y); // Set the right child to the second node

            // Insert the new parent node back into the heap
            heap.insert(z);
        }

        // The last remaining node in the heap is the root of the Huffman tree
        TreeNode rootTreeNode = heap.remove();

        // Assign codes to the nodes:
        // If it's a leaf, assign it the code "1"
        if (rootTreeNode.getLeft() == null && rootTreeNode.getRight() == null) {
            rootTreeNode.setCode("1");
        } else {
            // Otherwise, recursively assign Huffman codes to each leaf node
            TreeNode.giveCodeForEachByte(rootTreeNode);
        }

        // Return the root of the Huffman tree
        return rootTreeNode;
    }

    private void createCompressedFile(TreeNode rootTreeNode) throws IOException {
        // Generate the header for the Huffman tree using its traversal
        StringBuilder header = new StringBuilder(rootTreeNode.traverse());
        // Extract the file extension from the original file path
        String fileExtension = originalFile.getName().substring(originalFile.getName().lastIndexOf(".") + 1);

        // Buffer for storing the file extension (8 bytes maximum)
        byte[] bufferIn = new byte[8];
        for (int i = 0; i < bufferIn.length; i++) {
            if (i < fileExtension.length()) {
                bufferIn[i] = (byte) fileExtension.charAt(i); // Store the file extension
                headerToShow.append(Configurations.byteToBinaryString(bufferIn[i])); // Append binary representation
            } else {
                bufferIn[i] = (byte) 0; // Fill unused bytes with 0
                headerToShow.append("00000000");
            }
        }

        // Buffer to store the size of the header (4 bytes for an integer)
        byte[] bufferForHeaderSize = {
                (byte) (header.length() >> 24),
                (byte) (header.length() >> 16),
                (byte) (header.length() >> 8),
                (byte) header.length()
        };
        for (byte b : bufferForHeaderSize) {
            headerToShow.append(Configurations.byteToBinaryString(b)); // Append binary representation
        }

        // Pad the header to make its length a multiple of 8 bits
        if (header.length() % 8 != 0) {
            int paddingLength = 8 - header.length() % 8;
            for (int i = 0; i < paddingLength; i++) {
                header.append("0");
            }
        }
        headerToShow.append(header); // Append the complete header to the visualization

        // Generate the output file name with ".huff" extension
        nameOfCompressedFile = new StringBuilder(Configurations.replaceExtension(originalFile.getName(), "huff"));
        File tempFile = new File(System.getProperty("user.dir"), nameOfCompressedFile.toString());

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            // Write the file extension and header size to the compressed file
            out.write(bufferIn);
            out.write(bufferForHeaderSize);

            // Write the header to the compressed file
            int numOfBytes = header.length() / 8;
            for (int i = 0; i < numOfBytes; i++) {
                String byteString = header.substring(i * 8, (i + 1) * 8);
                bufferIn[i % 8] = (byte) Integer.parseInt(byteString, 2);
                if (i % 8 == 7) {
                    out.write(bufferIn); // Write full chunks
                }
            }
            if (numOfBytes % 8 != 0) {
                out.write(bufferIn, 0, numOfBytes % 8); // Write remaining bytes
            }

            // Initialize a buffer for the Huffman-encoded data
            StringBuilder data = new StringBuilder();
            byte[] bufferOut = new byte[8]; // Output buffer
            int bufferLength;

            // Read the input file and encode it using Huffman codes
            try (FileInputStream in = new FileInputStream(originalFile)) {
                while ((bufferLength = in.read(bufferIn)) != -1) {
                    for (int k = 0; k < bufferLength; k++) {
                        // Append the Huffman code for each byte
                        if (bufferIn[k] < 0) {
                            data.append(bytes[bufferIn[k] + 256].getCode());
                        } else {
                            data.append(bytes[bufferIn[k]].getCode());
                        }

                        // Write full bytes from the binary string
                        while (data.length() >= 8) {
                            bufferOut[0] = (byte) Integer.parseInt(data.substring(0, 8), 2);
                            out.write(bufferOut, 0, 1);
                            data.delete(0, 8);
                        }
                    }
                }
            }

            // Handle remaining bits
            int numberOfEffectiveBits = data.length();
            if (numberOfEffectiveBits % 8 != 0) {
                int paddingLength = 8 - (numberOfEffectiveBits % 8);
                for (int i = 0; i < paddingLength; i++) {
                    data.append("0");
                }
            }
            while (data.length() >= 8) {
                bufferOut[0] = (byte) Integer.parseInt(data.substring(0, 8), 2);
                out.write(bufferOut, 0, 1);
                data.delete(0, 8);
            }

            // Write the number of effective bits in the last byte
            out.write((byte) (numberOfEffectiveBits % 8 == 0 ? 8 : numberOfEffectiveBits % 8));

            // Update the size of the compressed file
            lengthFA = tempFile.length();
            filePathAfterSave = tempFile.getAbsolutePath();
        } catch (Exception e) {
            throw e; // Propagate exceptions for handling
        }
    }

    // Helper method to format file size using decimal units (Base 1000 same as the laptop showing. If we wnt to represent it from binary base case 1024.)
    private String formatFileSize(long size) {
        if (size < 1000) {
            return size + " Bytes"; // Bytes
        } else if (size < 1000 * 1000) {
            return String.format("%.2f KB", size / 1000.0); // Kilobytes
        } else if (size < 1000 * 1000 * 1000) {
            return String.format("%.2f MB", size / (1000.0 * 1000)); // Megabytes
        } else if (size < 1000L * 1000 * 1000 * 1000) {
            return String.format("%.2f GB", size / (1000.0 * 1000 * 1000)); // Gigabytes
        } else {
            return String.format("%.2f TB", size / (1000.0 * 1000 * 1000 * 1000)); // Terabytes
        }
    }

    private void showHuffmanTable() {
        // Create a scrollable pane to contain the table and its layout
        ScrollPane subScenePane = new ScrollPane();
        subScenePane.setFitToWidth(true); // Ensures the content adjusts to the width of the scroll pane
        subScenePane.setFitToHeight(true);

        // Create a horizontal box layout for centering and spacing content
        HBox hBox = new HBox(40); // 40 px spacing between elements
        hBox.setAlignment(Pos.CENTER); // Align elements to the center
        hBox.setPadding(new Insets(20)); // Add padding inside the box
        hBox.setStyle("-fx-background-color: #2b2b3b;"); // Set background color for the HBox

        // Create a TableView to display Huffman table data
        TableView<TreeNode> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // Ensures columns adjust to available space
        tableView.setPrefSize(900, 600); // Set preferred size for the table
        tableView.setStyle("-fx-background-color: #2b2b3b; -fx-border-color: #444444;"); // Set table background and border

        // Define the Byte column to display byte content
        TableColumn<TreeNode, String> byteColumn = new TableColumn<>("Byte");
        byteColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                String.valueOf(cellData.getValue().getByteContent() & 0xFF) // Convert signed byte to unsigned
        ));
        byteColumn.setStyle("-fx-alignment: CENTER; -fx-font-size: 16px; -fx-text-fill: #ffffff;"); // Styling for the column

        // Define the Huffman Code column
        TableColumn<TreeNode, String> huffmanColumn = new TableColumn<>("Huffman Code");
        huffmanColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCode())); // Get Huffman code
        huffmanColumn.setStyle("-fx-alignment: CENTER; -fx-font-size: 16px; -fx-text-fill: #ffffff;");

        // Define the Frequency column to show frequency of each byte
        TableColumn<TreeNode, Integer> frequencyColumn = new TableColumn<>("Frequency");
        frequencyColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getFrequency()).asObject());
        frequencyColumn.setStyle("-fx-alignment: CENTER; -fx-font-size: 16px; -fx-text-fill: #ffffff;");

        // Define the Code Length column to display the length of the Huffman code
        TableColumn<TreeNode, Integer> lengthColumn = new TableColumn<>("Code Length");
        lengthColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getCode().length()).asObject());
        lengthColumn.setStyle("-fx-alignment: CENTER; -fx-font-size: 16px; -fx-text-fill: #ffffff;");

        // Add all columns to the TableView
        tableView.getColumns().addAll(byteColumn, huffmanColumn, frequencyColumn, lengthColumn);

        // Populate the TableView with data from the TreeNode array
        ObservableList<TreeNode> data = FXCollections.observableArrayList();
        for (TreeNode node : bytes) {
            if (node.getCode() != null) { // Only add nodes with valid Huffman codes
                data.add(node);
            }
        }
        tableView.setItems(data); // Set the data in the TableView

        // Create a label to display the table header
        Label huffmanLabel = new Label("Huffman Table");
        huffmanLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 20)); // Set font style and size
        huffmanLabel.setStyle("-fx-text-fill: #ffa726;"); // Set text color

        // Create a vertical box to hold the header and the table
        VBox contentContainer = new VBox(20); // 20 px spacing between elements
        contentContainer.setAlignment(Pos.CENTER); // Align elements to the center
        contentContainer.setPadding(new Insets(20)); // Add padding inside the box
        contentContainer.setStyle("-fx-background-color: #2b2b3b; " + // Set background color
                "-fx-border-color: #444444; " + // Set border color
                "-fx-border-width: 2px;"); // Set border width
        contentContainer.getChildren().addAll(huffmanLabel, tableView); // Add header and table to the VBox

        // Add the VBox to the HBox
        hBox.getChildren().add(contentContainer);

        // Set the HBox as the content of the ScrollPane
        subScenePane.setContent(hBox);

        // Create a new scene and stage to display the Huffman table
        Scene scene = new Scene(subScenePane, 1500, 1300); // Set scene size
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm()); // Add external stylesheet for styling
        Stage newStage = new Stage(); // Create a new window (stage)
        newStage.setScene(scene); // Set the scene for the stage
        newStage.setTitle("Huffman Table"); // Set the title of the window
        newStage.show(); // Display the window
    }

    @FXML
    void onMainPageClicked(ActionEvent event) throws IOException {
        // Navigate back to the main page
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScene.fxml"));
        BorderPane bpPane = loader.load();
        mainPageBt.getScene().setRoot(bpPane);
    }

    @FXML
    void onOpenFolderClicked(ActionEvent event) {
        // Check if the path to the saved compressed file is available
        if (filePathAfterSave != null) {
            // Create a File object representing the saved compressed file
            File savedFile = new File(filePathAfterSave);

            // Check if the file exists at the specified location
            if (savedFile.exists()) {
                // Open the folder containing the saved compressed file
                Configurations.openFolder(savedFile.getParent());
            } else {
                // Show an error alert if the file does not exist
                Configurations.showAlert(
                        Alert.AlertType.ERROR, // Type of alert: ERROR
                        "Error",               // Alert title
                        "Compressed file not found at the saved location." // Alert message
                );
            }
        } else {
            // Show an error alert if no compressed file path is available
            Configurations.showAlert(
                    Alert.AlertType.ERROR, // Type of alert: ERROR
                    "Error",               // Alert title
                    "No compressed file to locate. Please save a compressed file first." // Alert message
            );
        }
    }

    @FXML // Handel clicked show header button
    void onShowHeaderClicked(ActionEvent event) {
        Configurations.handleShowHeader(headerToShow);
    }

    @FXML // Handle clicked show table button
    void onShowHuffmanTableClicked(ActionEvent event) {
        showHuffmanTable();
    }

    @FXML
    void onSaveClicked(ActionEvent event) {
        // Create a FileChooser dialog to allow the user to select a save location
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Compressed File"); // Set the dialog title

        // Restrict file types to Huffman compressed files with a .huff extension
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Huffman Compressed Files (*.huff)", "*.huff"));

        // Set the initial file name for the file chooser dialog without the original extension
        if (nameOfCompressedFile != null && !nameOfCompressedFile.toString().isEmpty()) {
            // Strip the extension from the name
            String fileNameWithoutExtension = nameOfCompressedFile.toString().contains(".")
                    ? nameOfCompressedFile.toString().substring(0, nameOfCompressedFile.toString().lastIndexOf('.'))
                    : nameOfCompressedFile.toString();
            fileChooser.setInitialFileName(fileNameWithoutExtension);
        } else {
            fileChooser.setInitialFileName("compressed"); // Default name if no file name is provided
        }

        // Show the save dialog and get the file the user selected
        File saveFile = fileChooser.showSaveDialog(null);

        // Check if the user selected a file
        if (saveFile != null) {
            try {
                // Move the compressed file from the default location to the selected location
                Files.move(
                        new File(System.getProperty("user.dir"), nameOfCompressedFile.toString()).toPath(), // Source path
                        saveFile.toPath(), // Destination path
                        StandardCopyOption.REPLACE_EXISTING // Replace file if it already exists
                );

                // Show a success message to the user
                Configurations.showAlert(
                        Alert.AlertType.INFORMATION, // Type of alert: INFORMATION
                        "File Saved", // Alert title
                        "File successfully saved to: " + saveFile.getAbsolutePath() // Alert message
                );

                // Update the filePathAfterSave variable with the new file path
                filePathAfterSave = saveFile.getAbsolutePath();
            } catch (IOException e) {
                // Show an error message if the file could not be saved
                Configurations.showAlert(
                        Alert.AlertType.ERROR, // Type of alert: ERROR
                        "Save Error", // Alert title
                        "Error saving file: " + e.getMessage() // Alert message with exception details
                );
            }
        } else {
            // Show an alert if the user canceled the save operation
            Configurations.showAlert(
                    Alert.AlertType.ERROR, // Type of alert: ERROR
                    "Save Canceled", // Alert title
                    "The file was not saved." // Alert message
            );
        }
    }
}