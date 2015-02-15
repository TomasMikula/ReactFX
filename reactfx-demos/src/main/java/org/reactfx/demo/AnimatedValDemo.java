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
        Circle circle = new Circle(30.0);
        Pane canvas = new Pane(circle);

        // animate circle position
        Var<Point2D> center = Var.newSimpleVar(new Point2D(W/2, H/2));
        Val<Point2D> animCenter = center.animate(
                (p1, p2) -> Duration.ofMillis((long) p1.distance(p2)),
                (p1, p2, frac) -> p1.multiply(1.0-frac).add(p2.multiply(frac)));

        circle.centerXProperty().bind(animCenter.map(Point2D::getX));
        circle.centerYProperty().bind(animCenter.map(Point2D::getY));

        // animate circle color
        Var<Color> color = Var.newSimpleVar(Color.BLUE);
        Val<Color> animColor = Val.animate(color, Duration.ofMillis(500));
        circle.fillProperty().bind(animColor);

        // on click, move to random position and transition to random color
        Random random = new Random();
        circle.setOnMouseClicked(click -> {
            center.setValue(new Point2D(
                    random.nextInt(W),
                    random.nextInt(H)));
            color.setValue(Color.rgb(
                    random.nextInt(240),
                    random.nextInt(240),
                    random.nextInt(240)));
        });

        primaryStage.setScene(new Scene(canvas, W, H));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}