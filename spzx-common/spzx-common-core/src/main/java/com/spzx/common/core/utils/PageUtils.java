package com.spzx.common.core.utils;

import com.github.pagehelper.PageHelper;
import com.spzx.common.core.utils.sql.SqlUtil;
import com.spzx.common.core.web.page.PageDomain;
import com.spzx.common.core.web.page.TableSupport;

/**
 * 分页工具类
 *
 * @author spzx
 */
public class PageUtils extends PageHelper {
    /**
     * 设置请求分页数据
     */
    public static void startPage()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
        Boolean reasonable = pageDomain.getReasonable();
        //用ThreadLocal将数据绑定到线程上。给持久化层的分页组件使用。
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(reasonable);
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage()
    {
        PageHelper.clearPage();
    }
}
