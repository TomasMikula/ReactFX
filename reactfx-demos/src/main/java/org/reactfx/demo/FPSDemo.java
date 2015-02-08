package org.reactfx.demo;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.reactfx.EventStreams;

public class FPSDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label();

        EventStreams.animationTicks()
                .latestN(100)
                .map(ticks -> {
                    int n = ticks.size() - 1;
                    return n * 1_000_000_000.0 / (ticks.get(n) - ticks.get(0));
                })
                .map(d -> String.format("FPS: %.3f", d))
                .feedTo(label.textProperty());

        primaryStage.setScene(new Scene(new StackPane(label), 250, 150));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}