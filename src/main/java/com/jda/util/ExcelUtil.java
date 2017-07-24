package com.jda.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Admin on 7/23/2017.
 */
public class ExcelUtil {

    private void writeMemberData(Row row, int colNum, MemberData memData) {
        Cell cell = row.createCell(colNum++);
        cell.setCellValue(memData.nickName);
        cell = row.createCell(colNum++);
        cell.setCellValue(memData.timeStamp);
        cell = row.createCell(colNum);
        cell.setCellValue(memData.content);
    }

    public void writeToExcel(Map<String, MemberData> uniqueUsersMap, File file,
                             String date, String channelName) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Data for " + date + " in " + channelName);

        // Add title to each column.
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Account name");
        cell = row.createCell(1);
        cell.setCellValue("Nick name");
        cell = row.createCell(2);
        cell.setCellValue("Most recent message time");
        cell = row.createCell(3);
        cell.setCellValue("Message");

        // Iterate through map.
        int rowNum = 1;
        for (Map.Entry entry : uniqueUsersMap.entrySet()) {
            int colNum = 0;
            row = sheet.createRow(rowNum++);
            cell = row.createCell(colNum++);
            cell.setCellValue((String) entry.getKey());
            // Iterate over the data of the member.
            MemberData memData = (MemberData) entry.getValue();
            writeMemberData(row, colNum, memData);
        }

        // Add total number of unique users.
        row = sheet.createRow(rowNum);
        cell = row.createCell(0);
        cell.setCellValue("Total number of users:");
        cell = row.createCell(1);
        cell.setCellValue(Integer.toString(uniqueUsersMap.size()));

        // Autosize the columns.
        for (int colIndex = 0; colIndex < 4; colIndex++) {
            sheet.autoSizeColumn(colIndex);
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            System.out.println("File cannot be written to excel. Error.");
            return;
        }
    }
}

