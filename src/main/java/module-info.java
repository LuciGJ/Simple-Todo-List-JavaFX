module com.example.guidatabase {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;



    opens com.example.todolist to javafx.fxml;
    opens datamodel to javafx.base;
    exports com.example.todolist;
    exports datamodel;

}
