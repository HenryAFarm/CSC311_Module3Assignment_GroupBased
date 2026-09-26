package org.example.csc311_module3assignment_groupbased;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.animation.AnimationTimer;

import java.util.*;

public class CarMaze {
    private static final double MOVE_DISTANCE = 5;

    public static Pane createContent() {

        Image mazeImage = new Image(
                CarMaze.class.getResourceAsStream("/images/maze2.png")
        );

        ImageView maze = new ImageView(mazeImage);
        PixelReader pixels = mazeImage.getPixelReader();

        //Body
        Rectangle body = new Rectangle(40, 20);
        body.setFill(Color.RED);

        //front
        Polygon front = new Polygon(
                40.0, 0.0,
                50.0, 10.0,
                40.0, 20.0
        );
        front.setFill(Color.RED);

        //wheels
        Circle wheel1 = new Circle(10, 2, 4, Color.BLACK);
        Circle wheel2 = new Circle(10, 18, 4, Color.BLACK);
        Circle wheel3 = new Circle(30, 2, 4, Color.BLACK);
        Circle wheel4 = new Circle(30, 18, 4, Color.BLACK);

        //headlight
        Circle light1 = new Circle(40, 5, 3, Color.YELLOW);
        Circle light2 = new Circle(40, 15, 3, Color.YELLOW);

        Pane car = new Pane();

        car.getChildren().addAll(
                body,
                front,
                wheel1,
                wheel2,
                wheel3,
                wheel4,
                light1,
                light2
        );

        //Starting position
        car.setLayoutX(15);
        car.setLayoutY(260);

        Pane root = new Pane();
        root.getChildren().addAll(maze, car);

        root.setFocusTraversable(true);

        root.setOnKeyPressed(event -> {

            double newX = car.getLayoutX();
            double newY = car.getLayoutY();

            if (event.getCode() == KeyCode.LEFT) {
                newX -= MOVE_DISTANCE;
                car.setRotate(180);
            }
            else if (event.getCode() == KeyCode.RIGHT) {
                newX += MOVE_DISTANCE;
                car.setRotate(0);
            }
            else if (event.getCode() == KeyCode.UP) {
                newY -= MOVE_DISTANCE;
                car.setRotate(270);
            }
            else if (event.getCode() == KeyCode.DOWN) {
                newY += MOVE_DISTANCE;
                car.setRotate(90);
            }
            else {
                return;
            }

            if (newX < 0 ||
                    newY < 0 ||
                    newX + 50 > mazeImage.getWidth() ||
                    newY + 20 > mazeImage.getHeight()) {
                return;
            }

            if (canMove(car, newX, newY, pixels, mazeImage)) {
                car.setLayoutX(newX);
                car.setLayoutY(newY);
            }
        });

        root.requestFocus();

        List<double[]> path = findPath(car, pixels, mazeImage);

        AnimationTimer timer = new AnimationTimer() {

            private int index = 0;
            private long lastMove = 0;

            @Override
            public void handle(long now) {

                if (index >= path.size()) {
                    stop();
                    return;
                }

                if (now - lastMove < 50_000_000) {
                    return;
                }

                double[] position = path.get(index);

                double oldX = car.getLayoutX();
                double oldY = car.getLayoutY();

                car.setLayoutX(position[0]);
                car.setLayoutY(position[1]);

                // Change car direction
                if (position[0] > oldX) {
                    car.setRotate(0);
                }
                else if (position[0] < oldX) {
                    car.setRotate(180);
                }
                else if (position[1] < oldY) {
                    car.setRotate(270);
                }
                else if (position[1] > oldY) {
                    car.setRotate(90);
                }

                index++;
                lastMove = now;
            }
        };

        timer.start();

        return root;
    }

    private static boolean canMove(
            Pane car,
            double x,
            double y,
            PixelReader pixels,
            Image mazeImage) {

        double left = x + 1;
        double right = x + 49;
        double top = y + 1;
        double bottom = y + 19;

        double middleX = x + 25;
        double middleY = y + 10;

        return isPath(left, top, pixels, mazeImage)
                && isPath(middleX, top, pixels, mazeImage)
                && isPath(right, top, pixels, mazeImage)
                && isPath(left, middleY, pixels, mazeImage)
                && isPath(right, middleY, pixels, mazeImage)
                && isPath(left, bottom, pixels, mazeImage)
                && isPath(middleX, bottom, pixels, mazeImage)
                && isPath(right, bottom, pixels, mazeImage);
    }

    private static boolean isPath(
            double x,
            double y,
            PixelReader pixels,
            Image mazeImage) {

        int pixelX = (int) x;
        int pixelY = (int) y;

        if (pixelX < 0 || pixelY < 0
                || pixelX >= mazeImage.getWidth()
                || pixelY >= mazeImage.getHeight()) {

            return false;
        }

        Color color = pixels.getColor(pixelX, pixelY);

        if (color.getRed() < 0.2
                && color.getGreen() < 0.3
                && color.getBlue() > 0.5) {
            return false;
        }

        return true;
    }

    private static boolean isExit(
            double x,
            double y,
            PixelReader pixels,
            Image mazeImage) {

        int pixelX = (int) x;
        int pixelY = (int) y;

        if (pixelX < 0 || pixelY < 0
                || pixelX >= mazeImage.getWidth()
                || pixelY >= mazeImage.getHeight()) {

            return false;
        }

        Color color = pixels.getColor(pixelX, pixelY);

        // Purple exit
        return color.getRed() > 0.3
                && color.getBlue() > 0.3
                && color.getGreen() < 0.3;
    }

    private static List<double[]> findPath(
            Pane car,
            PixelReader pixels,
            Image mazeImage) {

        Queue<double[]> queue = new LinkedList<>();
        Map<String, String> previous = new HashMap<>();
        Map<String, double[]> positions = new HashMap<>();

        double startX = car.getLayoutX();
        double startY = car.getLayoutY();

        String start = startX + "," + startY;

        queue.add(new double[]{startX, startY});
        positions.put(start, new double[]{startX, startY});

        double[] directions = {
                MOVE_DISTANCE, 0,
                -MOVE_DISTANCE, 0,
                0, MOVE_DISTANCE,
                0, -MOVE_DISTANCE
        };

        String end = null;

        while (!queue.isEmpty()) {

            double[] current = queue.remove();

            double x = current[0];
            double y = current[1];

            if (isExit(x + 25, y + 10, pixels, mazeImage)) {
                end = x + "," + y;
                break;
            }

            for (int i = 0; i < directions.length; i += 2) {

                double newX = x + directions[i];
                double newY = y + directions[i + 1];

                if (!canMove(car, newX, newY, pixels, mazeImage)) {
                    continue;
                }

                String key = newX + "," + newY;

                if (positions.containsKey(key)) {
                    continue;
                }

                positions.put(key, new double[]{newX, newY});
                previous.put(key, x + "," + y);

                queue.add(new double[]{newX, newY});
            }
        }

        List<double[]> path = new ArrayList<>();

        if (end == null) {
            return path;
        }

        String current = end;

        while (!current.equals(start)) {

            path.add(positions.get(current));
            current = previous.get(current);
        }

        Collections.reverse(path);

        return path;
    }
}