package org.example.csc311_module3assignment_groupbased;

import javafx.animation.AnimationTimer;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

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
    private static final double LIGHT_RADIUS = 2.5;
    private static final double LIGHT_SIDE_OFFSET = 4;
    private static final double LIGHT_FRONT_OFFSET = 1;
    private static final long ANIMATION_INTERVAL_NANOS = 30_000_000L;
    private static final int PATH_STEP = 5;
    private static final double START_X = 0;
    private static final double START_Y = 0;
    private static final double EXIT_LEFT = 430;
    private static final double EXIT_TOP = 314;
    private static final double EXIT_BOTTOM = 343;

    private CarMazeScreen() {
    }

    static Pane createContent() {
        //Using the images and calling them to be used in tab
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

        //adding the head lights for the car
        Circle leftLight = createHeadlight();
        Circle rightLight = createHeadlight();
        updateHeadlights(leftLight, rightLight, car, 0);

        Pane root = new Pane(maze, car, leftLight, rightLight);
        root.setFocusTraversable(true);

        //adding the button that will auto-solve the maze
        Button autoSolveButton = new Button("Auto Solve");
        autoSolveButton.setLayoutX(10);
        autoSolveButton.setLayoutY(mazeImage.getHeight() + 10);
        root.getChildren().add(autoSolveButton);

        //Having animation to the car
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

                Position current = new Position((int) car.getX(), (int) car.getY());
                Position next = path[0].get(nextPathPosition[0]++);
                updateHeading(car, current, next);
                car.setX(next.x);
                car.setY(next.y);
                updateHeadlights(leftLight, rightLight, car, car.getRotate());
            }
        };

        //Setting up action for the button
        autoSolveButton.setOnAction(event -> {
            animation[0].stop();
            car.setX(START_X);
            car.setY(START_Y);
            car.setRotate(0);
            updateHeadlights(leftLight, rightLight, car, 0);
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

            Position current = new Position((int) car.getX(), (int) car.getY());
            Position next = new Position((int) nextX, (int) nextY);
            if (canMove(nextX, nextY, mazePixels, mazeImage)) {
                updateHeading(car, current, next);
                car.setX(nextX);
                car.setY(nextY);
                updateHeadlights(leftLight, rightLight, car, car.getRotate());
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

    private static Circle createHeadlight() {
        Circle headlight = new Circle(LIGHT_RADIUS, Color.YELLOW);
        headlight.setStroke(Color.GOLD);
        return headlight;
    }

    private static void updateHeadlights(Circle leftLight, Circle rightLight,
                                         ImageView car, double heading) {
        double x = car.getX();
        double y = car.getY();

        if (heading == 0) {
            leftLight.setCenterX(x + CAR_WIDTH + LIGHT_FRONT_OFFSET);
            leftLight.setCenterY(y + LIGHT_SIDE_OFFSET);
            rightLight.setCenterX(x + CAR_WIDTH + LIGHT_FRONT_OFFSET);
            rightLight.setCenterY(y + CAR_HEIGHT - LIGHT_SIDE_OFFSET);
        } else if (heading == 180) {
            leftLight.setCenterX(x - LIGHT_FRONT_OFFSET);
            leftLight.setCenterY(y + LIGHT_SIDE_OFFSET);
            rightLight.setCenterX(x - LIGHT_FRONT_OFFSET);
            rightLight.setCenterY(y + CAR_HEIGHT - LIGHT_SIDE_OFFSET);
        } else if (heading == 90) {
            leftLight.setCenterX(x + CAR_WIDTH / 2 - (CAR_HEIGHT / 2 - LIGHT_SIDE_OFFSET));
            leftLight.setCenterY(y + CAR_HEIGHT / 2 + CAR_WIDTH / 2 + LIGHT_FRONT_OFFSET);
            rightLight.setCenterX(x + CAR_WIDTH / 2 + (CAR_HEIGHT / 2 - LIGHT_SIDE_OFFSET));
            rightLight.setCenterY(y + CAR_HEIGHT / 2 + CAR_WIDTH / 2 + LIGHT_FRONT_OFFSET);
        } else {
            leftLight.setCenterX(x + CAR_WIDTH / 2 - (CAR_HEIGHT / 2 - LIGHT_SIDE_OFFSET));
            leftLight.setCenterY(y + CAR_HEIGHT / 2 - CAR_WIDTH / 2 - LIGHT_FRONT_OFFSET);
            rightLight.setCenterX(x + CAR_WIDTH / 2 + (CAR_HEIGHT / 2 - LIGHT_SIDE_OFFSET));
            rightLight.setCenterY(y + CAR_HEIGHT / 2 - CAR_WIDTH / 2 - LIGHT_FRONT_OFFSET);
        }
    }

    private static List<Position> findPath(ImageView car, PixelReader mazePixels, Image mazeImage) {
        Position start = new Position((int) car.getX(), (int) car.getY());
        ArrayDeque<Position> queue = new ArrayDeque<>();
        Map<Position, Position> previous = new HashMap<>();
        queue.add(start);
        previous.put(start, null);

        int[][] directions = {
                {PATH_STEP, 0},
                {-PATH_STEP, 0},
                {0, PATH_STEP},
                {0, -PATH_STEP}
        };

        Position exit = null;
        while (!queue.isEmpty()) {
            Position current = queue.remove();
            if (isAtExit(current, mazePixels, mazeImage)) {
                exit = current;
                break;
            }

            for (int[] direction : directions) {
                Position next = new Position(
                        current.x + direction[0],
                        current.y + direction[1]);

                if (!previous.containsKey(next)
                        && canMove(next.x, next.y, mazePixels, mazeImage)) {
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

    private static boolean isAtExit(Position position,
                                    PixelReader mazePixels, Image mazeImage) {
        double rightEdge = position.x + CAR_WIDTH - CAR_MARGIN;
        double centerY = position.y + CAR_HEIGHT / 2.0;

        return rightEdge >= EXIT_LEFT - CAR_MARGIN
                && centerY >= EXIT_TOP
                && centerY <= EXIT_BOTTOM
                && isWalkable(rightEdge, position.y + CAR_MARGIN, mazePixels, mazeImage)
                && isWalkable(rightEdge, position.y + CAR_HEIGHT - CAR_MARGIN,
                mazePixels, mazeImage);
    }

    //Allowing image of car to be moved
    private static boolean canMove(double x, double y,
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

        Color color = mazePixels.getColor(pixelX, pixelY);
        return color.getRed() > 0.8
                && color.getGreen() > 0.8
                && color.getBlue() > 0.8;
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
