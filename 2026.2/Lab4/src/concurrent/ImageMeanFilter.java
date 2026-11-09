import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class provides functionality to apply a mean filter to an image.
 * The mean filter is used to smooth images by averaging the pixel values
 * in a neighborhood defined by a kernel size.
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * ImageMeanFilter.applyMeanFilter("input.jpg", "output.jpg", 3);
 * }
 * </pre>
 * 
 * <p>Supported image formats: JPG, PNG</p>
 * 
 * <p>Author: temmanuel@comptuacao.ufcg.edu.br</p>
 */
public class ImageMeanFilter {
    
    /**
     * Applies mean filter to an image
     * 
     * @param inputPath  Path to input image
     * @param outputPath Path to output image 
     * @param kernelSize Size of mean kernel
     * @throws IOException If there is an error reading/writing
     */
    public static void applyMeanFilter(String inputPath, String outputPath, int kernelSize, int totalThreads) throws IOException {
        // Load image
        BufferedImage originalImage = ImageIO.read(new File(inputPath));
        
        // Create result image
        BufferedImage filteredImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            BufferedImage.TYPE_INT_RGB
        );
        
        // Image processing
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        // Process each pixel

        Thread[] threads = new Thread[totalThreads];
        
        for (int i = 0; i < totalThreads; i++) {
            Work work = new Work(i, totalThreads, height, width, originalImage, kernelSize, filteredImage);
            Thread thread = new Thread(work);
            threads[i] = thread;
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (Exception e) {
                System.out.println("erro");
            }
        }

        // Save filtered image
        ImageIO.write(filteredImage, "jpg", new File(outputPath));
    }

    public static void process_line(int y, int width, BufferedImage originalImage, int kernelSize, BufferedImage filteredImage) {
        for (int x = 0; x < width; x++) {
                // Calculate neighborhood average
                int[] avgColor = calculateNeighborhoodAverage(originalImage, x, y, kernelSize);
                
                // Set filtered pixel
                filteredImage.setRGB(x, y, 
                    (avgColor[0] << 16) | 
                    (avgColor[1] << 8)  | 
                    avgColor[2]
                );
            }
    }
    
    /**
     * Calculates average colors in a pixel's neighborhood
     * 
     * @param image      Source image
     * @param centerX    X coordinate of center pixel
     * @param centerY    Y coordinate of center pixel
     * @param kernelSize Kernel size
     * @return Array with R, G, B averages
     */
    private static int[] calculateNeighborhoodAverage(BufferedImage image, int centerX, int centerY, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pad = kernelSize / 2;
        
        // Arrays for color sums
        long redSum = 0, greenSum = 0, blueSum = 0;
        int pixelCount = 0;
        
        // Process neighborhood
        for (int dy = -pad; dy <= pad; dy++) {
            for (int dx = -pad; dx <= pad; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                
                // Check image bounds
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    // Get pixel color
                    int rgb = image.getRGB(x, y);
                    
                    // Extract color components
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;
                    
                    // Sum colors
                    redSum += red;
                    greenSum += green;
                    blueSum += blue;
                    pixelCount++;
                }
            }
        }
        
        // Calculate average
        return new int[] {
            (int)(redSum / pixelCount),
            (int)(greenSum / pixelCount),
            (int)(blueSum / pixelCount)
        };
    }
    
    /**
     * Main method for demonstration
     * 
     * Usage: java ImageMeanFilter <input_file>
     * 
     * Arguments:
     *   input_file - Path to the input image file to be processed
     *                Supported formats: JPG, PNG
     * 
     * Example:
     *   java ImageMeanFilter input.jpg
     * 
     * The program will generate a filtered output image named "filtered_output.jpg"
     * using a 7x7 mean filter kernel
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ImageMeanFilter <input_file> <n_threads>");
            System.exit(1);
        }

        String inputFile = args[0];
        int totalThreads = Integer.parseInt(args[1]);

        if (totalThreads < 2) {
            System.err.println("n_threads must be at least 2");
            System.exit(1);
        }

        try {
            applyMeanFilter(inputFile, "filtered_output.jpg", 7, totalThreads);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        }
    }
}

class Work implements Runnable {
    
    int threadNumber;
    int totalThreads;
    int height;
    int width;
    BufferedImage originalImage;
    int kernelSize;
    BufferedImage filteredImage;

    public Work(int threadNumber, int totalThreads, int height, int width, BufferedImage originalImage, int kernelSize, BufferedImage filteredImage) {
        this.threadNumber = threadNumber;
        this.totalThreads = totalThreads;
        this.height = height;
        this.width = width;
        this.originalImage = originalImage;
        this.kernelSize = kernelSize;
        this.filteredImage = filteredImage;
    }

    @Override
    public void run() {
        for (int y = 0; y < height; y++) {
            if (y % totalThreads == threadNumber) {
                ImageMeanFilter.process_line(y, width, originalImage, kernelSize, filteredImage);
            }
        }
    }
}
