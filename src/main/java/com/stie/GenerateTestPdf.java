package com.stie;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;

public class GenerateTestPdf {
    public static void main(String[] args) {
        try {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.setLeading(22);
            contentStream.newLineAtOffset(50, 700);
            
            // Name
            contentStream.showText("Raju Bani");
            contentStream.newLine();
            
            // Contact
            contentStream.setFont(PDType1Font.HELVETICA, 11);
            contentStream.setLeading(16);
            contentStream.showText("Email: vxs3@ecommerce.com");
            contentStream.newLine();
            contentStream.showText("Phone: +65 8123 4567");
            contentStream.newLine();
            contentStream.showText("Nationality: Singaporean");
            contentStream.newLine();
            contentStream.showText("Security License: PLRD-123456");
            contentStream.newLine();
            contentStream.newLine();
            
            // Education
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.showText("Education");
            contentStream.newLine();
            contentStream.setFont(PDType1Font.HELVETICA, 11);
            contentStream.showText("Master of Computer Applications (MCA) - University of Kashmir (2018 - 2021)");
            contentStream.newLine();
            contentStream.newLine();
            
            // Experience
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.showText("Experience");
            contentStream.newLine();
            contentStream.setFont(PDType1Font.HELVETICA, 11);
            contentStream.showText("Senior Security Officer & Developer (2021 - Present)");
            contentStream.newLine();
            contentStream.newLine();
            
            // Skills
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.showText("Skills");
            contentStream.newLine();
            contentStream.setFont(PDType1Font.HELVETICA, 11);
            contentStream.showText("Java, Spring Boot, SQL, CCTV, Patrolling, Access Control, First Aid");
            contentStream.newLine();
            
            contentStream.endText();
            contentStream.close();

            File file = new File("C:\\Users\\Asus\\OneDrive\\Desktop\\Raju_Bani_Resume.pdf");
            document.save(file);
            document.close();
            System.out.println("Test resume PDF generated successfully at: " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

