package com.spzx.product.controller;

import com.spzx.common.core.domain.R;
import com.spzx.common.core.web.controller.BaseController;
import com.spzx.common.core.web.domain.AjaxResult;
import com.spzx.common.core.web.page.TableDataInfo;
import com.spzx.common.log.annotation.Log;
import com.spzx.common.log.enums.BusinessType;
import com.spzx.common.security.annotation.InnerAuth;
import com.spzx.common.security.annotation.Logical;
import com.spzx.common.security.annotation.RequiresPermissions;
import com.spzx.common.security.utils.SecurityUtils;
import com.spzx.product.api.domain.Brand;
import com.spzx.product.service.IBrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;

//import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * http://localhost:9205/brand/list
 */
@Slf4j
@Tag(name="品牌管理")
@RequestMapping("/brand")
@RestController
public class BrandController extends BaseController {


    // 依赖注入方式： 构造注入  setter注入  字段注入
    //Field injection is not recommended  字段注入不推荐的。
    @Autowired
    //@Resource
    private IBrandService brandService;

    /**
     * 查询品牌列表
     * @param brand
     * @return
     */
    @Operation(summary = "查询品牌列表")
    @GetMapping("list")
    @RequiresPermissions(value = {"product:brand:list","product:brand:query"},logical = Logical.OR)
    public TableDataInfo list(Brand brand){
        //com.github.pagehelper.Page extends ArrayList
        startPage(); //Page<E> page = startPage(pageNum, pageSize);

        List<Brand> brandList =  brandService.list(brand);

        return getDataTable(brandList);
    }

    @Operation(summary = "查询品牌详情")
    @GetMapping("/{id}")
    @RequiresPermissions(value = {"product:brand:query"})
    public AjaxResult getById(@PathVariable Long id){
        Brand brand = brandService.getById(id);
        return success(brand);
    }

    @Operation(summary = "添加品牌")
    @PostMapping
    @RequiresPermissions(value = {"product:brand:add"})
    @Log(title = "品牌管理", businessType = BusinessType.INSERT)
    public AjaxResult save(@RequestBody @Validated Brand brand){ //io.swagger.v3.oas.annotations.parameters.RequestBody; 错误的导包，导致数据封装不进来
        brand.setCreateBy(SecurityUtils.getUsername()); //从线程上获取绑定的数据。（请求流程中，经过网关过滤器，经过SpringMVC拦截器,它们往线程上绑定的数据。）
        int rows = brandService.save(brand); //sql语句对数据起作用行数
        return success(rows);
    }

    @Operation(summary = "修改品牌")
    @PutMapping
    @RequiresPermissions(value = {"product:brand:edit"})
    @Log(title = "品牌管理", businessType = BusinessType.UPDATE)
    public AjaxResult update(@RequestBody @Validated Brand brand){
        brand.setUpdateBy(SecurityUtils.getUsername()); //从线程上获取绑定的数据。（请求流程中，经过网关过滤器，经过SpringMVC拦截器,它们往线程上绑定的数据。）
        int rows = brandService.update(brand); //sql语句对数据起作用行数
        return toAjax(rows);
    }

    @Operation(summary = "删除品牌")
    @DeleteMapping("/{ids}")
    @RequiresPermissions(value = {"product:brand:remove"})
    @Log(title = "品牌管理", businessType = BusinessType.DELETE)
    public AjaxResult deleteBatch(@PathVariable("ids") Long[] ids){ //@PathVariable("ids") List<Long> ids
        int rows = brandService.deleteBatch(ids);
        return toAjax(rows);
    }

    @RequiresPermissions(value = {"product:brand:query"})
    @Operation(summary = "获取全部品牌")
    @GetMapping("getBrandAll")
    public AjaxResult getBrandAll() {
        return success(brandService.selectBrandAll());
    }


    //=====给前台系统使用接口================================================
    @InnerAuth
    @Operation(summary = "获取全部品牌")
    @GetMapping("getBrandAllList")
    public R<List<Brand>> getBrandAllList() {
        return R.ok(brandService.selectBrandAll());
    }

}
