package com.stie.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    /**
     * Generates a PDF report from a JRXML template and a list of data maps.
     *
     * @param templateName The name of the JRXML file in src/main/resources/reports/ (e.g. "candidates_report.jrxml")
     * @param data         A list of maps where keys match the field names in the JRXML.
     * @return A byte array containing the compiled PDF.
     */
    public byte[] generatePdfReport(String templateName, List<Map<String, Object>> data) throws Exception {
        // Load the JRXML template from the classpath
        InputStream reportStream = new ClassPathResource("reports/" + templateName).getInputStream();

        // Compile the Jasper report from .jrxml to .jasper
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // Convert the List of Maps into a Jasper-compatible DataSource
        JRDataSource dataSource = new JRMapCollectionDataSource((List) data);

        // Fill the report with data
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        // Export the report to PDF
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    /**
     * Generates an Excel (.xlsx) report from a JRXML template and a list of data maps.
     *
     * @param templateName The name of the JRXML file in src/main/resources/reports/
     * @param data         A list of maps where keys match the field names in the JRXML.
     * @return A byte array containing the compiled Excel file.
     */
    public byte[] generateExcelReport(String templateName, List<Map<String, Object>> data) throws Exception {
        InputStream reportStream = new ClassPathResource("reports/" + templateName).getInputStream();
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
        JRDataSource dataSource = new JRMapCollectionDataSource((List) data);
        
        Map<String, Object> parameters = new HashMap<>();
        // Ignore pagination for continuous Excel sheet
        parameters.put(JRParameter.IS_IGNORE_PAGINATION, true);
        
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter exporter = new net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter();
        
        exporter.setExporterInput(new net.sf.jasperreports.export.SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new net.sf.jasperreports.export.SimpleOutputStreamExporterOutput(out));
        
        net.sf.jasperreports.export.SimpleXlsxReportConfiguration config = new net.sf.jasperreports.export.SimpleXlsxReportConfiguration();
        config.setOnePagePerSheet(false);
        config.setRemoveEmptySpaceBetweenRows(true);
        config.setDetectCellType(true);
        config.setWhitePageBackground(false);
        exporter.setConfiguration(config);
        
        exporter.exportReport();
        
        return out.toByteArray();
    }
}
