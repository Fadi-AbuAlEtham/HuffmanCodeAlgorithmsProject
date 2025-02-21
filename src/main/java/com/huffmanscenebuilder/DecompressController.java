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

public class DecompressController {

    @FXML
    private Button mainPageButton;

    @FXML
    private Button saveButton;

    @FXML
    private Label decompRatioLbl;

    @FXML
    private Label fSizeALbl;

    @FXML
    private Label fSizeBLbl;

    @FXML
    private Button openFolderBt;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private VBox resultsVbox;

    @FXML
    private Button showHeadBt;

    @FXML
    private Button showTableBt;

    @FXML
    private TextField txtDecompressRatio;

    @FXML
    private TextField txtFSizeB;

    @FXML
    private TextField txtFSizeA;

    @FXML
    private Label selectedFileText;

    @FXML
    private Label waitLbl;

    File originalFile = Configurations.deCompressFile; // Path of the file to be decompressed
    private TreeNode[] bytes; // Array to store TreeNode objects for Huffman tree
    private long lengthFB; // Length of the file before decompression
    private long lengthFA; // Length of the file after decompression
    StringBuilder headerToShow = new StringBuilder(""); // StringBuilder to construct and show header information
    private StringBuilder nameOfDecompressedFile; // StringBuilder to store the name of the decompressed file
    private String filePathAfterDecompress;

    @FXML
    void initialize() {
        decompress();
    }

