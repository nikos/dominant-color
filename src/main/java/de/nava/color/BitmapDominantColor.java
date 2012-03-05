package de.nava.color;

import com.google.common.primitives.Doubles;
import com.google.common.util.concurrent.AtomicLongMap;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math.stat.descriptive.moment.Skewness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Analyse colors of a given bitmap.
 *
 * Inspired by an blog post of Jared Allen (http://chironexsoftware.com/blog/?p=60)
 *
 * @author Niko Schmuck
 */
public class BitmapDominantColor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapDominantColor.class);


    public static void main(String args[]) throws Exception {
        File imgDir = new File("src/test/resources/sample-images");
        
        //File img = new File(imgDir, "0955.jpg"); // mostly green
        File img = new File(imgDir, "0793.jpg"); // multi-colored red/orange/yellow
        //File img = new File(imgDir, "0241.jpg"); // black jeans on white/grey background
        //File img = new File(imgDir, "0443.jpg"); // half yellow/black

        Result result = analyseColors(img);
        LOGGER.info("Color code is #{}", result.getHexCode());
    }

    public static Result analyseColors(File file) throws IOException {
        LOGGER.info("Starting to analyse colors from {}", file.getName());
        ImageInputStream is = ImageIO.createImageInputStream(file);
        Iterator<ImageReader> iter = ImageIO.getImageReaders(is);

        if (!iter.hasNext()) {
            throw new IllegalArgumentException("Cannot load the specified file " + file);
        }
        ImageReader imageReader = iter.next();
        imageReader.setInput(is);

        BufferedImage image = imageReader.read(0);

        int height = image.getHeight();
        int width = image.getWidth();

        Map<Color, Double> colorDist = new HashMap<Color, Double>();
        AtomicLongMap<Color> colorCount = AtomicLongMap.create();

        final int alphaThershold = 10;
        long pixelCount = 0;
        long avgAlpha = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int[] rgbArr = getRGBArr(rgb);

                if (rgbArr[0] <= alphaThershold)
                   continue; //ignore

                avgAlpha += rgbArr[0];

                Color clr = new Color(rgbArr[1], rgbArr[2], rgbArr[3]);
                colorCount.getAndIncrement(clr);
                if (!colorDist.containsKey(clr)) {
                    double dist = 0.0d;

                    for (int y2 = 0; y2 < height; y2++) {
                        for (int x2 = 0; x2 < width; x2++) {
                            int rgb2 = image.getRGB(x2, y2);
                            int[] rgbArr2 = getRGBArr(rgb2);

                            if (rgbArr2[0] <= alphaThershold)
                                continue; //ignore

                            dist += Math.sqrt(Math.pow((double) (rgbArr2[1] - rgbArr[1]), 2) +
                                              Math.pow((double) (rgbArr2[2] - rgbArr[2]), 2) +
                                              Math.pow((double) (rgbArr2[3] - rgbArr[3]), 2));
                        } // for-x2
                    } // inner for-y2 loop

                    colorDist.put(clr, dist);
                }
                pixelCount++;
            } // for-x
        } // outer for-y loop

        LOGGER.info("COUNTER {} <-> {}", colorCount.sum(), pixelCount);

        // clamp alpha
        avgAlpha = avgAlpha / pixelCount;
        if (avgAlpha >= (255 - alphaThershold))
            avgAlpha = 255;

        // sort RGB distances
        ValueComparator bvc = new ValueComparator(colorDist);
        TreeMap<Color, Double> sorted_map = new TreeMap<Color, Double>(bvc);
        sorted_map.putAll(colorDist);

        // take weighted average of top 2% colors
        double threshold = 0.02;
        int nrToThreshold = Math.max(1, (int)(colorDist.size() * threshold));
        int mostThreshold = Math.max(1, (int)(colorDist.size() * 0.8));
        LOGGER.info("--> include {} out of {} values", nrToThreshold, colorDist.size());
        Map<Color, Double> clrsDist = new HashMap<Color, Double>();
        java.util.List<Double> topDist = new ArrayList<Double>();
        java.util.List<Double> mostDist = new ArrayList<Double>();
        java.util.List<Double> allDist = new ArrayList<Double>();
        int i = 0;
        for (Map.Entry<Color, Double> e : sorted_map.entrySet()) {
            Double distance = 1.0d / Math.max(1.0, e.getValue());
            if (i < nrToThreshold) {
                Color clr = e.getKey();
                clrsDist.put(clr, distance);
                topDist.add(e.getValue());
            }
            if (i < mostThreshold) {
                mostDist.add(e.getValue());
            }
            allDist.add(e.getValue());
            i++;
        }

        // calculate statistics
        double[] allDistsArr = Doubles.toArray(allDist);
        double allDistsMean = StatUtils.mean(allDistsArr);
        double allDistsVariance = StatUtils.variance(allDistsArr);

        Skewness skewness = new Skewness();
        double skAll = skewness.evaluate(allDistsArr);
        double skMost = skewness.evaluate(Doubles.toArray(mostDist));
        Kurtosis kurtosis = new Kurtosis();
        double kurtAll = kurtosis.evaluate(allDistsArr);
        double kurtMost = kurtosis.evaluate(Doubles.toArray(mostDist));

        double[] topDistsArr = Doubles.toArray(topDist);
        double topDistsMean = StatUtils.mean(topDistsArr);
        double topDistsVariance = StatUtils.variance(topDistsArr);

        //
        double sumDist = 0.0d;
        double sumR = 0.0d;
        double sumG = 0.0d;
        double sumB = 0.0d;
        for (Map.Entry<Color,Double> e : clrsDist.entrySet()) {
            sumR += e.getKey().getRed() * e.getValue();
            sumG += e.getKey().getGreen() * e.getValue();
            sumB += e.getKey().getBlue() * e.getValue();
            sumDist += e.getValue();
        }
        Color dominantColor = new Color((int) (sumR / sumDist),
                                        (int) (sumG / sumDist),
                                        (int) (sumB / sumDist));

        return new Result(dominantColor, allDistsMean, topDistsMean, skAll, skMost, kurtAll, kurtMost, nrToThreshold, colorDist.size());
    }





    private static int[] getRGBArr(int pixel) {
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        return new int[]{alpha, red, green, blue};
    }


    static class Result {

        public final Color color;
        public final double allMean;
        public final double topMean;
        public final int nrToThreshold;
        public final int nrColors;

        // Skewness:    http://en.wikipedia.org/wiki/Skewness
        //              http://de.wikipedia.org/wiki/Schiefe_(Statistik)
        public double skAll;
        public double skMost;

        // Kurtosis: http://en.wikipedia.org/wiki/Kurtosis
        //           http://de.wikipedia.org/wiki/W%C3%B6lbung_(Statistik)
        public double kurtAll;
        public double kurtMost;

        public Result(Color color, double allMean, double topMean, double skAll, double skMost, double kurtAll, double kurtMost, int nrToThreshold, int nrColors) {
            this.color = color;
            this.allMean = allMean;
            this.topMean = topMean;
            this.skAll = skAll;
            this.skMost = skMost;
            this.kurtAll = kurtAll;
            this.kurtMost = kurtMost;
            this.nrToThreshold = nrToThreshold;
            this.nrColors = nrColors;
        }

        public String getHexCode() {
            return String.format("%1$02x%2$02x%3$02x",
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue());
        }

    }


    static class ValueComparator implements Comparator {

        Map<Color, Double> base;

        public ValueComparator(Map<Color, Double> base) {
            this.base = base;
        }

        public int compare(Object a, Object b) {
            if (base.get(a) < base.get(b)) {
                return -1;
            } else if (base.get(a) == base.get(b)) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}
