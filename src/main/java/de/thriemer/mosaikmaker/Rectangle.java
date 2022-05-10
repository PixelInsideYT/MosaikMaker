package de.thriemer.mosaikmaker;

import java.awt.image.BufferedImage;

public class Rectangle {

    int posX;
    int posY;
    int width;
    int height;

    public Rectangle(int posX, int posY, int width, int height) {
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
    }

    float avgError = -1;

    int getArea() {
        return width * height;
    }

    float getAvgError(BufferedImage image) {
        if (avgError == -1) {
            float[] avgAndStd = ImageUtil.getCIELabAVGAndSTD(posX, posY, width, height, image);
            avgError = (avgAndStd[3] + avgAndStd[4] + avgAndStd[5]) / 3f;
        }
        return avgError;
    }

    float getAspectRatio() {
        return width / (float) height;
    }

    //avg, error


}
