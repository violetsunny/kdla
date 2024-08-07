/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.kdla.framework.supplement.excel.exp;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.util.FileUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.MapUtils;
import top.kdla.framework.common.help.MultiThreadInvokeHelp;
import top.kdla.framework.supplement.excel.BaseExcel;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
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
public class KdlaExcelWriteHelp<T extends BaseExcel> {

    public static <T> void writeWeb(HttpServletResponse response, Collection<T> list, String fileName) throws IOException {
        if (list == null || list.isEmpty()) {
            return;
        }
        T t = list.stream().findFirst().get();
        //设置Header并且输出文件
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 这里URLEncoder.encode可以防止中文乱码
        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-disposition");
        ServletOutputStream out = response.getOutputStream();
        EasyExcel.write(out, t.getClass()).sheet("sheet1").doWrite(list);
    }

    public static <T> void writeFile(File file, Collection<T> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        T t = list.stream().findFirst().get();
        EasyExcel.write(file, t.getClass()).sheet("sheet1").doWrite(list);
    }

    public static <T> void writeSheet(File file, Map<Class<T>, Collection<T>> lists) {
        ExcelWriter writer = EasyExcel.write(file).build();
        for (Map.Entry<Class<T>, Collection<T>> classMap : lists.entrySet()) {
            WriteSheet sheet = EasyExcel.writerSheet("sheet1").head(classMap.getKey()).build();
            writer.write(classMap.getValue(), sheet);
        }
    }

    public static <T> File writeDataFile(Collection<T> list, String fileUrl) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        //fileUrl文件全路径
        File file = new File(fileUrl);
        //填充导excel 一个请求对应多个excel,一个excel可以有多个sheet
        writeFile(file, list);
        return file;
    }

    public static <T> File getExcelFile(Supplier<Collection<T>> supplier, String fileUrl) {
        //获取数据
        Collection<T> tList = supplier.get();
        //转成文件
        return writeDataFile(tList, fileUrl);
    }

    public static <T> byte[] writeDataByte(Collection<T> list, String fileUrl) throws IOException {
        File file = writeDataFile(list, fileUrl);
        //转成byte
        return FileUtils.readFileToByteArray(file);
    }

    public static <T> byte[] getExcelByte(Supplier<Collection<T>> supplier, String fileUrl) throws IOException {
        //获取数据
        Collection<T> tList = supplier.get();
        //转成byte
        return writeDataByte(tList, fileUrl);
    }

    public static <T> Map<String, File> multiGetExcelFile(Map<String, Supplier<Collection<T>>> supplierMap, Executor executor) throws Exception {
        //将每个key对应的请求转换成每个key对应生成的excel文件
        Map<String, File> fileMap = Maps.newHashMap();
        //先将supplierMap转换成k,v的suppliers
        List<Supplier<Map<String, Collection<T>>>> suppliers = Lists.newArrayList();
        supplierMap.forEach((k, v) -> {
            Supplier<Map<String, Collection<T>>> supplier = () -> {
                //这样map就只有一个数据，每个key对应一个请求
                Map<String, Collection<T>> map = Maps.newHashMap();
                map.put(k, v.get());
                return map;
            };
            suppliers.add(supplier);
        });
        //CompletableFuture异步执行，同步等待suppliers结果数据
        List<Map<String, Collection<T>>> resultList = MultiThreadInvokeHelp.invokeGetS(suppliers, executor);
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
