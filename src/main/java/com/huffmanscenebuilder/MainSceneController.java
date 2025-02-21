package com.huffmanscenebuilder;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

public class MainSceneController {

    @FXML
    private BorderPane bpMainScene;

    @FXML
    private Button btCompress;

    @FXML
    private Button btDecompress;

    @FXML
    private Label lblTitle;
    private Alert alert;

    @FXML
    void onCompressClicked(ActionEvent event) throws IOException {
        // Create a new FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a File to Compress");

        // Add a custom filter to exclude .huff and .huf files
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "All Files Except .huff and .huf", "*.*"));

        // Open the file dialog and get the selected file
        File selectedFile = fileChooser.showOpenDialog(bpMainScene.getScene().getWindow());

        // Validate the selected file
        if (selectedFile != null) {
            String fileName = selectedFile.getName().toLowerCase();
            if (fileName.endsWith(".huff") || fileName.endsWith(".huf")) {
                // Show an error alert if the file extension is invalid
                showAlert(Alert.AlertType.ERROR, "Invalid File", "Files with .huff or .huf extensions are not allowed.");
            } else {
                // Load the compression scene with the valid file
                FXMLLoader loader = new FXMLLoader(getClass().getResource("compress.fxml"));
                Configurations.compressFile = selectedFile;
                ScrollPane scrollPane = loader.load();
                bpMainScene.getScene().setRoot(scrollPane);
            }
        }
    }

    @FXML
    void onDecompressClicked(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir"))); // Set the initial directory to the user's current directory
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Huffman file", "*.huff")); // Set the file extension filter
        File selectedFile = fileChooser.showOpenDialog(bpMainScene.getScene().getWindow()); // Show the file chooser and get the selected file

        // Check if the selected file is not a .huff file and show an error if it's not
        if (selectedFile != null && !selectedFile.getName().endsWith(".huff")) {
            showAlert(Alert.AlertType.ERROR,"Error", "");
        } else if (selectedFile != null) { // If the selected file is a .huff file, change the scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("decompress.fxml"));
            Configurations.deCompressFile=selectedFile;
            ScrollPane scrollPane = loader.load();
            bpMainScene.getScene().setRoot(scrollPane);
        }
    }

    private void showAlert(Alert.AlertType alertType, String header, String content) {
        alert = new Alert(alertType);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
    }

}
