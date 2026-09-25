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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {

        //Since this is testing, it will change!
        //Testing to see the image(of robot) popping up!
        //This gets the image from resource under images

        //Pane root = RobotMazeScreen.createContent();
        //Scene scene = new Scene(root, 620, 470);
/*
            stage.setTitle("Robot Maze");
            stage.setScene(scene);
            stage.show();

            root.requestFocus();
        }
*/

            Pane robotRoot = RobotMazeScreen.createContent();
            Pane carRoot = CarMaze.createContent();

            Tab robotTab = new Tab("Robot Maze", robotRoot);
            robotTab.setClosable(false);

            Tab carTab = new Tab("Car Maze", carRoot);
            carTab.setClosable(false);

            TabPane tabPane = new TabPane(robotTab, carTab);

            Scene scene = new Scene(tabPane, 620, 470);

            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

                if (!isArrowKey(event.getCode())) {
                    return;
                }

                Pane selectedRoot =
                        tabPane.getSelectionModel().getSelectedItem() == carTab
                                ? carRoot
                                : robotRoot;

                if (selectedRoot.getOnKeyPressed() != null) {
                    selectedRoot.getOnKeyPressed().handle(event);
                }

                event.consume();
            });

            stage.setTitle("Maze Assignment");
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
    }