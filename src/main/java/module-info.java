module org.example.csc311_module3assignment_groupbased {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.csc311_module3assignment_groupbased to javafx.fxml;
    exports org.example.csc311_module3assignment_groupbased;
}