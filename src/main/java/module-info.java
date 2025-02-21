module com.huffmanscenebuilder {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.huffmanscenebuilder to javafx.fxml;
    exports com.huffmanscenebuilder;
}