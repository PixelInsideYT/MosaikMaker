package de.thriemer.mosaikmaker;

import me.tongfei.progressbar.ProgressBar;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MosaikMaker {

    private static final float LANDSCAPE_ASPECT_RATIO = 1188f / 841f;
    private static final float PORTRAIT_ASPECT_RATIO = 1f / LANDSCAPE_ASPECT_RATIO;

    private static final String BASE_DIRECTORY = "/home/linus/Repositories/IdeaProjects/MosaikMaker/src/main/resources";
    Map<BufferedImage, Map<Dimension, BufferedImage>> imageDownscaledMap = new ConcurrentHashMap<>();
    BufferedImage baseImage;
    List<LoadedImage> images;

    Map<String, float[]> nameCIEAverageMap = new HashMap<>();

    void loadImageCIEAverageCache() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(MosaikMaker.class.getResourceAsStream("/avgCIELAB")));
        nameCIEAverageMap = reader.lines()
                .map(l -> l.split("\\["))
                .collect(Collectors.toMap(l -> l[0], l -> parseFloatArray(l[1])));
    }

    float[] parseFloatArray(String a) {
        String[] content = a.split(",");
        float[] result = new float[content.length];
        for (int i = 0; i < content.length; i++) {
            result[i] = Float.parseFloat(content[i].trim());
        }
        return result;
    }

    void loadBaseImage() throws IOException {
        var baseImageDialog = new JFileChooser(BASE_DIRECTORY);
        baseImageDialog.showOpenDialog(null);
        var smolBaseImage = convertToFittingAspectRatio(ImageIO.read(baseImageDialog.getSelectedFile()));
        baseImage = ImageUtil.reScale(4f, smolBaseImage);
    }

    void loadImage() {
        loadImageCIEAverageCache();
        var folder = new JFileChooser(BASE_DIRECTORY);
        folder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folder.showOpenDialog(null);
        var files = folder.getSelectedFile().listFiles();
        var imageLoader = ProgressBar.wrap(Arrays.stream(files).parallel(), "Loading Images");
        images = imageLoader
                .map(f -> {
                    LoadedImage loadedImage;
                    BufferedImage img = convertToFittingAspectRatio(ImageUtil.loadImage(f));
                    if (nameCIEAverageMap.containsKey(f.getName())) {
                        loadedImage = new LoadedImage(img, nameCIEAverageMap.get(f.getName()));
                    } else {
                        loadedImage = new LoadedImage(img);
                    }
                    ImageUtil.clearCache();
                    return loadedImage;
                }).collect(Collectors.toList());
    }


    BufferedImage convertToFittingAspectRatio(BufferedImage img) {
        if (img.getWidth() > img.getHeight()) {
            return ImageUtil.fixToAspectRatio(img, LANDSCAPE_ASPECT_RATIO);
        }
        return ImageUtil.fixToAspectRatio(img, PORTRAIT_ASPECT_RATIO);
    }


    BufferedImage getDownscaledOf(BufferedImage image, int width, int height) {
        Dimension d = new Dimension(width, height);
        var downscaledMap = imageDownscaledMap.get(image);
        if (downscaledMap != null && downscaledMap.containsKey(d)) {
            return downscaledMap.get(d);
        }
        BufferedImage small = ImageUtil.downScale(width, height, image);
        if (downscaledMap == null) {
            imageDownscaledMap.put(image, new ConcurrentHashMap<>());
        }
        imageDownscaledMap.get(image).put(d, small);
        return small;
    }


    void createImageFromSpaceFillingRects(List<Rectangle> rects) {
        BufferedImage image = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        var drawingRects = ProgressBar.wrap(rects.parallelStream(), "Generating image");
        drawingRects.forEach(r -> {
            float[] avgCIE = ImageUtil.getAvgCIELabColor(r.posX, r.posY, r.width, r.height, baseImage);
            var bestFit = getImageWithLeastError(avgCIE, r.width / (float) r.height);
            var scaled = getDownscaledOf(bestFit, r.width, r.height);
            g.drawImage(scaled, r.posX, r.posY, null);
        });
        ImageUtil.saveImage("lowRes", image);
    }

    BufferedImage getImageWithLeastError(float[] cie, float aspectRatio) {
        var threadSafe= images.stream()
                .filter(i -> {
                    boolean imgIsPortrait = i.aspectRatio < 1;
                    boolean rectIsPortrait = aspectRatio < 1;
                    return imgIsPortrait == rectIsPortrait;
                })
                .sorted((i1, i2) -> Float.compare(i1.getMSE(cie), i2.getMSE(cie)))
                .toList();
        int index = Math.random() > 0.5 ? 1 : 0;
        return threadSafe.get(index).image;
    }

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        var maker = new MosaikMaker();
        maker.loadBaseImage();
        maker.loadImage();
        ImagePartitioner imagePartitioner = new ImagePartitioner(maker.baseImage);
        imagePartitioner.partitionImage(3f);
        maker.createImageFromSpaceFillingRects(imagePartitioner.getPartionedRectangles());
        long millis = System.currentTimeMillis() - start;
        System.out.printf("%d min, %d sec%n",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

}
