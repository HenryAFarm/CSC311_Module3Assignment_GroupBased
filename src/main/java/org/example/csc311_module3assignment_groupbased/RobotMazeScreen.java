package org.example.csc311_module3assignment_groupbased;

import javafx.animation.AnimationTimer;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RobotMazeScreen {
    private static final double MOVE_DISTANCE = 5;
    private static final double ROBOT_MARGIN = 1;
    private static final long ANIMATION_INTERVAL_NANOS = 15_000_000L;
    private static final double START_X = 15;
    private static final double START_Y = 260;

    private RobotMazeScreen() {
    }

    static Pane createContent() {
        //Setting up the image to be used in the tab
        Image mazeImage = new Image(RobotMazeScreen.class.getResourceAsStream("/images/maze.png"));
        Image robotImage = new Image(RobotMazeScreen.class.getResourceAsStream("/images/robot.png"));
        PixelReader mazePixels = mazeImage.getPixelReader();

        ImageView maze = new ImageView(mazeImage);
        ImageView robot = new ImageView(robotImage);
        robot.setX(START_X);
        robot.setY(START_Y);

        Pane root = new Pane(maze, robot);
        root.setFocusTraversable(true);

        //Adding the button which allows the robot to auto move the end
        Button autoSolveButton = new Button("Auto Solve");
        autoSolveButton.setLayoutX(10);
        autoSolveButton.setLayoutY(mazeImage.getHeight() + 10);
        root.getChildren().add(autoSolveButton);

        //Adding animation to this, so that after the auto solve button is pressed, the robot moves to the end
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
            animation[0].stop();
            robot.setX(START_X);
            robot.setY(START_Y);
            path[0] = findPath(robot, mazePixels, mazeImage);
            nextPathPosition[0] = 1;
            lastAnimationUpdate[0] = 0;
            animation[0].start();
            root.requestFocus();
        });

        root.setOnKeyPressed(event -> {
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
        });

        return root;
    }

    private static List<Position> findPath(ImageView robot, PixelReader mazePixels, Image mazeImage) {
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

    private static boolean isAtExit(Position position, ImageView robot,
                                    PixelReader mazePixels, Image mazeImage) {
        double rightEdge = position.x + robot.getImage().getWidth() - ROBOT_MARGIN;
        double centerY = position.y + robot.getImage().getHeight() / 2.0;

        return rightEdge >= mazeImage.getWidth() - 1
                && isWalkable(mazeImage.getWidth() - 1, centerY, mazePixels, mazeImage);
    }

    //Allowing for the robot image to be moved
    private static boolean canMove(ImageView robot, double x, double y,
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

    private static boolean isWalkable(double x, double y,
                                     PixelReader mazePixels, Image mazeImage) {
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