    public void decompress() {
        // Define a task for performing decompression asynchronously
        Task<Void> decompressTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Step 1: Initialization
                    updateMessage("Initializing decompression...");
                    updateProgress(0, 1);

                    // Record the size of the compressed file
                    lengthFB = originalFile.length();

                    // Open a FileInputStream to read the compressed file
                    FileInputStream in = new FileInputStream(originalFile);

                    // Prepare variables for header processing
                    StringBuilder fileExtension = new StringBuilder(); // Store the file extension
                    byte[] buffer = new byte[8]; // Buffer for reading 8 bytes at a time
                    byte[] sizeOfHeaderBuffer = new byte[4]; // Buffer to read the size of the header

                    // Step 2: Read the header and retrieve its size
                    int sizeOfHeader = readFileHeader(in, buffer, sizeOfHeaderBuffer, fileExtension);

                    // Step 3: Read the file data
                    updateMessage("Reading file data...");
                    StringBuilder header = new StringBuilder(); // Stores the header in binary format
                    StringBuilder processData = new StringBuilder(); // Stores the serialized data for decompression

                    // Read and process the file data
                    readFileData(in, buffer, sizeOfHeader, header, processData);

                    // Close the FileInputStream as it's no longer needed
                    in.close();

                    // Append the header for visualization/debugging
                    headerToShow.append(header);

                    // Step 4: Reconstruct the Huffman tree
                    updateMessage("Reconstructing Huffman tree...");
                    TreeNode rootTreeNode = reconstructHuffmanTree(header, sizeOfHeader);

                    // Step 5: Create the decompressed file
                    updateMessage("Creating decompressed file...");
                    createDecompressedFile(processData, rootTreeNode, fileExtension);

                    // Step 6: Finalize the task
                    updateMessage("Decompression completed.");
                    updateProgress(1, 1);

                    return null; // Indicate successful completion
                } catch (Exception e) {
                    // Update the message with the error and rethrow the exception
                    updateMessage("Error: " + e.getMessage());
                    throw e;
                }
            }
        };

        // Bind progress and messages to the UI components
        bindProgressAndMessages(decompressTask);

        // Start the decompression task on a new thread
        new Thread(decompressTask).start();
    }

    private int readFileHeader(FileInputStream in, byte[] buffer, byte[] sizeOfHeaderBuffer, StringBuilder fileExtension) throws IOException {
        // Read the first 8 bytes of the file into the buffer
        if (in.read(buffer) != -1) {
            // Process the first 8 bytes to extract the file extension and header details
            for (int i = 0; i < 8; i++) {
                if (buffer[i] != 0) {
                    // Append the non-zero byte values as characters to the fileExtension
                    fileExtension.append((char) buffer[i]);
                }
                // Convert each byte to a binary string and append it to the headerToShow for debugging/visualization
                headerToShow.append(Configurations.byteToBinaryString(buffer[i]));
            }

            // Read the next 4 bytes, which represent the size of the header
            if (in.read(sizeOfHeaderBuffer) != -1) {
                // Convert the 4-byte array into an integer representing the header size
                int headerSize = byteArrayToInt(sizeOfHeaderBuffer);

                // Append the binary representation of the header size to headerToShow
                for (byte b : sizeOfHeaderBuffer) {
                    headerToShow.append(Configurations.byteToBinaryString(b));
                }

                // Return the header size as an integer
                return headerSize;
            }
        }

        // If reading fails or reaches EOF, close the stream and throw an exception
        in.close();
        throw new IllegalArgumentException("The input file cannot be read");
    }

    private void readFileData(FileInputStream in, byte[] buffer, int headerSize, StringBuilder header, StringBuilder processData) throws IOException {
        // Calculate the number of bytes required to store the header
        // If the header size (in bits) is not a multiple of 8, round up to the next byte
        int numberOfBytesForHeader = (headerSize % 8 == 0) ? headerSize / 8 : (headerSize / 8) + 1;

        // Variable to track the number of bytes read in each iteration
        int numberOfBytesRead;
        // Counter to keep track of how many bytes of the header have been processed
        int countNumBytesReadFromHead = 0;

        // Read the file in chunks using the provided buffer
        while ((numberOfBytesRead = in.read(buffer)) != -1) {
            // Process each byte in the current buffer
            for (int i = 0; i < numberOfBytesRead; i++) {
                // Check if we are still reading the header
                if (countNumBytesReadFromHead < numberOfBytesForHeader) {
                    // Convert the current byte to a binary string and append it to the header
                    header.append(Configurations.byteToBinaryString(buffer[i]));
                    countNumBytesReadFromHead++;
                } else {
                    // If the header is fully read, append the remaining bytes to the serialized data
                    processData.append(Configurations.byteToBinaryString(buffer[i]));
                }
            }
        }
    }

    private TreeNode reconstructHuffmanTree(StringBuilder header, int sizeOfHeader) {
        // Initialize a stack to assist in reconstructing the Huffman tree
        Stack stack = new Stack(256); // Size 256 is sufficient for all possible byte values
        int counter = 0; // Tracks the current position in the header string
        int numberOfLeafNode = 0; // Tracks the number of leaf nodes (unique byte values)

        // Process the header string to reconstruct the Huffman tree
        while (counter < sizeOfHeader) {
            if (header.charAt(counter) == '0') {
                // If the current bit is '0', it represents a leaf node
                counter++; // Move to the byte content
                // Extract the byte from the next 8 bits and create a new TreeNode
                stack.push(new TreeNode((byte) Integer.parseInt(header.substring(counter, counter + 8), 2)));
                numberOfLeafNode++; // Increment the leaf node counter
                counter += 8; // Move past the 8 bits representing the byte content
            } else {
                // If the current bit is '1', it represents an internal node
                counter++; // Move past the '1'
                TreeNode node = new TreeNode(0); // Create a new internal node

                // Pop the last two nodes from the stack and set them as children
                node.setRight(stack.pop()); // Right child is the last pushed node
                node.setLeft(stack.pop()); // Left child is the second last pushed node

                // Push the internal node back onto the stack
                stack.push(node);
            }
        }

        // The root node of the Huffman tree is the last node remaining on the stack
        TreeNode rootTreeNode = stack.peek();

        // Assign Huffman codes to the tree nodes
        if (rootTreeNode.getLeft() == null && rootTreeNode.getRight() == null) {
            // If there's only one node in the tree, assign it the code "1"
            rootTreeNode.setCode("1");
        } else {
            // Otherwise, recursively assign codes to all nodes
            TreeNode.giveCodeForEachByte(rootTreeNode);
        }

        // Store all the leaf nodes (unique byte values) in the `bytes` array
        bytes = new TreeNode[numberOfLeafNode];
        rootTreeNode.getLeafNodes(bytes);

        // Return the root of the reconstructed Huffman tree
        return rootTreeNode;
    }

    private void createDecompressedFile(StringBuilder processData, TreeNode rootTreeNode, StringBuilder fileExtension) throws IOException {
        // Step 1: Generate the name for the decompressed file
        // Replace the extension of the original file name with the extracted file extension
        nameOfDecompressedFile = new StringBuilder(Configurations.replaceExtension(originalFile.getName(), fileExtension.toString()));

        // Create a File object for the decompressed file in the current working directory
        File tempFile = new File(System.getProperty("user.dir"), nameOfDecompressedFile.toString());

        // Step 2: Create a FileOutputStream to write the decompressed data to the file
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            // Decode the serialized data and write the decompressed data to the file
            processSerializedData(processData, rootTreeNode, out);

            // Step 3: Record the size of the decompressed file
            lengthFA = tempFile.length(); // Store the file size after decompression
            filePathAfterDecompress = tempFile.getAbsolutePath(); // Store the absolute path of the decompressed file
        }
    }

    private void processSerializedData(StringBuilder processData, TreeNode rootTreeNode, FileOutputStream out) throws IOException {
        // Step 1: Extract and handle the effective bits information
        int startIndex = processData.length() - 8; // The last 8 bits contain the number of effective bits in the last byte
        int numberOfEffectiveBits = Integer.parseInt(processData.substring(startIndex), 2); // Parse as an integer

        // Remove the padding and the last byte containing the number of effective bits
        processData.delete(startIndex + numberOfEffectiveBits - 8, processData.length());

        // Step 2: Initialize buffers for writing the decoded data
        byte[] bufferOut = new byte[8]; // Buffer to hold decoded bytes for output

        int counterForBufferprocessData = 0; // Tracks the position in the serialized data
        int counterForBufferOut = 0; // Tracks the number of bytes in the output buffer

        // Step 3: Traverse the serialized data and decode using the Huffman tree
        while (counterForBufferprocessData < processData.length()) {
            TreeNode curr = rootTreeNode; // Start traversal from the root of the Huffman tree

            // Traverse the tree until a leaf node (decoded byte) is reached
            while (curr != null && counterForBufferprocessData < processData.length()) {
                if (processData.charAt(counterForBufferprocessData) == '0' && curr.hasLeft()) {
                    // Move to the left child if the bit is '0'
                    curr = curr.getLeft();
                } else if (curr.hasRight()) {
                    // Move to the right child if the bit is '1'
                    curr = curr.getRight();
                } else if (rootTreeNode.getLeft() == null && rootTreeNode.getRight() == null) {
                    // Handle the special case where the tree has only one node (single byte in the file)
                    counterForBufferprocessData++;
                    break;
                } else break;

                counterForBufferprocessData++; // Move to the next bit in the serialized data
            }

            // Add the byte from the leaf node to the output buffer
            bufferOut[counterForBufferOut++] = curr.getByteContent();

            // Write the buffer to the output stream when full
            if (counterForBufferOut == 8) {
                out.write(bufferOut);
                counterForBufferOut = 0; // Reset the buffer counter
            }
        }

        // Step 4: Write any remaining bytes in the output buffer
        if (counterForBufferOut > 0) {
            out.write(bufferOut, 0, counterForBufferOut); // Write only the valid bytes
        }
    }

    private void bindProgressAndMessages(Task<Void> decompressTask) {
        // Bind the progress property of the progress bar to the task's progress property
        // This allows the progress bar to update in real-time as the task progresses
        progressBar.progressProperty().bind(decompressTask.progressProperty());

        // Bind the text property of the waitLabel to the task's message property
        // This allows the label to display real-time status updates from the task
        waitLbl.textProperty().bind(decompressTask.messageProperty());

        // Set an event handler for when the task completes successfully
        decompressTask.setOnSucceeded(event -> handleSuccess());

        // Set an event handler for when the task fails due to an exception
        decompressTask.setOnFailed(event -> handleFailure(decompressTask));
    }

    private void handleSuccess() {
        // Step 1: Unbind the waitLabel's text property from the task's message property
        // This ensures the label is no longer tied to the task and can be updated independently
        waitLbl.textProperty().unbind();

        // Step 2: Update the label to indicate that decompression is complete
        waitLbl.setText("Decompression complete!");

        // Step 3: Hide and unmanage the progress bar since the task has finished
        progressBar.setVisible(false); // Makes the progress bar invisible
        progressBar.setManaged(false); // Removes it from the layout calculations

        // Step 4: Update the UI on the JavaFX Application thread
        // The updateUIAfterSuccess method handles additional UI updates after success
        Platform.runLater(() -> updateUIAfterSuccess());
    }

    private void handleFailure(Task<Void> decompressTask) {
        // Step 1: Unbind the waitLabel's text property from the task's message property
        // This allows manual updates to the label text after the task fails
        waitLbl.textProperty().unbind();

        // Step 2: Update the label to inform the user that decompression failed
        waitLbl.setText("Decompression failed!");

        // Step 3: Hide and unmanage the progress bar since the task has ended
        progressBar.setVisible(false); // Makes the progress bar invisible
        progressBar.setManaged(false); // Removes it from the layout calculations

        // Step 4: Display an error alert to the user
        // The alert contains details about the exception that caused the failure
        Configurations.showAlert(
                Alert.AlertType.ERROR,                          // Type of alert: ERROR
                "Decompression Error",                          // Alert title
                decompressTask.getException().getMessage()      // Detailed error message from the task
        );
    }

    private void updateUIAfterSuccess() {
        // Make sure the parent container for results is visible and managed
        resultsVbox.setVisible(true);
        resultsVbox.setManaged(true);

        // Update file size before decompression
        fSizeBLbl.setVisible(true);
        fSizeBLbl.setManaged(true);
        fSizeBLbl.setText("Original Size:");
        selectedFileText.setVisible(true);
        selectedFileText.setText("Selected File: " + originalFile.getName());

        txtFSizeB.setVisible(true);
        txtFSizeB.setManaged(true);
        txtFSizeB.setText(formatFileSize(lengthFB));

        // Update file size after decompression
        fSizeALbl.setVisible(true);
        fSizeALbl.setManaged(true);
        fSizeALbl.setText("Decompressed Size:");

        txtFSizeA.setVisible(true);
        txtFSizeA.setManaged(true);
        txtFSizeA.setText(formatFileSize(lengthFA));

        // Update decompression ratio
        decompRatioLbl.setVisible(true);
        decompRatioLbl.setManaged(true);
        decompRatioLbl.setText("Decompression Ratio:");

        txtDecompressRatio.setVisible(true);
        txtDecompressRatio.setManaged(true);
        txtDecompressRatio.setText(String.format("%.3f%%", (1 - (double) lengthFA / lengthFB) * 100));

        // Show action buttons
        openFolderBt.setVisible(true);
        openFolderBt.setManaged(true);

        showHeadBt.setVisible(true);
        showHeadBt.setManaged(true);

        showTableBt.setVisible(true);
        showTableBt.setManaged(true);

        saveButton.setVisible(true);
        saveButton.setManaged(true);
    }

    // Method to convert a byte array to an integer
    public static int byteArrayToInt(byte[] b) {
        // Combines 4 bytes into an integer, assuming big-endian order
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

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

    // Method to display the Huffman table
    private void showHuffmanTable() {
        // Step 1: Create a scrollable pane to hold the table and layout
        ScrollPane subScenePane = new ScrollPane();
        subScenePane.setFitToWidth(true); // Allow content to adjust to the width of the scroll pane
        subScenePane.setFitToHeight(true);

        // Step 2: Create a horizontal box (HBox) for layout
        HBox hBox = new HBox(40); // Spacing of 40px between elements
        hBox.setAlignment(Pos.CENTER); // Align content in the center
        hBox.setPadding(new Insets(20)); // Add padding around the box
        hBox.setStyle("-fx-background-color: #2b2b3b;"); // Set background color for the HBox

        // Step 3: Create a TableView to display the Huffman table data
        TableView<TreeNode> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // Ensure columns resize to fit the table
        tableView.setPrefSize(900, 600); // Set preferred dimensions for the table
        tableView.setStyle("-fx-background-color: #2b2b3b; -fx-border-color: #444444;"); // Set table style

        // Step 4: Define table columns
        // Column 1: Byte (unsigned representation of byte content)
        TableColumn<TreeNode, String> byteColumn = new TableColumn<>("Byte");
        byteColumn.setCellValueFactory(cellData -> {
            int unsignedByte = cellData.getValue().getByteContent() & 0xFF; // Convert signed byte to unsigned
            return new SimpleStringProperty(String.valueOf(unsignedByte)); // Display the byte as a string
        });
        byteColumn.setStyle("-fx-alignment: CENTER; -fx-font-size: 16px; -fx-text-fill: #ffffff;"); // Style the column

        // Column 2: Huffman Code (binary representation)
        TableColumn<TreeNode, String> huffmanColumn = new TableColumn<>("Huffman Code");
        huffmanColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCode())); // Bind to Huffman code
        huffmanColumn.setStyle("-fx-alignment: CENTER; -fx-font-size: 16px; -fx-text-fill: #ffffff;");

        // Column 3: Code Length (number of bits in the Huffman code)
        TableColumn<TreeNode, Integer> lengthColumn = new TableColumn<>("Code Length");
        lengthColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getCode().length()).asObject());
        lengthColumn.setStyle("-fx-alignment: CENTER; -fx-font-size: 16px; -fx-text-fill: #ffffff;");

        // Step 5: Add columns to the table
        tableView.getColumns().addAll(byteColumn, huffmanColumn, lengthColumn);

        // Step 6: Populate the table with data from the TreeNode array
        ObservableList<TreeNode> data = FXCollections.observableArrayList();
        for (TreeNode node : bytes) {
            if (node.getCode() != null) { // Only add nodes with valid Huffman codes
                data.add(node);
            }
        }
        tableView.setItems(data); // Bind data to the table

        // Step 7: Create a label for the table header
        Label huffmanLabel = new Label("Huffman Table");
        huffmanLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 20)); // Set font style and size
        huffmanLabel.setStyle("-fx-text-fill: #ffa726;"); // Set text color

        // Step 8: Create a vertical box (VBox) to hold the header and table
        VBox contentContainer = new VBox(20); // Spacing of 20px between elements
        contentContainer.setAlignment(Pos.CENTER); // Align content in the center
        contentContainer.setPadding(new Insets(20)); // Add padding inside the VBox
        contentContainer.setStyle("-fx-background-color: #2b2b3b; " + // Set background color
                "-fx-border-color: #444444; " + // Set border color
                "-fx-border-width: 2px;"); // Set border width
        contentContainer.getChildren().addAll(huffmanLabel, tableView); // Add the label and table to the VBox

        // Step 9: Add the VBox to the HBox
        hBox.getChildren().add(contentContainer);

        // Step 10: Set up the ScrollPane and Scene
        subScenePane.setContent(hBox); // Add the HBox to the ScrollPane
        Scene scene = new Scene(subScenePane, 1500, 1300); // Create a new Scene with dimensions 1500x1300
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm()); // Apply external CSS for advanced styling
        Stage newStage = new Stage(); // Create a new Stage (window)
        newStage.setScene(scene); // Set the scene for the stage
        newStage.setTitle("Huffman Table"); // Set the title of the window
        newStage.show(); // Display the window
    }

    @FXML
    void onMainPageClicked(ActionEvent event) throws IOException {
        // Navigate back to the main page
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScene.fxml"));
        BorderPane bpPane = loader.load();
        mainPageButton.getScene().setRoot(bpPane);
    }

    @FXML
    void onOpenFolderClicked(ActionEvent event) {
        // Step 1: Check if the path to the decompressed file is available
        if (filePathAfterDecompress != null) {
            // Create a File object representing the decompressed file
            File savedFile = new File(filePathAfterDecompress);

            // Step 2: Check if the file exists at the specified location
            if (savedFile.exists()) {
                // Open the folder containing the decompressed file
                Configurations.openFolder(savedFile.toString()); // Use the file path as a string
            } else {
                // Show an error alert if the file does not exist
                Configurations.showAlert(
                        Alert.AlertType.ERROR, // Type of alert: ERROR
                        "Error",               // Alert title
                        "Decompressed file not found at the saved location." // Alert message
                );
            }
        } else {
            // Show an error alert if no decompressed file path is available
            Configurations.showAlert(
                    Alert.AlertType.ERROR, // Type of alert: ERROR
                    "Error",               // Alert title
                    "No Decompressed file to locate. Please save a decompressed file first." // Alert message
            );
        }
    }

    @FXML
    void onShowHeaderClicked(ActionEvent event) {
        Configurations.handleShowHeader(headerToShow);
    }

    @FXML
    void onShowHuffmanTableClicked(ActionEvent event) {
        showHuffmanTable();
    }

    @FXML
    void onSaveClicked(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Decompressed File");

        // Determine the original extension of the decompressed file
        String originalExtension = nameOfDecompressedFile.toString().substring(nameOfDecompressedFile.toString().lastIndexOf('.') + 1);

        // Add a filter for the original file extension
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Original File Type (*." + originalExtension + ")", "*." + originalExtension));

        // Set the initial file name for the file chooser dialog without the original extension
        if (nameOfDecompressedFile != null && !nameOfDecompressedFile.toString().isEmpty()) {
            // Strip the extension from the name
            String fileNameWithoutExtension = nameOfDecompressedFile.toString().contains(".")
                    ? nameOfDecompressedFile.toString().substring(0, nameOfDecompressedFile.toString().lastIndexOf('.'))
                    : nameOfDecompressedFile.toString();
            fileChooser.setInitialFileName(fileNameWithoutExtension);
        } else {
            fileChooser.setInitialFileName("decompressed"); // Default name if no file name is provided
        }

        // Show the save dialog
        File saveFile = fileChooser.showSaveDialog(null);
        if (saveFile != null) {
            try {
                // Ensure the saved file has the correct extension
                if (!saveFile.getName().endsWith("." + originalExtension)) {
                    saveFile = new File(saveFile.getAbsolutePath() + "." + originalExtension);
                }

                // Move the decompressed file to the selected location
                Files.move(new File(System.getProperty("user.dir"), nameOfDecompressedFile.toString()).toPath(),
                        saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                // Show success alert
                Configurations.showAlert(Alert.AlertType.INFORMATION, "File Saved", "File successfully saved to: " + saveFile.getAbsolutePath());
                filePathAfterDecompress = saveFile.getAbsolutePath();
            } catch (IOException e) {
                // Show error alert in case of an exception
                Configurations.showAlert(Alert.AlertType.ERROR, "Save Error", "Error saving file: " + e.getMessage());
            }
        } else {
            // Show alert if the save process was canceled
            Configurations.showAlert(Alert.AlertType.ERROR, "Save Canceled", "The file was not saved.");
        }
    }
}