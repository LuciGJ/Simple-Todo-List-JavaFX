package com.example.todolist;

import datamodel.DataModel;
import datamodel.TodoItem;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;


public class Controller {
    @FXML
    private ListView<TodoItem> itemsList;
    @FXML
    private TextArea detailsArea;
    @FXML
    private Label dateLabel;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private ContextMenu listMenu;
    @FXML
    private ToggleButton toggleToday;

    @FXML
    private ToggleButton toggleComplete;
    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label completedTasks;

    private final SimpleStringProperty completedTasksProperty = new SimpleStringProperty();

    private void setCompletedTasks() {
        completedTasksProperty.set(DataModel.getModel().getCompleted());
    }


    private void setEmpty() {
        detailsArea.setText("");
        dateLabel.setText("");
    }

    public void initialize() {
        GetAllTasks task = new GetAllTasks();
        itemsList.itemsProperty().bind(task.valueProperty());
        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.setVisible(true);
        task.setOnSucceeded(e -> {
            progressBar.setVisible(false);
            setCompletedTasks();
            completedTasks.textProperty().bind(completedTasksProperty);
        });
        task.setOnFailed(e -> progressBar.setVisible(false));
        new Thread(task).start();
        listMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(actionEvent -> {
            TodoItem item = itemsList.getSelectionModel().getSelectedItem();
            deleteItem(item);
        });

        MenuItem updateItem = new MenuItem("Update");
        updateItem.setOnAction(actionEvent -> {
            TodoItem item = itemsList.getSelectionModel().getSelectedItem();
            updateItem(item);
        });

        MenuItem setComplete = new MenuItem("Set complete");
        setComplete.setOnAction(actionEvent -> {
            TodoItem item = itemsList.getSelectionModel().getSelectedItem();
            setComplete(item);
        });
        listMenu.getItems().setAll(deleteItem, updateItem, setComplete);
        itemsList.getSelectionModel().selectedItemProperty().addListener((observableValue, todoItem, t1) -> {
            if (t1 != null) {
                TodoItem currentItem = itemsList.getSelectionModel().getSelectedItem();
                detailsArea.setText(currentItem.getDetailDescription());
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                dateLabel.setText(dateTimeFormatter.format(currentItem.getDueDate()));
            }
        });

        itemsList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        itemsList.getSelectionModel().select(0);

        itemsList.setCellFactory(new Callback<>() {
            @Override
            public ListCell<TodoItem> call(ListView<TodoItem> todoItemListView) {
                ListCell<TodoItem> cell = new ListCell<>() {
                    @Override
                    protected void updateItem(TodoItem todoItem, boolean b) {
                        super.updateItem(todoItem, b);
                        if (b) {
                            setText(null);
                        } else {
                            setText(todoItem.getShortDescription());
                            if (todoItem.getComplete() == 1) {
                                setTextFill(Color.GREEN);
                            } else if (todoItem.getDueDate().isBefore(LocalDate.now().plusDays(1))) {
                                setTextFill(Color.RED);
                            } else if (todoItem.getDueDate().isEqual(LocalDate.now().plusDays(1))) {
                                setTextFill(Color.ORANGE);
                            } else {
                                setTextFill(Color.BLACK);
                            }
                        }
                    }
                };

                cell.emptyProperty().addListener(
                        (obs, wasEmpty, isEmpty) -> {
                            if (isEmpty)
                                cell.setContextMenu(null);
                            else
                                cell.setContextMenu(listMenu);
                        });

                return cell;
            }
        });
    }

