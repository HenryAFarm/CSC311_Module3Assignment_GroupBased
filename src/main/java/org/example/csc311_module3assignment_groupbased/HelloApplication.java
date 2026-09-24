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


import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelloApplication extends Application {
    private static final double MOVE_DISTANCE = 5;
    private static final double ROBOT_MARGIN = 1;
    private static final long ANIMATION_INTERVAL_NANOS = 15_000_000L;
    private static final double START_X = 15;
    private static final double START_Y = 260;

    @Override
    public void start(Stage stage) {
        Image mazeImage = new Image(getClass().getResourceAsStream("/images/maze.png"));
        Image robotImage = new Image(getClass().getResourceAsStream("/images/robot.png"));
        PixelReader mazePixels = mazeImage.getPixelReader();

        ImageView maze = new ImageView(mazeImage);
        ImageView robot = new ImageView(robotImage);
        robot.setX(START_X);
        robot.setY(START_Y);

        Pane robotRoot = new Pane(maze, robot);
        robotRoot.setFocusTraversable(true);
        Button autoSolveButton = new Button("Auto Solve");
        autoSolveButton.setLayoutX(10);
        autoSolveButton.setLayoutY(mazeImage.getHeight() + 10);
        robotRoot.getChildren().add(autoSolveButton);

        Pane carRoot = CarMazeScreen.createContent();
        Tab robotTab = new Tab("Robot Maze", robotRoot);
        robotTab.setClosable(false);
        Tab carTab = new Tab("Car Maze", carRoot);
        carTab.setClosable(false);
        TabPane tabPane = new TabPane(robotTab, carTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, previousTab, selectedTab) -> {
                    if (selectedTab == robotTab) {
                        robotRoot.requestFocus();
                    } else if (selectedTab == carTab) {
                        carRoot.requestFocus();
                    }
                });

        Scene scene = new Scene(tabPane, mazeImage.getWidth(), mazeImage.getHeight() + 80);

        AnimationTimer[] animation = new AnimationTimer[1];
        List<Position>[] path = new List[]{Collections.singletonList(
                new Position((int) robot.getX(), (int) robot.getY()))};
        final int[] nextPathPosition = {1};
        final long[] lastAnimationUpdate = {0};

        animation[0] = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastAnimationUpdate[0] < ANIMATION_INTERVAL_NANOS) {
                    return;
                }
                lastAnimationUpdate[0] = now;

                if (nextPathPosition[0] >= path[0].size()) {
                    stop();
                    return;
                }

                Position next = path[0].get(nextPathPosition[0]++);
                robot.setX(next.x);
                robot.setY(next.y);
            }
        };

        autoSolveButton.setOnAction(event -> {
            robot.setX(START_X);
            robot.setY(START_Y);
            path[0] = findPath(robot, mazePixels, mazeImage);
            nextPathPosition[0] = 1;
            lastAnimationUpdate[0] = 0;
            animation[0].start();
            robotRoot.requestFocus();
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!isArrowKey(event.getCode())) {
                return;
            }

            if (tabPane.getSelectionModel().getSelectedItem() == carTab) {
                if (carRoot.getOnKeyPressed() != null) {
                    carRoot.getOnKeyPressed().handle(event);
                }
                event.consume();
                return;
            }

            animation[0].stop();
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
            event.consume();
        });

        stage.setTitle("Robot Maze");
        stage.setScene(scene);
        stage.show();
        robotRoot.requestFocus();
    }

    private boolean isArrowKey(KeyCode keyCode) {
        return keyCode == KeyCode.LEFT
                || keyCode == KeyCode.RIGHT
                || keyCode == KeyCode.UP
                || keyCode == KeyCode.DOWN;
    }

    private List<Position> findPath(ImageView robot, PixelReader mazePixels, Image mazeImage) {
        Position start = new Position((int) robot.getX(), (int) robot.getY());
        ArrayDeque<Position> queue = new ArrayDeque<>();
        Map<Position, Position> previous = new HashMap<>();
        queue.add(start);
        previous.put(start, null);

        int[][] directions = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1}
        };

        Position exit = null;
        while (!queue.isEmpty()) {
            Position current = queue.remove();
            if (isAtExit(current, robot, mazePixels, mazeImage)) {
                exit = current;
                break;
            }

            for (int[] direction : directions) {
                Position next = new Position(
                        current.x + direction[0],
                        current.y + direction[1]);

                if (!previous.containsKey(next)
                        && canMove(robot, next.x, next.y, mazePixels, mazeImage)) {
                    previous.put(next, current);
                    queue.add(next);
                }
            }
        }

        if (exit == null) {
            return Collections.singletonList(start);
        }

        List<Position> path = new ArrayList<>();
        for (Position current = exit; current != null; current = previous.get(current)) {
            path.add(current);
        }
        Collections.reverse(path);
        return path;
    }

    private boolean isAtExit(Position position, ImageView robot,
                              PixelReader mazePixels, Image mazeImage) {
        double rightEdge = position.x + robot.getImage().getWidth() - ROBOT_MARGIN;
        double centerY = position.y + robot.getImage().getHeight() / 2.0;

        return rightEdge >= mazeImage.getWidth() - 1
                && isWalkable(mazeImage.getWidth() - 1, centerY, mazePixels, mazeImage);
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

    private static class Position {
        private final int x;
        private final int y;

        private Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Position)) {
                return false;
            }
            Position position = (Position) object;
            return x == position.x && y == position.y;
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }
    }
}
