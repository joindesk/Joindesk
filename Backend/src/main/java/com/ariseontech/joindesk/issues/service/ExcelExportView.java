package com.ariseontech.joindesk.issues.service;

import com.ariseontech.joindesk.issues.domain.Issue;
import com.ariseontech.joindesk.issues.domain.Label;
import com.ariseontech.joindesk.issues.domain.Version;
import com.ariseontech.joindesk.project.domain.Component;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

class ExcelExportView {
    ByteArrayInputStream buildExcelDocument(List<Issue> issues, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setHeader("Content-Disposition", "attachment;filename=\"export" + LocalDateTime.now().toString() + ".xls\"");
        workbook = buildExcel(workbook, issues);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    ByteArrayInputStream buildCSVDocument(List<Issue> issues, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setHeader("Content-Disposition", "attachment;filename=\"export" + LocalDateTime.now().toString() + ".csv\"");
        workbook = buildExcel(workbook, issues);
        Sheet selSheet = workbook.getSheetAt(0);
        // Iterate through all the rows in the selected sheet
        Iterator rowIterator = selSheet.iterator();
        StringBuilder csv = new StringBuilder();
        while (rowIterator.hasNext()) {

            Row row = (Row) rowIterator.next();

            // Iterate through all the columns in the row and build ","
            // separated string
            Iterator cellIterator = row.cellIterator();
            StringBuilder sb = new StringBuilder();
            while (cellIterator.hasNext()) {
                Cell cell = (Cell) cellIterator.next();
                if (sb.length() != 0) {
                    sb.append(",");
                }

                // If you are using poi 4.0 or over, change it to
                // cell.getCellType
                switch (cell.getCellTypeEnum()) {
                    case STRING:
                        sb.append(cell.getStringCellValue());
                        break;
                    case NUMERIC:
                        sb.append(cell.getNumericCellValue());
                        break;
                    case BOOLEAN:
                        sb.append(cell.getBooleanCellValue());
                        break;
                    default:
                }
            }
            if (csv.toString().isEmpty()) csv.append(sb.toString());
            else csv.append('\n').append(sb.toString());
        }
        workbook.close();
        return new ByteArrayInputStream(csv.toString().getBytes());
    }

    private Workbook buildExcel(Workbook workbook, List<Issue> issues) {
        Sheet sheet = workbook.createSheet("Issues");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Issue");
        header.createCell(1).setCellValue("Summary");
        header.createCell(2).setCellValue("Description");
        header.createCell(3).setCellValue("Created");
        header.createCell(4).setCellValue("Updated");
        header.createCell(5).setCellValue("Due");
        header.createCell(6).setCellValue("Project");
        header.createCell(7).setCellValue("Priority");
        header.createCell(8).setCellValue("Assignee");
        header.createCell(9).setCellValue("Reporter");
        header.createCell(10).setCellValue("Current Step");
        header.createCell(11).setCellValue("Type");
        header.createCell(12).setCellValue("Resolution");
        header.createCell(13).setCellValue("Labels");
        header.createCell(14).setCellValue("Versions");
        header.createCell(15).setCellValue("Components");
        header.createCell(16).setCellValue("Start Date");
        header.createCell(17).setCellValue("End Date");
        int rowNum = 1;
        for (Issue issue : issues) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(issue.getKey());
            row.createCell(1).setCellValue(issue.getSummary());
            row.createCell(2).setCellValue(issue.getDescription());
            row.createCell(3).setCellValue(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(issue.getCreated()));
            String updatedDate = null != issue.getUpdated() ? new SimpleDateFormat("yyyy/MM/dd HH:mm").format(issue.getUpdated()) : "";
            row.createCell(4).setCellValue(updatedDate);
            String dueDate = null != issue.getDueDate() ? issue.getDueDate().toString() : "";
            row.createCell(5).setCellValue(dueDate);
            row.createCell(6).setCellValue(issue.getProject().getKey());
            row.createCell(7).setCellValue(issue.getPriority().toString());
            String assignee = null != issue.getAssignee() ? issue.getAssignee().getUserName() : "";
            row.createCell(8).setCellValue(assignee);
            row.createCell(9).setCellValue(issue.getReporter().getUserName());
            row.createCell(10).setCellValue(issue.getCurrentStep().getIssueStatus().getName());
            row.createCell(11).setCellValue(issue.getIssueType().getName());
            String resolution = null != issue.getResolution() ? issue.getResolution().getName() : "";
            row.createCell(12).setCellValue(resolution);
            row.createCell(13).setCellValue(null != issue.getLabels() ? StringEscapeUtils.escapeCsv(StringUtils.collectionToCommaDelimitedString(issue.getLabels().stream().map(Label::getName).collect(Collectors.toList()))) : "");
            row.createCell(14).setCellValue(null != issue.getVersions() ? StringEscapeUtils.escapeCsv(StringUtils.collectionToCommaDelimitedString(issue.getVersions().stream().map(Version::getName).collect(Collectors.toList()))) : "");
            row.createCell(15).setCellValue(null != issue.getComponents() ? StringEscapeUtils.escapeCsv(StringUtils.collectionToCommaDelimitedString(issue.getComponents().stream().map(Component::getName).collect(Collectors.toList()))) : "");
            row.createCell(16).setCellValue(null != issue.getStartDate() ? issue.getStartDate().toString() : "");
            row.createCell(17).setCellValue(null != issue.getEndDate() ? issue.getEndDate().toString() : "");
        }
        return workbook;
    }

}
