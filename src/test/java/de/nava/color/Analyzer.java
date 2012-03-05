package de.nava.color;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

/**
 * @author Niko Schmuck
 */
public class Analyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Analyzer.class);

    private static final String IMAGE_DIR = "src/test/resources/sample-images";


    @Test
    public void analyzeImages() throws Exception {
        String reportFilename = "image-matrix.html";

        // ~~ prepare HTML output report
        Writer writer = new FileWriter(reportFilename);
        writer.append("<html><head><title>Image Color Analysis</title>\n")
                .append("<style type=\"text/css\">\n")
                .append("td.square { padding-right: 12px; } \n")
                .append("tr > td { font-size:9px; padding-bottom: 12px; } \n")
                .append(".square div { border: 1px gray solid;} \n")
                .append("</style></head>\n")
                .append("<body><table>\n");

        String templateStr = "<td>" +
                             "<div class=\"colorImg\"><img src=\"URL\" title=\"TITLE\" /><div class=\"square\" style=\"background-color: #HEXCODE\">&nbsp; &nbsp; &nbsp;</div></div>" +
                             "<div class=\"colorInfo\">INFO</div>" +
                             "</td>";

        // ~~ process every image
        LOGGER.info("Going to process images in {}", IMAGE_DIR);

        final List<String> interestingImages = Arrays.asList("0046.jpg", "0041.jpg", "0241.jpg", "0248.jpg", "0271.jpg",
                "0443.jpg", "0617.jpg", "0622.jpg", "0720.jpg", "0793.jpg", "0953.jpg", "0955.jpg");
        File[] imageFiles = new File(IMAGE_DIR).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return interestingImages.contains(name);
                //return name.toLowerCase().endsWith(".jpg");
            }
        });

        int nr = 1;
        for (File imageFile : imageFiles) {

            // Analyse colors
            BitmapDominantColor.Result dominantColor = BitmapDominantColor.analyseColors(imageFile);

            // Append color analyse result to HTML report
            String htmlFragment = templateStr.replaceAll("URL", imageFile.getAbsolutePath())
                                .replaceAll("TITLE", imageFile.getName())
                                .replaceAll("HEXCODE", dominantColor.getHexCode())
                                .replaceAll("INFO", String.format("%d / %d<br>T%.0f<br>A%.0f<br>%.2f %.2f<br>%.2f %.2f", dominantColor.nrToThreshold, dominantColor.nrColors,
                                        dominantColor.topMean ,
                                        dominantColor.allMean ,
                                        dominantColor.skAll, dominantColor.skMost,
                                        dominantColor.kurtAll, dominantColor.kurtMost));
            writer.append(htmlFragment);
            if (nr % 20 == 0) {
                writer.append("</tr>\n\n<tr>");
            }
            nr++;
        }

        // ~~ finish
        writer.append("</tr>\n\n</table></body></html>");
        writer.flush();
        writer.close();
        LOGGER.info("Saved HTML report to {}", reportFilename);
    }

}
