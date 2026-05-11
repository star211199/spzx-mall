package com.spzx.product.controller;

import com.spzx.common.core.domain.R;
import com.spzx.common.core.web.controller.BaseController;
import com.spzx.common.core.web.domain.AjaxResult;
import com.spzx.common.security.annotation.InnerAuth;
import com.spzx.product.api.domain.vo.CategoryVo;
import com.spzx.product.domain.Category;
import com.spzx.product.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商品分类Controller
 */
@Tag(name = "商品分类接口管理")
@RestController
@RequestMapping("/category")
public class CategoryController extends BaseController {

    @Autowired
    private ICategoryService categoryService;

    @Operation(summary = "获取分类下拉树列表")
    @GetMapping(value = "/getTreeSelect/{id}")
    public AjaxResult treeSelect(@PathVariable Long id) {
        return success(categoryService.treeSelect(id));
    }

    @PostMapping("/export")
    public void export(HttpServletResponse response/*, Category category*/) throws Exception {
        categoryService.exportData(response);
    }


    @PostMapping("/import")
    public AjaxResult importData(MultipartFile file) throws Exception {
        try {
            categoryService.importData(file);
            return AjaxResult.success("导入成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return AjaxResult.error("导入失败");
    }

    //======以下接口：给前台系统使用================================================================

    //@InnerAuth(isUser = true) //具备内部接口权限才能调用。 即：必须携带请求头   from-source = "inner"  如果设置了isUser=true必须携带 user_id和username两个头信息
    @InnerAuth
    @GetMapping("getOneCategory")
    public R<List<CategoryVo>> getOneCategory(){
        List<CategoryVo> categoryVoList = categoryService.getOneCategory();
        return R.ok(categoryVoList);
    }

    /**
     * 查询三级分类
     * @return 组装好树结构分类数据
     */
    @InnerAuth
    @GetMapping(value = "/tree")
    public R<List<CategoryVo> > tree() {
        return R.ok(categoryService.tree());
    }
}