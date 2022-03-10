package top.kdla.framework.infra.dal.mybatis.convertor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.kdla.framework.domain.shared.Page;

/**
 * 分页转换类
 *
 * @author haoxin
 * @date 2021-02-04
 **/
public class PageConvertor {

    public static Page toPage(IPage iPage) {
        Page page = new Page(iPage.getRecords(), iPage.getTotal(), iPage.getSize(), iPage.getCurrent());
        return page;
    }
}
