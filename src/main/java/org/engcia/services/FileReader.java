package org.engcia.services;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.*;

public class FileReader {

    public static List<Double> readXLSXByColumn(String filePath, String columnName){
        List<Double> columnList = new ArrayList<>();
        try {
            Workbook wb = null;           //initialize Workbook null


            FileInputStream fis = new FileInputStream(filePath);

            wb = new XSSFWorkbook(fis);


            Sheet sheet = wb.getSheetAt(0);   //getting the XSSFSheet object at given index
            Row header = sheet.getRow(0); //returns the logical row

            Iterator<Cell> headerIterator = header.cellIterator();
            int cellColumn = 0;
            while(headerIterator.hasNext()) {
                if (headerIterator.next().getStringCellValue().equals(columnName)) {
                    cellColumn = headerIterator.next().getColumnIndex() -1 ;
                    break;
                }
            }

            Iterator<Row> itr = sheet.iterator();    //iterating over excel file
            while (itr.hasNext()) {
                Row row = itr.next();
                if(!header.equals(row)) {
                    Iterator<Cell> cellIterator = row.cellIterator();   //iterating over each column
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        if(cell.getColumnIndex() == cellColumn) {
                            columnList.add(cell.getNumericCellValue());
                        }
                    }
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return columnList;
    }
    public static Map<String, List<Double>> readXLSX(String filePath){
        Map<String, List<Double>> columnList = new LinkedHashMap<>();
        try {
            Workbook wb = null;           //initialize Workbook null


            FileInputStream fis = new FileInputStream(filePath);

            wb = new XSSFWorkbook(fis);


            Sheet sheet = wb.getSheetAt(0);   //getting the XSSFSheet object at given index
            Row header = sheet.getRow(0); //returns the logical row

            //first column has no header
            columnList.put("",new ArrayList<>());
            Iterator<Cell> c1 = header.cellIterator();
            while(c1.hasNext()){
                columnList.put(c1.next().getStringCellValue(), new ArrayList<>());
            }

            Iterator<Row> itr = sheet.iterator();    //iterating over excel file
            while (itr.hasNext()) {
                Row row = itr.next();
                if(!header.equals(row)) {
                    Iterator<Cell> cellIterator = row.cellIterator();   //iterating over each column
                    int rowNumber =0;
                    while (cellIterator.hasNext()) {
                            Cell cell = cellIterator.next();
                            String[] listColumn = columnList.keySet().toArray(new String[0]);
                            columnList.get(listColumn[rowNumber]).add(cell.getNumericCellValue());
                            rowNumber++;
                    }
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return columnList;
    }
    public static List<String> headerFromFile(String filePath){
        List<String> list = new ArrayList<>();
        try {
            Workbook wb = null;           //initialize Workbook null


            FileInputStream fis = new FileInputStream(filePath);

            wb = new XSSFWorkbook(fis);


            Sheet sheet = wb.getSheetAt(0);   //getting the XSSFSheet object at given index
            Row header = sheet.getRow(0); //returns the logical row


            Iterator<Cell> c1 = header.cellIterator();
            while (c1.hasNext()) {
                list.add(c1.next().getStringCellValue());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

}

