package com.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PdfGenerator {
    public static Path generatePdf(List<Path> imagePaths, String outputPath, 
                                 String layout, int borderSize, Color borderColor) throws IOException {
        try (PDDocument document = new PDDocument()) {
            boolean doublePage = "double".equalsIgnoreCase(layout);
            
            // Process images in pairs if double page layout
            for (int i = 0; i < imagePaths.size(); i += doublePage ? 2 : 1) {
                Path imagePath1 = imagePaths.get(i);
                Path imagePath2 = (doublePage && i + 1 < imagePaths.size()) ? imagePaths.get(i + 1) : null;
                
                PDPage page = createPageForImages(doublePage, imagePath1, imagePath2);
                document.addPage(page);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    if (borderSize > 0) {
                        drawBorder(contentStream, page, borderSize, borderColor);
                    }
                    
                    if (imagePath2 != null) {
                        // Double page layout - place two images side by side
                        addImageToPage(document, contentStream, imagePath1, 
                                    0, 0, page.getMediaBox().getWidth() / 2, page.getMediaBox().getHeight(), 
                                    borderSize);
                        addImageToPage(document, contentStream, imagePath2, 
                                    page.getMediaBox().getWidth() / 2, 0, 
                                    page.getMediaBox().getWidth() / 2, page.getMediaBox().getHeight(), 
                                    borderSize);
                    } else {
                        // Single page layout - place one image centered
                        addImageToPage(document, contentStream, imagePath1, 
                                    0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight(), 
                                    borderSize);
                    }
                }
            }
            
            Path pdfPath = Path.of(outputPath);
            document.save(pdfPath.toFile());
            return pdfPath;
        }
    }
    
    private static PDPage createPageForImages(boolean doublePage, Path imagePath1, Path imagePath2) throws IOException {
        if (doublePage && imagePath2 != null) {
            // Create a wider page for double spread
            return new PDPage(new PDRectangle(PDRectangle.A4.getWidth() * 2, PDRectangle.A4.getHeight()));
        } else {
            // Standard A4 page for single image
            return new PDPage(PDRectangle.A4);
        }
    }
    
    private static void addImageToPage(PDDocument document, PDPageContentStream contentStream, 
                                     Path imagePath, float x, float y, float width, float height, 
                                     int borderSize) throws IOException {
        PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath.toString(), document);
        
        // Calculate aspect ratio preserving dimensions
        float imageWidth = pdImage.getWidth();
        float imageHeight = pdImage.getHeight();
        float ratio = Math.min((width - 2 * borderSize) / imageWidth, 
                             (height - 2 * borderSize) / imageHeight);
        
        float scaledWidth = imageWidth * ratio;
        float scaledHeight = imageHeight * ratio;
        
        // Center the image in its allocated space
        float xPos = x + (width - scaledWidth) / 2;
        float yPos = y + (height - scaledHeight) / 2;
        
        contentStream.drawImage(pdImage, xPos, yPos, scaledWidth, scaledHeight);
    }
    
    private static void drawBorder(PDPageContentStream contentStream, PDPage page, 
                                 int borderSize, Color borderColor) throws IOException {
        contentStream.setStrokingColor(borderColor);
        contentStream.setLineWidth(borderSize);
        
        PDRectangle mediaBox = page.getMediaBox();
        float margin = borderSize / 2f; // Half the border size to account for line width
        
        contentStream.addRect(
            margin, 
            margin, 
            mediaBox.getWidth() - 2 * margin, 
            mediaBox.getHeight() - 2 * margin
        );
        contentStream.stroke();
    }
    
    public static Color getColorFromString(String colorName) {
        switch (colorName.toLowerCase()) {
            case "red":
                return Color.RED;
            case "blue":
                return Color.BLUE;
            case "green":
                return Color.GREEN;
            case "gray":
                return Color.GRAY;
            default:
                return Color.BLACK;
        }
    }
}