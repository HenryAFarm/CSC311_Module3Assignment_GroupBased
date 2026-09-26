package org.example.csc311_module3assignment_groupbased;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    private static final int STEP = 5;
    private static final double OPEN_THRESHOLD = 0.80;

    @Override
    public void start(Stage stage) {
        MazeWorld world = new MazeWorld("/images/maze.png");
        BorderPane root = new BorderPane();
        root.setCenter(world.arena());
        root.setBottom(world.statusLabel());

        Scene scene = new Scene(root, world.width(), world.height() + 32);
        scene.setOnKeyPressed(event -> world.moveByKey(event.getCode()));

        stage.setTitle("Step 1 - Robot Maze");
        stage.setScene(scene);
        stage.show();
        world.arena().requestFocus();
    }

    private enum Direction {
        UP(0, -STEP),
        DOWN(0, STEP),
        LEFT(-STEP, 0),
        RIGHT(STEP, 0);

        final int dx;
        final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    private abstract static class MazeActor {
        protected double x;
        protected double y;

        MazeActor(double x, double y) {
            this.x = x;
            this.y = y;
        }

        abstract Node node();

        abstract int width();

        abstract int height();

        void moveTo(double nx, double ny) {
            x = nx;
            y = ny;
            node().setLayoutX(nx);
            node().setLayoutY(ny);
        }
    }

    private static final class RobotActor extends MazeActor {
        private final ImageView robotView;

        RobotActor(double x, double y) {
            super(x, y);
            robotView = new ImageView(new Image(HelloApplication.class.getResourceAsStream("/images/robot.png")));
            robotView.setFitWidth(26);
            robotView.setFitHeight(26);
            robotView.setPreserveRatio(true);
            robotView.setLayoutX(x);
            robotView.setLayoutY(y);
        }

        @Override
        Node node() {
            return robotView;
        }

        @Override
        int width() {
            return (int) Math.ceil(robotView.getFitWidth());
        }

        @Override
        int height() {
            return (int) Math.ceil(robotView.getFitHeight());
        }
    }

    private static final class MazeWorld {
        private final Image mazeImage;
        private final PixelReader pixels;
        private final ImageView mazeView;
        private final Pane arena;
        private final Label status;
        private final RobotActor robot;

        MazeWorld(String mazeResourcePath) {
            mazeImage = new Image(HelloApplication.class.getResourceAsStream(mazeResourcePath));
            pixels = mazeImage.getPixelReader();
            mazeView = new ImageView(mazeImage);

            robot = new RobotActor(0, 0);
            int[] start = findStartPosition(robot.width(), robot.height());
            int startX = start[0];
            int startY = start[1];
            robot.moveTo(startX, startY);

            status = new Label("Use arrow keys. Movement is blocked by maze walls.");
            arena = new Pane(mazeView, robot.node());
            arena.setPrefSize(mazeImage.getWidth(), mazeImage.getHeight());
            arena.setFocusTraversable(true);
            arena.setOnMouseMoved(this::handleMouseMove);
        }

        Pane arena() {
            return arena;
        }

        Label statusLabel() {
            return status;
        }

        int width() {
            return (int) mazeImage.getWidth();
        }

        int height() {
            return (int) mazeImage.getHeight();
        }

        void moveByKey(KeyCode code) {
            Direction direction;
            switch (code) {
                case UP:
                    direction = Direction.UP;
                    break;
                case DOWN:
                    direction = Direction.DOWN;
                    break;
                case LEFT:
                    direction = Direction.LEFT;
                    break;
                case RIGHT:
                    direction = Direction.RIGHT;
                    break;
                default:
                    direction = null;
                    break;
            }
            if (direction != null) {
                tryMove(direction);
            }
        }

        private void tryMove(Direction direction) {
            double nx = robot.x + direction.dx;
            double ny = robot.y + direction.dy;
            if (canOccupy(nx, ny)) {
                robot.moveTo(nx, ny);
                status.setText(String.format("Robot at (%.0f, %.0f)", robot.x, robot.y));
            } else {
                status.setText("Blocked by wall.");
            }
        }

        private boolean canOccupy(double x, double y) {
            int inset = 3;
            return isAreaOpen((int) Math.floor(x) + inset, (int) Math.floor(y) + inset,
                    robot.width() - (2 * inset), robot.height() - (2 * inset));
        }

        private boolean isWalkable(int px, int py) {
            Color c = pixels.getColor(px, py);
            return c.getRed() >= OPEN_THRESHOLD
                    && c.getGreen() >= OPEN_THRESHOLD
                    && c.getBlue() >= OPEN_THRESHOLD;
        }

        private int[] findStartPosition(int robotWidth, int robotHeight) {
            int width = (int) mazeImage.getWidth();
            int height = (int) mazeImage.getHeight();
            for (int y = 0; y <= height - robotHeight; y++) {
                for (int x = 0; x <= width - robotWidth; x++) {
                    if (isAreaOpen(x, y, robotWidth, robotHeight)) {
                        return new int[]{x, y};
                    }
                }
            }
            throw new IllegalStateException("No open starting area found in maze image");
        }

        private boolean isAreaOpen(int x, int y, int width, int height) {
            int maxX = (int) mazeImage.getWidth() - 1;
            int maxY = (int) mazeImage.getHeight() - 1;
            if (x < 0 || y < 0 || x + width - 1 > maxX || y + height - 1 > maxY) {
                return false;
            }

            int stepX = Math.max(1, width / 6);
            int stepY = Math.max(1, height / 6);
            for (int sampleX = 0; sampleX <= width - 1; sampleX += stepX) {
                for (int sampleY = 0; sampleY <= height - 1; sampleY += stepY) {
                    if (!isWalkable(x + sampleX, y + sampleY)) {
                        return false;
                    }
                }
            }
            return isWalkable(x + width - 1, y)
                    && isWalkable(x + (width / 2), y)
                    && isWalkable(x, y + height - 1)
                    && isWalkable(x, y + (height / 2))
                    && isWalkable(x + width - 1, y + (height / 2))
                    && isWalkable(x + (width / 2), y + height - 1)
                    && isWalkable(x + width - 1, y + height - 1);
        }

        private void handleMouseMove(MouseEvent event) {
            status.setText(String.format("Mouse (%.0f, %.0f) | Robot (%.0f, %.0f)",
                    event.getX(), event.getY(), robot.x, robot.y));
        }
    }
}