    @FXML
    public void showNewItemDialogue() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add new item");
        dialog.initOwner(mainBorderPane.getScene().getWindow());
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("todoItemDialog.fxml"));
        try {
            dialog.getDialogPane().setContent(fxmlLoader.load());
        } catch (IOException e) {
            System.out.println("Could not open the dialog");
            e.printStackTrace();
        }


        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        DialogController controller = fxmlLoader.getController();
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> controller.getShortDescriptionTextField().getText().trim().equals("") ||
                                controller.getDetailsDescriptionTextField().getText().trim().equals("") ||
                                controller.getDatePicker().getValue() == null,
                        controller.getShortDescriptionTextField().textProperty(),
                        controller.getDetailsDescriptionTextField().textProperty(),
                        controller.getDatePicker().valueProperty()
                ));
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<ObservableList<TodoItem>> task = new Task<>() {
                @Override
                protected ObservableList<TodoItem> call() {
                    controller.processResults();
                    return FXCollections.observableArrayList(DataModel.getModel().queryTask());
                }
            };
            new Thread(task).start();
            task.setOnSucceeded((e) -> {
                setEmpty();
                setCompletedTasks();
                filterItems();
            });

        }


    }

    public void deleteItem(TodoItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete item");
        alert.setHeaderText("Delete item " + item.getShortDescription());
        alert.setContentText("Are you sure? Use OK to confirm or Cancel to stop");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<ObservableList<TodoItem>> task = new Task<>() {
                @Override
                protected ObservableList<TodoItem> call() {
                    DataModel.getModel().deleteTask(item.getId());
                    return FXCollections.observableArrayList(DataModel.getModel().queryTask());
                }
            };
            new Thread(task).start();
            task.setOnSucceeded((e) -> {
                setEmpty();
                setCompletedTasks();
                filterItems();
            });
        }
    }


    @FXML
    public void handleKeyPressed(KeyEvent event) {
        TodoItem selectedItem = itemsList.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            if (event.getCode().equals(KeyCode.DELETE)) {
                deleteItem(selectedItem);
            }
        }
    }

    @FXML
    private void filterItems() {
        if (toggleToday.isSelected() && toggleComplete.isSelected()) {
            Task<ObservableList<TodoItem>> task = new Task<>() {
                @Override
                protected ObservableList<TodoItem> call() {
                    return FXCollections.observableArrayList(DataModel.getModel().queryTodayCompleteTask());
                }
            };
            new Thread(task).start();
            itemsList.itemsProperty().bind(task.valueProperty());
        } else if (toggleToday.isSelected()) {
            Task<ObservableList<TodoItem>> task = new Task<>() {
                @Override
                protected ObservableList<TodoItem> call() {
                    return FXCollections.observableArrayList(DataModel.getModel().queryTodayTask());
                }
            };
            new Thread(task).start();
            itemsList.itemsProperty().bind(task.valueProperty());

        } else if (toggleComplete.isSelected()) {
            Task<ObservableList<TodoItem>> task = new Task<>() {
                @Override
                protected ObservableList<TodoItem> call() {
                    return FXCollections.observableArrayList(DataModel.getModel().queryCompleteTask());
                }
            };
            new Thread(task).start();
            itemsList.itemsProperty().bind(task.valueProperty());

        } else {
            Task<ObservableList<TodoItem>> task = new Task<>() {
                @Override
                protected ObservableList<TodoItem> call() {
                    return FXCollections.observableArrayList(DataModel.getModel().queryTask());
                }
            };
            new Thread(task).start();
            itemsList.itemsProperty().bind(task.valueProperty());
        }
        setEmpty();

    }

    public void updateItem(TodoItem item) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update item");
        dialog.initOwner(mainBorderPane.getScene().getWindow());
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("todoItemDialog.fxml"));

        try {
            dialog.getDialogPane().setContent(fxmlLoader.load());

        } catch (IOException e) {
            System.out.println("Could not open the dialog");
            e.printStackTrace();
        }
        final DialogController controller = fxmlLoader.getController();
        controller.setShortDescription(item.getShortDescription());
        controller.setDetailsDescription(item.getDetailDescription());
        controller.setDatePicker(item.getDueDate());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> controller.getShortDescriptionTextField().getText().trim().equals("") ||
                                controller.getDetailsDescriptionTextField().getText().trim().equals("") ||
                                controller.getDatePicker().getValue() == null,
                        controller.getShortDescriptionTextField().textProperty(),
                        controller.getDetailsDescriptionTextField().textProperty(),
                        controller.getDatePicker().valueProperty()
                ));
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return controller.processUpdate(item);
                }
            };
            new Thread(task).start();
            task.setOnSucceeded((e) -> {
                item.setShortDescription(controller.getShortDescription());
                item.setDetailDescription(controller.getDetails());
                item.setDueDate(controller.getLocalDate());
                setEmpty();
                itemsList.refresh();
            });

        }
    }


    public void setComplete(TodoItem item) {

        int complete;
        if (item.getComplete() == 0) {
            complete = 1;
        } else {
            complete = 0;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return DataModel.getModel().updateComplete(item.getId(), complete);
            }
        };

        new Thread(task).start();
        task.setOnSucceeded((e) -> {
            if (task.valueProperty().get()) {
                item.setComplete(complete);
                setEmpty();
                setCompletedTasks();
                filterItems();
            }
        });
    }

    @FXML
    public void handleExit() {
        Platform.exit();
    }
}

class GetAllTasks extends Task<ObservableList<TodoItem>> {
    @Override
    public ObservableList<TodoItem> call() {
        return FXCollections.observableArrayList(
                DataModel.getModel().queryTask());
    }
}