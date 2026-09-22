package org.example.csc311_module3assignment_groupbased;

/**
 *
 *
 * @author Henry Arevalo, Sheraz Rahim, Paul Quigley, Muhammad Faseeh
 *
 *
 * Objectives:
 *
 * Practice with the use of OOP, and inheritance.
 * Use of Key and Mouse, Motion events, and listeners.
 * Practice with JavaFx GUI and graphics.
 * Use images, files, and pixels.
 * User Timer, Threads with animation.
 *
 * STEPS
 *
 *
 *Step1)
 * Create a Java project with at least two classes(car is something to have as a class).
 * Load the maze image – attached.
 * Load the robot picture.
 * Move the robot horizontally by enabling the KeyListener.
 * Move the robot using the arrow keys up, down, left, and right.
 * Sense the environment where the robot navigates (using pixel values).
 * Limit the movements to only possible paths.
 *
 * Step 2)  Move Droid from the starting point to exit automatically (as an animation).
 *
 * Step 3)
 *
 * Replace the robot Droid with a car that you draw (using rect, oval, and polygons)
 * Move the car through the maze similar to Driod.
 * Change car headings when a direction changes.
 * Repeat all previous steps for maze2 (attached maze2.png)
 * Use tabbed panes to switch between the two mazes.
 * Add any required components such as buttons, and labels, to achieve the requirements
 *
 */


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    private static final double MOVE_DISTANCE = 5;
    private static final double ROBOT_MARGIN = 1;

    @Override
    public void start(Stage stage) {
        Image mazeImage = new Image(getClass().getResourceAsStream("/images/maze.png"));
        Image robotImage = new Image(getClass().getResourceAsStream("/images/robot.png"));
        PixelReader mazePixels = mazeImage.getPixelReader();

        ImageView maze = new ImageView(mazeImage);
        ImageView robot = new ImageView(robotImage);
        robot.setX(45);
        robot.setY(35);

        Pane root = new Pane(maze, robot);
        root.setFocusTraversable(true);
        Scene scene = new Scene(root, mazeImage.getWidth(), mazeImage.getHeight());

        scene.setOnKeyPressed(event -> {
            double nextX = robot.getX();
            double nextY = robot.getY();

            if (event.getCode() == KeyCode.LEFT) {
                nextX -= MOVE_DISTANCE;
            } else if (event.getCode() == KeyCode.RIGHT) {
                nextX += MOVE_DISTANCE;
            } else if (event.getCode() == KeyCode.UP) {
                nextY -= MOVE_DISTANCE;
            } else if (event.getCode() == KeyCode.DOWN) {
                nextY += MOVE_DISTANCE;
            } else {
                return;
            }

            if (canMove(robot, nextX, nextY, mazePixels, mazeImage)) {
                robot.setX(nextX);
                robot.setY(nextY);
            }
        });

        stage.setTitle("Robot Maze");
        stage.setScene(scene);
        stage.show();
        scene.getRoot().requestFocus();
    }

    private boolean canMove(ImageView robot, double x, double y,
                            PixelReader mazePixels, Image mazeImage) {
        double left = x + ROBOT_MARGIN;
        double right = x + robot.getImage().getWidth() - ROBOT_MARGIN;
        double top = y + ROBOT_MARGIN;
        double bottom = y + robot.getImage().getHeight() - ROBOT_MARGIN;

        return isWalkable(left, top, mazePixels, mazeImage)
                && isWalkable(right, top, mazePixels, mazeImage)
                && isWalkable(left, bottom, mazePixels, mazeImage)
                && isWalkable(right, bottom, mazePixels, mazeImage);
    }

    private boolean isWalkable(double x, double y, PixelReader mazePixels, Image mazeImage) {
        int pixelX = (int) Math.floor(x);
        int pixelY = (int) Math.floor(y);

        if (pixelX < 0 || pixelY < 0
                || pixelX >= mazeImage.getWidth()
                || pixelY >= mazeImage.getHeight()) {
            return false;
        }

        return mazePixels.getColor(pixelX, pixelY).getBrightness() > 0.8;
    }
}
