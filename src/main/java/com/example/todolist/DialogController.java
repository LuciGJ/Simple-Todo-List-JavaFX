package com.example.todolist;

import datamodel.DataModel;
import datamodel.TodoItem;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;

public class DialogController {
    @FXML
    private TextField shortDescription;
    @FXML
    private TextArea detailsDescription;
    @FXML
    private DatePicker datePicker;

    public void setShortDescription(String shortDescription) {
        this.shortDescription.setText(shortDescription);
    }

    public void setDetailsDescription(String detailsDescription) {
        this.detailsDescription.setText(detailsDescription);
    }

    public String getDetails() {
        return detailsDescription.getText().trim();
    }

    public TextField getShortDescriptionTextField(){
        return shortDescription;
    }

    public TextArea getDetailsDescriptionTextField(){
        return detailsDescription;
    }

    public DatePicker getDatePicker(){
        return datePicker;
    }

    public String getShortDescription() {
        return shortDescription.getText().trim();
    }

    public LocalDate getLocalDate() {
        return datePicker.getValue();
    }

    public void setDatePicker(LocalDate date) {
        this.datePicker.setValue(date);
    }
    public void processResults() {
        String retrievedShort = shortDescription.getText().trim();
        String details = detailsDescription.getText().trim();
        LocalDate retrievedDate = datePicker.getValue();
        DataModel.getModel().insertTask(retrievedShort, details, retrievedDate);
    }

    public boolean processUpdate(TodoItem item) {
        String retrievedShort = shortDescription.getText().trim();
        String details = detailsDescription.getText().trim();
        LocalDate retrievedDate = datePicker.getValue();
        return DataModel.getModel().updateTask(retrievedShort, details, retrievedDate, item.getId());
    }


}
