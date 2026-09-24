package org.example.csc311_module3assignment_groupbased;

/**
 *
 * @author Henry Arevalo, Sheraz Rahim, Paul Quigley, Muhammad Faseeh
 *
 */

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        Pane robotRoot = RobotMazeScreen.createContent();
        Pane carRoot = CarMazeScreen.createContent();

        Tab robotTab = new Tab("Robot Maze", robotRoot);
        robotTab.setClosable(false);
        Tab carTab = new Tab("Car Maze", carRoot);
        carTab.setClosable(false);

        //Switching tabs in the application
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

        Scene scene = new Scene(tabPane, 480, 440);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!isArrowKey(event.getCode())) {
                return;
            }

            Pane selectedRoot = tabPane.getSelectionModel().getSelectedItem() == carTab
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
