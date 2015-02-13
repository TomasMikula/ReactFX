package org.reactfx.demo;

import java.time.Duration;
import java.util.Random;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import org.reactfx.value.Val;
import org.reactfx.value.Var;

public class AnimatedValDemo extends Application {
    private static final int W = 600;
    private static final int H = 600;

    @Override
    public void start(Stage primaryStage) {
        Circle circle = new Circle(30.0, Color.BLUE);
        Pane canvas = new Pane(circle);

        Var<Point2D> center = Var.newSimpleVar(new Point2D(W/2, H/2));
        Val<Point2D> animCenter = center.animate(
                (p1, p2) -> Duration.ofMillis((long) p1.distance(p2)),
                (p1, p2, frac) -> p1.multiply(1.0-frac).add(p2.multiply(frac)));

        circle.centerXProperty().bind(animCenter.map(Point2D::getX));
        circle.centerYProperty().bind(animCenter.map(Point2D::getY));

        Random random = new Random();

        circle.setOnMouseClicked(click -> {
            center.setValue(new Point2D(
                    random.nextInt(W),
                    random.nextInt(H)));
        });

        primaryStage.setScene(new Scene(canvas, W, H));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}