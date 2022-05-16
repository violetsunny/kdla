/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.kdla.framework.excel.exp;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.apache.commons.io.FileUtils;
import top.kdla.framework.excel.BaseExcel;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 写入excel
 *
 * @author kanglele
 * @version $Id: KdlaExcelWrite, v 0.1 2022/5/12 19:24 kanglele Exp $
 */
public class KdlaExcelWrite<T extends BaseExcel> {

    private KdlaExcelWrite() {
    }

    public void writeWeb(HttpServletResponse response, List<T> list, String fileName) throws IOException {
        if (list == null || list.size() == 0) {
            return;
        }
        T t = list.get(0);
        //设置Header并且输出文件
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码
        fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-disposition");
        ServletOutputStream out = response.getOutputStream();
        EasyExcel.write(out, t.getClass()).sheet("sheet1").doWrite(list);
    }

    public void writeFile(File file, List<T> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        T t = list.get(0);
        EasyExcel.write(file, t.getClass()).sheet("sheet1").doWrite(list);
    }

    public <E> void writeSheet(File file, Map<Class<E>, List<E>> lists) {
        ExcelWriter writer = EasyExcel.write(file).build();
        for (Map.Entry<Class<E>, List<E>> classMap : lists.entrySet()) {
            WriteSheet sheet = EasyExcel.writerSheet("sheet1").head(classMap.getKey()).build();
            writer.write(classMap.getValue(), sheet);
        }
    }

    public byte[] getExcelByte(Supplier<T> supplier, String fileUrl) throws IOException {
        //分页获取数据
        List<T> tList = (List<T>) supplier.get();
        if (tList == null || tList.size() == 0) {
            return null;
        }
        //fileUrl文件全路径
        File file = new File(fileUrl);
        //填充导excel 一个请求对应多个excel,一个excel可以有多个sheet
        writeFile(file, tList);
        //上传oss或者直接导出页面
        byte[] bytes = FileUtils.readFileToByteArray(file);
        byte[] bytes2 = FileUtil.readBytes(file);
        byte[] bytes3 = com.alibaba.excel.util.FileUtils.readFileToByteArray(file);
        return bytes;
    }
}
