package de.thriemer.mosaikmaker;

import java.awt.image.BufferedImage;

public class LoadedImage {

    int width;
    int height;
    float aspectRatio;
    BufferedImage image;
    float[] cieAverage;

    public LoadedImage(BufferedImage image, float[] cieAverage) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.aspectRatio = width / (float) height;
        this.cieAverage = cieAverage;
    }

    public LoadedImage(BufferedImage image) {
        this(image, ImageUtil.getAvgCIELabColor(0, 0, image.getWidth(), image.getHeight(), image));
    }


    float getMSE(float[] otherCIE) {
        float summedError = 0;
        for (int i = 0; i < otherCIE.length; i++) {
            summedError += (float) Math.pow(otherCIE[i] - cieAverage[i], 2);
        }
        return summedError / (float) otherCIE.length;
    }

}
