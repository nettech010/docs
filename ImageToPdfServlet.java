package com.example;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

@WebServlet("/convert")
public class ImageToPdfServlet extends HttpServlet {
    private static final String UPLOAD_DIR = "uploads";
    private static final String OUTPUT_DIR = "generated_pdfs";
    
    @Override
    public void init() throws ServletException {
        // Create directories if they don't exist
        new File(getServletContext().getRealPath(UPLOAD_DIR)).mkdirs();
        new File(getServletContext().getRealPath(OUTPUT_DIR)).mkdirs();
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Check if request is multipart
        if (!ServletFileUpload.isMultipartContent(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request must be multipart");
            return;
        }
        
        try {
            // Configure upload settings
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(1024 * 1024); // 1MB memory threshold
            
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setSizeMax(100 * 1024 * 1024); // 100MB max request size
            
            // Parse the request
            List<FileItem> items = upload.parseRequest(request);
            List<Path> uploadedImages = new ArrayList<>();
            String layout = "single";
            int borderSize = 3;
            String borderColor = "black";
            
            // Process each item
            for (FileItem item : items) {
                if (item.isFormField()) {
                    // Process regular form field
                    String fieldName = item.getFieldName();
                    String fieldValue = item.getString();
                    
                    switch (fieldName) {
                        case "layout":
                            layout = fieldValue;
                            break;
                        case "borderSize":
                            borderSize = Integer.parseInt(fieldValue);
                            break;
                        case "borderColor":
                            borderColor = fieldValue;
                            break;
                    }
                } else {
                    // Process uploaded file
                    if (!item.getName().isEmpty()) {
                        String fileName = UUID.randomUUID().toString() + 
                                item.getName().substring(item.getName().lastIndexOf('.'));
                        Path uploadPath = Paths.get(getServletContext().getRealPath(UPLOAD_DIR), fileName);
                        item.write(uploadPath.toFile());
                        uploadedImages.add(uploadPath);
                    }
                }
            }
            
            // Generate PDF
            if (!uploadedImages.isEmpty()) {
                String outputFileName = UUID.randomUUID().toString() + ".pdf";
                Path outputPath = Paths.get(getServletContext().getRealPath(OUTPUT_DIR), outputFileName);
                
                Color color = PdfGenerator.getColorFromString(borderColor);
                PdfGenerator.generatePdf(uploadedImages, outputPath.toString(), 
                                        layout, borderSize, color);
                
                // Clean up uploaded images
                for (Path imagePath : uploadedImages) {
                    Files.deleteIfExists(imagePath);
                }
                
                // Redirect to result page with download link
                response.sendRedirect("result.html?file=" + outputFileName);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No images uploaded");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing request");
        }
    }
}