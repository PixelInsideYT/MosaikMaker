package de.thriemer.mosaikmaker;

import me.tongfei.progressbar.ProgressBar;

import java.awt.image.BufferedImage;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImagePartitioner {

    private int minArea = 28 * 28;
    private int maxSize = 400 * 400;

    private List<Rectangle> spaceFillingRectangles = new ArrayList<>();
    private List<Rectangle> finishedSpaceFillingRectangles = new ArrayList<>();
    private BufferedImage baseImage;

    public ImagePartitioner(BufferedImage baseImage) {
        this.baseImage = baseImage;
        spaceFillingRectangles.add(new Rectangle(0, 0, baseImage.getWidth(), baseImage.getHeight()));
    }

    public final List<Rectangle> getPartionedRectangles() {
        return finishedSpaceFillingRectangles;
    }

    private static final float CIE_STARTING_STD_DEV = 70;

    public void partitionImage(float wantedError) {
        partitionImageBySize(maxSize);
        calculateEveryPartitionError();
        partitionImageByColor(wantedError);
        finishedSpaceFillingRectangles.addAll(spaceFillingRectangles);
        spaceFillingRectangles.clear();
    }

    private void splitRectangle(Rectangle r) {
        if (spaceFillingRectangles.contains(r)) {
            spaceFillingRectangles.remove(r);
            spaceFillingRectangles.addAll(createNewRectangles(r));
        }
    }

    private void partitionImageBySize(int maxSize) {
        int maxRectSize = Integer.MAX_VALUE;
        spaceFillingRectangles.sort((r1, r2) -> Integer.compare(r2.getArea(), r1.getArea()));
        int rectEstimate = baseImage.getWidth() * baseImage.getHeight() / maxSize;
        try (ProgressBar pb = new ProgressBar("Partition by size", rectEstimate)) {
            while (maxRectSize > maxSize) {
                spaceFillingRectangles.sort((r1, r2) -> Integer.compare(r2.getArea(), r1.getArea()));
                maxRectSize = spaceFillingRectangles.get(0).getArea();
                splitRectangle(spaceFillingRectangles.get(0));
                pb.stepBy(3);
            }
        }
    }

    private void partitionImageByColor(float wantedError) {
        float error = Float.MAX_VALUE;
        try (ProgressBar pb = new ProgressBar("Partition by color", (int) (CIE_STARTING_STD_DEV - wantedError))) {
            while (error > wantedError && !spaceFillingRectangles.isEmpty()) {
                filterRectangleWithSmallArea();
                spaceFillingRectangles.sort((r1, r2) -> Float.compare(r2.getAvgError(baseImage), r1.getAvgError(baseImage)));
                error = spaceFillingRectangles.get(0).getAvgError(baseImage);
                splitRectangle(spaceFillingRectangles.get(0));
                pb.stepTo((long) (CIE_STARTING_STD_DEV - error));
            }
        }
    }

    private void calculateEveryPartitionError() {
        ImageUtil.getCIELabFromImage(baseImage, true);
        ProgressBar.wrap(spaceFillingRectangles.parallelStream(), "Calculate error")
                .forEach(r -> r.getAvgError(baseImage));
    }

    private void filterRectangleWithSmallArea() {
        var itr = spaceFillingRectangles.iterator();
        while (itr.hasNext()) {
            var rect = itr.next();
            if (rect.width * rect.height < minArea) {
                itr.remove();
                finishedSpaceFillingRectangles.add(rect);
            }
        }
    }

    private List<Rectangle> createNewRectangles(Rectangle removed) {
        if (Math.random() > 0.6) {
            return splitIntoSameAspectRatio(removed);
        }
        return splitIntoOtherAspectRatio(removed);
    }

    private List<Rectangle> splitIntoSameAspectRatio(Rectangle removed) {
        float w2f = removed.width / 2f;
        float h2f = removed.height / 2f;
        int w2 = (int) Math.ceil(w2f);
        int h2 = (int) Math.ceil(h2f);

        var n1 = new Rectangle(removed.posX, removed.posY, (int) w2f, (int) h2f);
        var n2 = new Rectangle(removed.posX + (int) w2f, removed.posY, w2, h2);
        var n3 = new Rectangle(removed.posX, removed.posY + (int) h2f, w2, h2);
        var n4 = new Rectangle(removed.posX + (int) w2f, removed.posY + (int) h2f, w2, h2);
        return List.of(n1, n2, n3, n4);
    }

    private List<Rectangle> splitIntoOtherAspectRatio(Rectangle r) {
        if (r.width > r.height) {
            return splitIntoPortrait(r);
        }
        return splitIntoLandscape(r);
    }

    private List<Rectangle> splitIntoPortrait(Rectangle r) {
        int hn = r.height;
        float bn = r.width / 2f;
        List<Rectangle> portraitRects = new ArrayList<>();
        portraitRects.add(new Rectangle(r.posX, r.posY, (int) bn, hn));
        portraitRects.add(new Rectangle(r.posX + (int) bn, r.posY, (int) Math.ceil(bn), hn));
        return portraitRects;
    }

    private List<Rectangle> splitIntoLandscape(Rectangle r) {
        int bn = r.width;
        float hn = r.height / 2f;
        List<Rectangle> landscapeRects = new ArrayList<>();
        landscapeRects.add(new Rectangle(r.posX, r.posY, bn, (int) hn));
        landscapeRects.add(new Rectangle(r.posX, r.posY + (int) hn, bn, (int) Math.ceil(hn)));
        return landscapeRects;
    }


}
