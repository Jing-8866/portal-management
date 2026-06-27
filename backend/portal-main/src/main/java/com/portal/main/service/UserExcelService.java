package com.portal.main.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.portal.common.model.*;
import com.portal.main.dto.ImportResult;
import com.portal.main.mapper.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.*;

@Service
public class UserExcelService {
    @Autowired private SysUserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;



    public void exportUsers(OutputStream os, java.util.List<Long> idList) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("用户列表");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = {"用户名", "真实姓名", "邮箱", "手机号", "状态"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 5000);
        }

        List<SysUser> users;
        if (idList != null && !idList.isEmpty()) {
            users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, idList).orderByAsc(SysUser::getId));
        } else {
            users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId));
        }
        int rowIdx = 1;
        for (SysUser user : users) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(user.getUsername() != null ? user.getUsername() : "");
            row.createCell(1).setCellValue(user.getRealName() != null ? user.getRealName() : "");
            row.createCell(2).setCellValue(user.getEmail() != null ? user.getEmail() : "");
            row.createCell(3).setCellValue(user.getPhone() != null ? user.getPhone() : "");
            row.createCell(4).setCellValue(user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用");
        }
        workbook.write(os);
        workbook.close();
    }

    public void generateTemplate(OutputStream os) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("用户导入模板");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = {"用户名(必填)", "真实姓名", "邮箱", "手机号", "状态(启用/禁用)"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 7000);
        }

        Row sample = sheet.createRow(1);
        sample.createCell(0).setCellValue("zhangsan");
        sample.createCell(1).setCellValue("张三");
        sample.createCell(2).setCellValue("zhangsan@example.com");
        sample.createCell(3).setCellValue("13800138000");
        sample.createCell(4).setCellValue("启用");

        workbook.write(os);
        workbook.close();
    }

    public ImportResult importUsers(InputStream is) throws IOException {
        ImportResult result = new ImportResult();
        Workbook workbook = new XSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        int lastRow = sheet.getLastRowNum();
        result.setTotalRows(lastRow);

        String defaultPwd = "123456";
        String encoded = passwordEncoder.encode(defaultPwd);

        for (int i = 1; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String username = getCellString(row, 0);
            if (!StringUtils.hasText(username)) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getErrors().add("第" + (i+1) + "行: 用户名为空");
                continue;
            }

            String realName = getCellString(row, 1);
            String email = getCellString(row, 2);
            String phone = getCellString(row, 3);
            String statusStr = getCellString(row, 4);
            Integer status = "禁用".equals(statusStr) ? 0 : 1;

            try {
                SysUser existUser = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
                if (existUser != null) {
                    LambdaUpdateWrapper<SysUser> w = new LambdaUpdateWrapper<>();
                    w.eq(SysUser::getId, existUser.getId());
                    if (StringUtils.hasText(realName)) w.set(SysUser::getRealName, realName);
                    if (StringUtils.hasText(email)) w.set(SysUser::getEmail, email);
                    if (StringUtils.hasText(phone)) w.set(SysUser::getPhone, phone);
                    w.set(SysUser::getStatus, status);
                    userMapper.update(null, w);

                    result.setUpdatedCount(result.getUpdatedCount() + 1);
                } else {
                    SysUser newUser = new SysUser();
                    newUser.setUsername(username);
                    newUser.setPassword(encoded);
                    newUser.setRealName(realName);
                    newUser.setEmail(email);
                    newUser.setPhone(phone);
                    newUser.setStatus(status);
                    userMapper.insert(newUser);
                    result.setCreatedCount(result.getCreatedCount() + 1);
                }
            } catch (Exception e) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getErrors().add("第" + (i+1) + "行(" + username + "): " + e.getMessage());
            }
        }
        workbook.close();
        return result;
    }

    private String getCellString(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long)cell.getNumericCellValue());
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}
