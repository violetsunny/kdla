/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.kdla.framework.excel.exp;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.util.FileUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.MapUtils;
import top.kdla.framework.common.help.MultiThreadInvokeHelp;
import top.kdla.framework.excel.BaseExcel;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * 写入excel
 *
 * @author kanglele
 * @version $Id: KdlaExcelWrite, v 0.1 2022/5/12 19:24 kanglele Exp $
 */
public class KdlaExcelWrite<T extends BaseExcel> {

    public KdlaExcelWrite() {
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

    public File writeDataFile(List<T> list, String fileUrl) {
        if (list == null || list.size() == 0) {
            return null;
        }
        //fileUrl文件全路径
        File file = new File(fileUrl);
        //填充导excel 一个请求对应多个excel,一个excel可以有多个sheet
        writeFile(file, list);
        return file;
    }

    public File getExcelFile(Supplier<List<T>> supplier, String fileUrl) {
        //获取数据
        List<T> tList = supplier.get();
        //转成文件
        return writeDataFile(tList, fileUrl);
    }

    public byte[] writeDataByte(List<T> list, String fileUrl) throws IOException {
        File file = writeDataFile(list, fileUrl);
        //转成byte
        return FileUtils.readFileToByteArray(file);
    }

    public byte[] getExcelByte(Supplier<List<T>> supplier, String fileUrl) throws IOException {
        //获取数据
        List<T> tList = supplier.get();
        //转成byte
        return writeDataByte(tList, fileUrl);
    }

    public Map<String, File> multiGetExcelFile(Map<String, Supplier<List<T>>> supplierMap, Executor executor) throws Exception {
        //将每个key对应的请求转换成每个key对应生成的excel文件
        Map<String, File> fileMap = Maps.newHashMap();
        //先将supplierMap转换成k,v的suppliers
        List<Supplier<Map<String, List<T>>>> suppliers = Lists.newArrayList();
        supplierMap.forEach((k, v) -> {
            Supplier<Map<String, List<T>>> supplier = () -> {
                //这样map就只有一个数据，每个key对应一个请求
                Map<String, List<T>> map = Maps.newHashMap();
                map.put(k, v.get());
                return map;
            };
            suppliers.add(supplier);
        });
        //CompletableFuture异步执行，同步等待suppliers结果数据
        List<Map<String, List<T>>> resultList = MultiThreadInvokeHelp.invokeGet(suppliers, executor);
        resultList.forEach(listMap -> {
            if (MapUtils.isEmpty(listMap)) {
                return;
            }
            //map应该只有一个数据，每个key对应返回结果数据
            listMap.forEach((k, v) -> {
                //转换成excel文件
                fileMap.put(k, writeDataFile(v, k));
            });
        });

        return fileMap;
    }
}
