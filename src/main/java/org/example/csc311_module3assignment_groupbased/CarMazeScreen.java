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

final class CarMazeScreen {
    private static final double MOVE_DISTANCE = 5;
    private static final double CAR_MARGIN = 1;
    private static final double CAR_WIDTH = 40;
    private static final double CAR_HEIGHT = 17;
    private static final long ANIMATION_INTERVAL_NANOS = 15_000_000L;
    private static final int ANIMATION_STEP = 5;
    private static final double START_X = 0;
    private static final double START_Y = 0;

    private CarMazeScreen() {
    }

    static Pane createContent() {
        Image mazeImage = new Image(CarMazeScreen.class.getResourceAsStream("/images/maze2.png"));
        Image carImage = new Image(CarMazeScreen.class.getResourceAsStream("/images/car.png"));
        PixelReader mazePixels = mazeImage.getPixelReader();

        ImageView maze = new ImageView(mazeImage);
        ImageView car = new ImageView(carImage);
        car.setFitWidth(CAR_WIDTH);
        car.setFitHeight(CAR_HEIGHT);
        car.setPreserveRatio(false);
        car.setX(START_X);
        car.setY(START_Y);

        Pane root = new Pane(maze, car);
        root.setFocusTraversable(true);

        Button autoSolveButton = new Button("Auto Solve");
        autoSolveButton.setLayoutX(10);
        autoSolveButton.setLayoutY(mazeImage.getHeight() + 10);
        root.getChildren().add(autoSolveButton);

        AnimationTimer[] animation = new AnimationTimer[1];
        List<Position>[] path = new List[]{Collections.singletonList(
                new Position((int) car.getX(), (int) car.getY()))};
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

                for (int step = 0;
                     step < ANIMATION_STEP && nextPathPosition[0] < path[0].size();
                     step++) {
                    Position current = new Position((int) car.getX(), (int) car.getY());
                    Position next = path[0].get(nextPathPosition[0]++);
                    updateHeading(car, current, next);
                    car.setX(next.x);
                    car.setY(next.y);
                }
            }
        };

        autoSolveButton.setOnAction(event -> {
            animation[0].stop();
            car.setX(START_X);
            car.setY(START_Y);
            car.setRotate(0);
            path[0] = findPath(car, mazePixels, mazeImage);
            nextPathPosition[0] = 1;
            lastAnimationUpdate[0] = 0;
            animation[0].start();
            root.requestFocus();
        });

        root.setOnKeyPressed(event -> {
            animation[0].stop();
            double nextX = car.getX();
            double nextY = car.getY();

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

            if (canMove(car, nextX, nextY, mazePixels, mazeImage)) {
                Position current = new Position((int) car.getX(), (int) car.getY());
                Position next = new Position((int) nextX, (int) nextY);
                updateHeading(car, current, next);
                car.setX(nextX);
                car.setY(nextY);
            }
        });

        return root;
    }

    private static void updateHeading(ImageView car, Position current, Position next) {
        if (next.x > current.x) {
            car.setRotate(0);
        } else if (next.x < current.x) {
            car.setRotate(180);
        } else if (next.y > current.y) {
            car.setRotate(90);
        } else if (next.y < current.y) {
            car.setRotate(270);
        }
    }

    private static List<Position> findPath(ImageView car, PixelReader mazePixels, Image mazeImage) {
        Position start = new Position((int) car.getX(), (int) car.getY());
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
            if (isAtExit(current, car, mazePixels, mazeImage)) {
                exit = current;
                break;
            }

            for (int[] direction : directions) {
                Position next = new Position(
                        current.x + direction[0],
                        current.y + direction[1]);

                if (!previous.containsKey(next)
                        && canMove(car, next.x, next.y, mazePixels, mazeImage)) {
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

    private static boolean isAtExit(Position position, ImageView car,
                                    PixelReader mazePixels, Image mazeImage) {
        double rightEdge = position.x + CAR_WIDTH - CAR_MARGIN;
        double centerY = position.y + CAR_HEIGHT / 2.0;

        return rightEdge >= mazeImage.getWidth() - 1
                && isWalkable(mazeImage.getWidth() - 1, centerY, mazePixels, mazeImage);
    }

    private static boolean canMove(ImageView car, double x, double y,
                                   PixelReader mazePixels, Image mazeImage) {
        double left = x + CAR_MARGIN;
        double right = x + CAR_WIDTH - CAR_MARGIN;
        double top = y + CAR_MARGIN;
        double bottom = y + CAR_HEIGHT - CAR_MARGIN;

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
