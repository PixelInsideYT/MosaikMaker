package de.thriemer.mosaikmaker;

import me.tongfei.progressbar.ProgressBar;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ImageUtil {
    private static final Runtime rt = Runtime.getRuntime();
    private static final Map<BufferedImage, float[][]> imageArrayMap = new HashMap<>();
    private static final Map<BufferedImage, int[]> intImageArrayMap = new HashMap<>();


    public static BufferedImage loadImage() {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileFilter(new FileNameExtensionFilter("Bilder", "png", "PNG", "JPG", "jpg"));
        jfc.showOpenDialog(null);
        return loadImage(jfc.getSelectedFile());
    }

    public static int[][] splitIntoColorChannels(BufferedImage intRGB) {
        int[] img = getArrayFromImage(intRGB);
        int[][] channels = new int[img.length][3];
        for (int i = 0; i < img.length; i++) {
            channels[i] = intToRGB(img[i]);
        }
        return channels;
    }

    public static BufferedImage convertToLowerColorDepth(BufferedImage in, int einzelFarbtiefe) {
        int w = in.getWidth();
        int h = in.getHeight();
        int[] readArray = getArrayFromImage(in);
        BufferedImage rt = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] rtImageArray = ((DataBufferInt) rt.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < readArray.length; i++) {
            int[] colorOfPixel = intToRGB(readArray[i]);
            int newR = Useful.map(Useful.map(colorOfPixel[0], 0, 255, 0, einzelFarbtiefe), 0, einzelFarbtiefe, 0, 255);
            int newG = Useful.map(Useful.map(colorOfPixel[1], 0, 255, 0, einzelFarbtiefe), 0, einzelFarbtiefe, 0, 255);
            int newB = Useful.map(Useful.map(colorOfPixel[2], 0, 255, 0, einzelFarbtiefe), 0, einzelFarbtiefe, 0, 255);
            rtImageArray[i] = RGBToInt(newR, newG, newB);
        }
        return rt;
    }

    public static int[] getWriteableRasterFromImage(BufferedImage img) {
        return ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
    }

    public static int[] getArrayFromImage(BufferedImage img) {
        if (intImageArrayMap.containsKey(img)) {
            return intImageArrayMap.get(img);
        }
        int[] rt = new int[img.getWidth() * img.getHeight()];
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), rt, 0, img.getWidth());
        intImageArrayMap.put(img, rt);
        return rt;
    }

    public static void clearCache() {
        intImageArrayMap.clear();
        imageArrayMap.clear();
    }

    public static float[][] getCIELabFromImage(BufferedImage img, boolean useProgressBar) {
        if (imageArrayMap.containsKey(img)) {
            return imageArrayMap.get(img);
        }
        int[] rt = getArrayFromImage(img);
        float[][] cieLab = new float[rt.length][];
        var c = CIELab.getInstance();
        var stream = IntStream.range(0, rt.length).boxed();
        if (useProgressBar) {
            stream = ProgressBar.wrap(stream, "CIELab conversion");
        }
        stream.forEach(i -> cieLab[i] = c.fromRGB(toZeroOneColor(intToRGB(rt[i]))));
        imageArrayMap.put(img, cieLab);
        return cieLab;
    }

    public static float[] toZeroOneColor(int[] rgb) {
        float[] result = new float[rgb.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = rgb[i] / 255f;
        }
        return result;
    }


    public static List<BufferedImage> loadAllImagesFromFolder(File folder) {
        List<BufferedImage> imgList = null;
        String[] extensions = new String[]{"png", "jpg", "JPG", "PNG", "JPEG"};
        if (folder.isDirectory()) {
            File[] files = folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    for (String ext : extensions) {
                        if (name.endsWith("." + ext)) {
                            return (true);
                        }
                    }
                    return (false);
                }
            });
            imgList = Arrays.stream(files).parallel().map(f -> loadImage(f)).collect(Collectors.toList());
        }
        return imgList;
    }

    public static void saveImage(String name, BufferedImage img) {
        saveImage(name, "png", img);
    }

    public static void saveImage(String name, String type, BufferedImage img) {
        try {
            File f = new File(name + (name.endsWith(type) ? "" : "." + type));
            ImageIO.write(img, type, f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage loadImage(File f) {
        BufferedImage rtImg = null;
        if (f != null && f.exists()) {
            if (rt.freeMemory() < rt.totalMemory() / 3) {
                System.gc();
            }
            try {
                rtImg = convertToIntRgb(ImageIO.read(f));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rtImg;
    }

    public static BufferedImage loadImage(URL f) {
        BufferedImage rtImg = null;
        if (rt.freeMemory() < rt.totalMemory() / 2) {
            System.gc();
            System.out.println("Performed GC");
        }
        try {
            rtImg = convertToIntRgb(ImageIO.read(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rtImg;
    }

    public static BufferedImage loadImage(FileInputStream fis) {
        BufferedImage rtImg = null;
        if (rt.freeMemory() < rt.totalMemory() / 2) {
            System.gc();
            System.out.println("Performed GC");
        }
        try {
            rtImg = convertToIntRgb(ImageIO.read(fis));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rtImg;
    }

    public static BufferedImage loadImageFromWeb(String url) {
        BufferedImage loadImg = null;
        try {
            URL imagePath = new URL(url);
            loadImg = convertToIntRgb(ImageIO.read(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loadImg;
    }

    public static BufferedImage blurImage(BufferedImage tbi, int radius) {
        int size = radius * 2 + 1;
        float weight = 1.0f / (size * size);
        float[] data = new float[size * size];

        for (int i = 0; i < data.length; i++) {
            data[i] = weight;
        }

        Kernel kernel = new Kernel(size, size, data);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        // tbi is BufferedImage
        return op.filter(tbi, null);
    }

    public static BufferedImage convertToIntRgb(BufferedImage in) {
        return convertToType(in, BufferedImage.TYPE_INT_RGB);
    }

    public static BufferedImage convertToType(BufferedImage in, int type) {
        if (in.getType() != type) {
            BufferedImage rtImg = new BufferedImage(in.getWidth(), in.getHeight(), type);
            Graphics g = rtImg.getGraphics();
            g.drawImage(in, 0, 0, null);
            g.dispose();
            return rtImg;
        } else {
            return in;
        }
    }


    public static float[] getAVGAndSTD(int x, int y, int w, int h, BufferedImage img) {
        float[] avg = getAvgColor(x, y, w, h, img);
        float[] std = new float[3];
        int[] rgbArray = getArrayFromImage(img);
        int imageWidth = img.getWidth();
        int counter = 0;
        for (int xi = x; xi < x + w; xi++) {
            for (int yi = y; yi < y + h; yi++) {
                int[] color = intToRGB(rgbArray[yi * imageWidth + xi]);
                for (int c = 0; c < color.length; c++) {
                    std[c] += Math.pow(color[c] - avg[c], 2);
                }
                counter++;
            }
        }
        for (int c = 0; c < std.length; c++) {
            std[c] = (float) Math.sqrt(std[c] / ((float) counter - 1f));
        }
        return new float[]{
                avg[0], avg[1], avg[2],
                std[0], std[1], std[2]
        };
    }

    public static float[] getCIELabAVGAndSTD(int x, int y, int w, int h, BufferedImage img) {
        float[] avg = getAvgCIELabColor(x, y, w, h, img);
        float[] std = new float[3];
        float[][] cieLab = getCIELabFromImage(img, false);
        int imageWidth = img.getWidth();
        int counter = 0;
        for (int xi = x; xi < x + w; xi++) {
            for (int yi = y; yi < y + h; yi++) {
                float[] color = cieLab[yi * imageWidth + xi];
                for (int c = 0; c < color.length; c++) {
                    std[c] += Math.pow(color[c] - avg[c], 2);
                }
                counter++;
            }
        }
        for (int c = 0; c < std.length; c++) {
            std[c] = (float) Math.sqrt(std[c] / ((float) counter - 1f));
        }
        return new float[]{
                avg[0], avg[1], avg[2],
                std[0], std[1], std[2]
        };
    }


    public static int[] getMinAvgMaxColor(int x, int y, int w, int h, BufferedImage img) {
        int[] rgbArray = new int[img.getWidth() * img.getHeight()];
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), rgbArray, 0, img.getWidth());
        int[] avgSum = new int[3];
        int[] minCol = new int[]{255, 255, 255};
        int[] maxCol = new int[]{0, 0, 0};
        int colorcounter = 0;
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight()) {
                    int i = yy * img.getWidth() + xx;
                    int[] rgb = intToRGB(rgbArray[i]);
                    for (int j = 0; j < 3; j++) {
                        avgSum[j] += rgb[j];
                        minCol[j] = Math.min(minCol[j], rgb[j]);
                        maxCol[j] = Math.max(maxCol[j], rgb[j]);
                        colorcounter++;
                    }
                }
            }
        }
        avgSum[0] = avgSum[0] / colorcounter;
        avgSum[1] = avgSum[1] / colorcounter;
        avgSum[2] = avgSum[2] / colorcounter;
        int avg = RGBToInt(avgSum);
        int min = RGBToInt(minCol);
        int max = RGBToInt(maxCol);
        return new int[]{min, avg, max};
    }

    public static float[] getAvgColor(int x, int y, int w, int h, BufferedImage img) {
        int[] rgbArray = getArrayFromImage(img);
        int imageWidth = img.getWidth();
        float[] avg = new float[3];
        int counter = 0;
        for (int xi = x; xi < x + w; xi++) {
            for (int yi = y; yi < y + h; yi++) {
                int[] color = intToRGB(rgbArray[yi * imageWidth + xi]);
                for (int c = 0; c < color.length; c++) {
                    avg[c] += color[c];
                }
                counter++;
            }
        }
        for (int c = 0; c < avg.length; c++) {
            avg[c] = avg[c] / (float) counter;
        }
        return avg;
    }

    public static float[] getAvgCIELabColor(int x, int y, int w, int h, BufferedImage img) {
        float[][] cieLabArray = getCIELabFromImage(img, false);
        int imageWidth = img.getWidth();
        float[] avg = new float[cieLabArray[0].length];
        int counter = 0;
        for (int xi = x; xi < x + w; xi++) {
            for (int yi = y; yi < y + h; yi++) {
                for (int c = 0; c < avg.length; c++) {
                    avg[c] += cieLabArray[yi * imageWidth + xi][c];
                }
                counter++;
            }
        }
        for (int c = 0; c < avg.length; c++) {
            avg[c] = avg[c] / (float) counter;
        }
        return avg;
    }

    public static BufferedImage makeSquared(BufferedImage in) {
        BufferedImage squared = null;
        if (in.getWidth() > in.getHeight()) {
            int wh2 = (in.getWidth() - in.getHeight()) / 2;
            squared = in.getSubimage(wh2, 0, in.getHeight(), in.getHeight());
        } else {
            int hw2 = (in.getHeight() - in.getWidth()) / 2;
            squared = in.getSubimage(0, hw2, in.getWidth(), in.getWidth());
        }
        return squared;
    }

    public static BufferedImage reScale(int size, BufferedImage in) {
        BufferedImage rescaled = new BufferedImage(size, size, in.getType());
        Graphics g = rescaled.getGraphics();
        g.drawImage(in, 0, 0, size, size, null);
        g.dispose();
        return rescaled;
    }

    public static BufferedImage reScale(float rate, BufferedImage in) {
        BufferedImage rescaled = new BufferedImage((int) Math.round(in.getWidth() * rate),
                (int) Math.round(in.getHeight() * rate), in.getType());
        Graphics g = rescaled.getGraphics();
        g.drawImage(in, 0, 0, (int) Math.round(in.getWidth() * rate), (int) Math.round(in.getHeight() * rate), null);
        g.dispose();
        return rescaled;
    }

    public static BufferedImage downScale(int width, int height, BufferedImage in) {
        return Scalr.resize(in, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, width, height);
    }


    public static int[] intToRGB(int c) {
        return new int[]{(c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF};
    }

    public static int RGBToInt(int... c) {
        return c[0] << 16 | c[1] << 8 | c[2];
    }

    public static BufferedImage convertToGrayScale(BufferedImage in) {
        BufferedImage rtImg = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_RGB);
        int[] raster = getWriteableRasterFromImage(rtImg);
        int[] input = getArrayFromImage(in);
        for (int i = 0; i < raster.length; i++) {
            int grey = Useful.getGreyScale(input[i]);
            raster[i] = RGBToInt(grey, grey, grey);
        }
        return rtImg;
    }

    public static BufferedImage convertToBinaryBW(BufferedImage in, int threshold) {
        BufferedImage rtImg = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_RGB);
        int[] raster = getWriteableRasterFromImage(rtImg);
        int[] input = getArrayFromImage(in);
        for (int i = 0; i < raster.length; i++) {
            raster[i] = Useful.getGreyScale(input[i]) > threshold ? Color.white.getRGB() : Color.black.getRGB();
        }
        return rtImg;
    }

    public static BufferedImage fixToAspectRatio(BufferedImage img, float aspectRatio) {
        float currentAS = img.getWidth() / (float) img.getHeight();
        if (currentAS > aspectRatio) {
            //cut in the width
            int cutAmount = img.getWidth() - (int) (img.getHeight() * aspectRatio);
            BufferedImage fitting = new BufferedImage(img.getWidth() - cutAmount, img.getHeight(), img.getType());
            Graphics g = fitting.getGraphics();
            g.drawImage(img, -cutAmount / 2, 0, null);
            g.dispose();
            return fitting;
        } else {
            //cut the height
            int cutAmount = (int) ((aspectRatio * img.getHeight() - img.getWidth()) / aspectRatio);
            BufferedImage fitting = new BufferedImage(img.getWidth(), img.getHeight() - cutAmount, img.getType());
            Graphics g = fitting.getGraphics();
            g.drawImage(img, 0, -cutAmount / 2, null);
            g.dispose();
            return fitting;
        }
    }
}
