package com.example.todolist;

import datamodel.DataModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 500);
        scene.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("style.css")).toExternalForm());
        stage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResourceAsStream("todolist.png"))));
        stage.setTitle("Todo List");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception{
       super.stop();
       DataModel.getModel().close();
    }

    @Override
    public void init() {
        if (!DataModel.getModel().open()) {
            System.out.println("Cannot open datasource");
            Platform.exit();
        }
    }



    public static void main(String[] args) {
        launch();
    }
}