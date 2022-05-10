package de.thriemer.mosaikmaker;

import javax.swing.*;
import java.awt.*;

public class Useful {

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static JFrame createFrame(String name, int width, int height, boolean fullscreen) {
        JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        if (fullscreen) {
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
            device.setFullScreenWindow(frame);
        }
        frame.setVisible(true);
        return frame;
    }

    public static double clamp(double in, double min, double max) {
        return in < min ? min : (in > max ? max : in);
    }

    public static float clamp(float in, float min, float max) {
        return in < min ? min : (in > max ? max : in);
    }

    public static int clamp(int in, int min, int max) {
        return in < min ? min : (in > max ? max : in);
    }

    public static int map(int x, int in_min, int in_max, int out_min, int out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public static float map(float x, float in_min, float in_max, float out_min, float out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public static int[] intToRGB(int c) {
        return new int[]{(c >> 16) & 0x000000FF, (c >> 8) & 0x000000FF, c & 0x000000FF};
    }

    public static int RGBToInt(int[] rgb) {
        return rgb[0] << 16 | rgb[1] << 8 | rgb[2];
    }

    public static int getGreyScale(int rgb) {
        int[] c = intToRGB(rgb);
        int rt = 0;
        for (int i = 0; i < c.length; i++) rt += c[i];
        rt /= 3;
        return rt;
    }

    public static float mix(float a, float b, float mix) {
        return a * mix + (1f - mix) * b;
    }
}
