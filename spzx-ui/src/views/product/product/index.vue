<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="商品名称" prop="name">
        <el-input
            v-model="queryParams.name"
            placeholder="请输入商品名称"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="品牌">
        <el-select
            v-model="queryParams.brandId"
            class="m-2"
            placeholder="选择品牌"
            size="small"
            style="width: 100%"
        >
          <el-option
              v-for="item in queryBrandList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="分类">
        <el-select
            v-model="queryParams.category1Id"
            @change="selectcategory1"
            class="m-2"
            placeholder="一级分类"
            size="small"
            style="width: 32%;margin-right: 5px;"
        >
          <el-option
              v-for="item in queryCategory1List"
              :key="item.id"
              :label="item.name"
              :value="item.id"
          />
        </el-select>
        <el-select
            v-model="queryParams.category2Id"
            @change="selectcategory2"
            class="m-2"
            placeholder="二级分类"
            size="small"
            style="width: 32%;margin-right: 5px;"
        >
          <el-option
              v-for="item in queryCategory2List"
              :key="item.id"
              :label="item.name"
              :value="item.id"
          />
        </el-select>
        <el-select
            v-model="queryParams.category3Id"
            class="m-2"
            placeholder="三级分类"
            size="small"
            style="width: 32%;"
        >
          <el-option
              v-for="item in queryCategory3List"
              :key="item.id"
              :label="item.name"
              :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="productList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column label="轮播图" #default="scope" width="200">
        <div style="height: 50px;float: left;">
          <img v-for="(item, index) in scope.row.sliderUrlList"
               :key="index"
               :src="item"
               width="50"
          />
        </div>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" width="160" />
      <el-table-column prop="brandName" label="品牌" />
      <el-table-column prop="category1Name" label="一级分类" />
      <el-table-column prop="category2Name" label="二级分类" />
      <el-table-column prop="category3Name" label="三级分类" />
      <el-table-column prop="unitName" label="计量单位" />
      <el-table-column prop="status" label="状态" #default="scope">
        {{ scope.row.status == 0 ? '未上架' : scope.row.status == 1 ? '上架' : '下架' }}
      </el-table-column>
      <el-table-column prop="auditStatus" label="审核状态" #default="scope">
        {{ scope.row.auditStatus == 0 ? '未审核' : scope.row.auditStatus == 1 ? '通过' : '驳回' }}
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="250">
        <template #default="scope">
          <el-button v-if="scope.row.auditStatus == 0" link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button v-if="scope.row.auditStatus == 0" link type="primary" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          <el-button v-if="scope.row.auditStatus == 0" link type="success"  @click="handleAudit(scope.row.id, 1)">通过</el-button>
          <el-button v-if="scope.row.auditStatus == 0" link type="danger"  @click="handleAudit(scope.row.id, -1)">驳回</el-button>
          <el-button v-if="scope.row.auditStatus == 1 && (scope.row.status == 0 || scope.row.status == -1)" link type="primary" @click="handleUpdateStatus(scope.row.id, 1)">上架</el-button>
          <el-button v-if="scope.row.auditStatus == 1 && scope.row.status == 1" link type="primary" plain @click="handleUpdateStatus(scope.row.id, -1)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

    <!-- 添加或修改商品对话框 -->
    <el-dialog :title="title" v-model="open" width="60%" append-to-body>
      <el-steps :active="activeIndex" finish-status="success">
        <el-step title="Step 1" description="商品基本信息"></el-step>
        <el-step title="Step 2" description="商品SKU信息"></el-step>
        <el-step title="Step 3" description="商品详情信息"></el-step>
      </el-steps>
      <el-form ref="productRef" :model="form" :rules="rules" label-width="80px" style="margin-top: 20px;">
        <el-divider />
        <div v-if="activeIndex == 0">
          <el-form-item label="商品名称" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="分类" prop="category3Id">
            <el-cascader
                :props="categoryProps"
                v-model="categoryIdList"
                @change="handleCategoryChange"
            />
          </el-form-item>
          <el-form-item label="品牌" prop="brandId">
            <el-select v-model="form.brandId" class="m-2" placeholder="选择品牌">
              <el-option
                  v-for="item in categoryBrandList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="计量单位" prop="unitName">
            <el-select
                v-model="form.unitName"
                class="m-2"
                placeholder="计量单位"
            >
              <el-option
                  v-for="item in unitList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.name"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="轮播图" prop="sliderUrlList">
            <el-upload
                v-model:file-list="sliderTempUrlList"
                :action="imgUpload.url"
                :headers="imgUpload.headers"
                list-type="picture-card"
                multiple
                :on-success="handleSliderSuccess"
                :on-remove="handleRemove"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
        </div>
        <div v-if="activeIndex == 1">
          <el-form-item label="选择规格">
            <el-select
                :disabled="form.id != null"
                v-model="form.specValue"
                class="m-2"
                placeholder="选择规格"
                size="default"
                @change="handleSpecValueChange"
            >
              <el-option
                  v-for="item in specList"
                  :key="item.specValue"
                  :label="item.specName"
                  :value="item.specValue"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="商品SKU">
            <el-table :data="form.productSkuList" border style="width: 100%">
              <el-table-column prop="skuSpec" label="规格" width="180" />
              <el-table-column label="图片" #default="scope" width="80">
                <el-upload
                    class="avatar-uploader"
                    :action="imgUpload.url"
                    :headers="imgUpload.headers"
                    :show-file-list="false"
                    :on-success="(response, uploadFile, fileList) =>handleSkuSuccess(response, uploadFile, fileList, scope.row)">
                  <img
                      v-if="scope.row.thumbImg"
                      :src="scope.row.thumbImg"
                      class="avatar"
                  />
                  <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
                </el-upload>
              </el-table-column>
              <el-table-column label="售价" #default="scope">
                <el-input v-model="scope.row.salePrice" />
              </el-table-column>
              <el-table-column label="市场价" #default="scope">
                <el-input v-model="scope.row.marketPrice" />
              </el-table-column>
              <el-table-column label="成本价" #default="scope">
                <el-input v-model="scope.row.costPrice" />
              </el-table-column>
              <el-table-column label="库存数" #default="scope">
                <el-input v-model="scope.row.stockNum" />
              </el-table-column>
              <el-table-column label="重量" #default="scope">
                <el-input v-model="scope.row.weight" />
              </el-table-column>
              <el-table-column label="体积" #default="scope">
                <el-input v-model="scope.row.volume" />
              </el-table-column>
            </el-table>
          </el-form-item>
        </div>
        <div v-if="activeIndex == 2">
          <el-form-item label="详情图片">
            <el-upload
                v-model:file-list="detailsImageTempUrlList"
                :action="imgUpload.url"
                :headers="imgUpload.headers"
                list-type="picture-card"
                multiple
                :on-success="handleDetailsSuccess"
                :on-remove="handleDetailsRemove"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
        </div>
        <div v-if="activeIndex == 3">
          <div style="font-size: large;margin: 30px 30px;">提交成功</div>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="danger" @click="submitForm(-1)" :disabled="activeIndex == 0">上一步</el-button>
          <el-button type="primary" @click="submitForm(1)">{{ activeIndex < 2 ? '下一步' : '提交' }}</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Product">
import { listProduct, getProduct, delProduct, addProduct, updateProduct, updateStatus, updateAuditStatus } from "@/api/product/product";
import { getTreeSelect } from "@/api/product/category";
import { getCategoryBrandAll } from "@/api/product/categoryBrand";
import { getBrandAll } from "@/api/product/brand";
import { getUnitAll } from "@/api/base/productUnit";
import { getCategorySpecAll } from "@/api/product/productSpec";
import { getToken } from "@/utils/auth.js";

const { proxy } = getCurrentInstance();

const productList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    brandId: null,
    category1Id: null,
    category2Id: null,
    category3Id: null,
    status: null,
    auditStatus: null
  },
  rules: {
    name: [
      { required: true, message: "商品名称不能为空", trigger: "change" }
    ],
    category3Id: [
      { required: true, message: "商品分类不能为空", trigger: "change" }
    ],
    brandId: [
      { required: true, message: "品牌不能为空", trigger: "change" }
    ],
    unitName: [
      { required: true, message: "商品单位不能为空", trigger: "change" }
    ],
    sliderUrlList: [
      { required: true, message: "轮播图不能为空", trigger: "change" }
    ]
  },
  imgUpload: {
    // 设置上传的请求头部
    headers: {
      Authorization: "Bearer " + getToken()
    },
    // 图片上传的方法地址:
    url: import.meta.env.VITE_APP_BASE_API + "/file/upload"
  }
});

const { queryParams, form, rules, imgUpload } = toRefs(data);

getList();
/** 查询商品列表 */
function getList() {
  loading.value = true;
  listProduct(queryParams.value).then(response => {
    productList.value = response.rows;
    productList.value.forEach(item => {
      item.sliderUrlList = item.sliderUrls ? item.sliderUrls.split(',') : []
    })
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    name: null,
    brandId: null,
    category1Id: null,
    category2Id: null,
    category3Id: null,
    unitName: null,
    sliderUrls: null,
    specValue: null,
    status: null,
    auditStatus: null,
    auditMessage: null,
    sliderUrlList: [],
    productSkuList: [],
    detailsImageUrlList: []
  };
  proxy.resetForm("productRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryCategory2List.value = []
  queryCategory3List.value = []
  proxy.resetForm("queryRef");
  queryParams.value.category1Id = null
  queryParams.value.category2Id = null
  queryParams.value.category3Id = null
  queryParams.value.brandId = null
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

//分类搜索
const queryCategory1List = ref([])
const queryCategory2List = ref([])
const queryCategory3List = ref([])
const getTreeSelectList = async (parentId, level) => {
  const { code, data, message } = await getTreeSelect(parentId)
  if (level == 1) {
    queryCategory1List.value = data
  }
  if (level == 2) {
    queryCategory2List.value = data
  }
  if (level == 3) {
    queryCategory3List.value = data
  }
}
const selectcategory1 = () => {
  queryCategory2List.value = []
  queryCategory3List.value = []
  getTreeSelectList(queryParams.value.category1Id, 2)
}
const selectcategory2 = () => {
  queryCategory3List.value = []
  getTreeSelectList(queryParams.value.category2Id, 3)
}
getTreeSelectList(0, 1)
//品牌搜索
const queryBrandList = ref([])
const getBrandAllList = async () => {
  const { data } = await getBrandAll()
  queryBrandList.value = data
}
getBrandAllList()

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加商品";

  //清空
  activeIndex.value = 0
  sliderTempUrlList.value = []
  categoryIdList.value = []
  detailsImageTempUrlList.value = []
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getProduct(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改商品";

    activeIndex.value = 0
    sliderTempUrlList.value = []
    detailsImageTempUrlList.value = []

    //分类赋值
    categoryIdList.value = [
      form.value.category1Id,
      form.value.category2Id,
      form.value.category3Id,
    ]
    // //处理图片
    form.value.sliderUrlList = (form.value.sliderUrls || '').split(",").filter(url => url)
    form.value.sliderUrlList.forEach(url => {
      sliderTempUrlList.value.push({ url: url })
    })

    //处理详情图片
    ;(form.value.detailsImageUrlList || []).forEach(url => {
      detailsImageTempUrlList.value.push({ url: url })
    })

    //加载分类品牌
    getCategoryBrand()
    //加载分类规格
    getCategorySpec()
  });
}

/** 提交按钮 */
const activeIndex = ref(0)
// function handlePrev() {
//   if (activeIndex.value-- < 0) activeIndex.value = 0
//   console.log(activeIndex.value)
// }
// function handleNext() {
//   if (activeIndex.value++ > 3) activeIndex.value = 3
//   submitForm()
//   console.log(activeIndex.value)
// }
function submitForm(index) {
  proxy.$refs["productRef"].validate(valid => {
    if (valid) {
      activeIndex.value = activeIndex.value + index

      //轮播图临时地址重新赋值，否则新上传的图片不显示
      sliderTempUrlList.value = []
      form.value.sliderUrlList.forEach(url => {
        sliderTempUrlList.value.push({ url: url })
      })

      //详情图片临时地址重新赋值，否则新上传的图片不显示
      detailsImageTempUrlList.value = []
      form.value.detailsImageUrlList.forEach(url => {
        detailsImageTempUrlList.value.push({ url: url })
      })

      if (activeIndex.value == 3) {
        form.value.sliderUrls = form.value.sliderUrlList.join(',')
        if (form.value.id != null) {
          updateProduct(form.value).then(response => {
            proxy.$modal.msgSuccess("修改成功");
            open.value = false;
            getList();
          });
        } else {
          addProduct(form.value).then(response => {
            proxy.$modal.msgSuccess("新增成功");
            open.value = false;
            getList();
          });
        }
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除商品编号为"' + _ids + '"的数据项？').then(function() {
    return delProduct(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

//审核
function handleAudit(id, status) {
  const msg = status == 1 ? '是否确认审批通过商品编号为"' + id + '"的数据项？' : '是否确认审批驳回商品编号为"' + id + '"的数据项？'
  proxy.$modal.confirm(msg).then(function() {
    return updateAuditStatus(id, status);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("操作成功");
  }).catch(() => {});
}

//上下架
function handleUpdateStatus(id, status) {
  const msg = status == 1 ? '是否确认上架商品编号为"' + id + '"的数据项？' : '是否确认下架商品编号为"' + id + '"的数据项？'
  proxy.$modal.confirm(msg).then(function() {
    return updateStatus(id, status);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("操作成功");
  }).catch(() => {});
}

//三级分类数据加载
//加载分类品牌
const categoryIdList = ref([])
const categoryProps = ref([])
categoryProps.value = {
  lazy: true,
  value: 'id',
  label: 'name',
  leaf: 'leaf',
  async lazyLoad(node, resolve) {
    const { level } = node
    if (typeof node.value == 'undefined') node.value = 0
    const { data } = await getTreeSelect(
        node.value
    )
    //hasChildren判断是否有子节点
    data.forEach(function(item) {
      item.leaf = !item.hasChildren
    })
    resolve(data)
  },
}

//处理分类change事件
const handleCategoryChange = () => {
  if (categoryIdList.value.length == 3) {
    form.value.category1Id = categoryIdList.value[0]
    form.value.category2Id = categoryIdList.value[1]
    form.value.category3Id = categoryIdList.value[2]
    getCategoryBrand()
    getCategorySpec()
  }
}
//加载品牌
const categoryBrandList = ref([])
const getCategoryBrand = async () => {
  if (!form.value.category3Id) {
    proxy.$modal.msgError("请选择分类");
    return
  }
  const { data } = await getCategoryBrandAll(form.value.category3Id)
  categoryBrandList.value = data
}

//加载商品单位
const unitList = ref([])
const getUnitAllList = async () => {
  const { data } = await getUnitAll()
  unitList.value = data
}
getUnitAllList()

// 上传商品轮播图图片
const sliderTempUrlList = ref([])
const handleRemove = (uploadFile, uploadFiles) => {
  //新上传的文件 uploadFile.url 是 blob 预览地址，真实地址在上传响应里
  const url = (uploadFile.response && uploadFile.response.data) ? uploadFile.response.data.url : uploadFile.url
  const index = form.value.sliderUrlList.indexOf(url)
  if (index > -1) {
    form.value.sliderUrlList.splice(index, 1)
  }
}
const handleSliderSuccess = (response, uploadFile) => {
  if(response.code == 200) {
    form.value.sliderUrlList.push(response.data.url)
  } else {
    proxy.$modal.msgError(response.msg);
  }
}

//sku图片
const handleSkuSuccess = (response, uploadFile, fileList, row) => {
  row.thumbImg = response.data.url
}

//加载商品规格
const specList = ref([])
const getCategorySpec = async () => {
  if (!form.value.category3Id) {
    proxy.$modal.msgError("请选择分类");
    return
  }
  const { data } = await getCategorySpecAll(form.value.category3Id)
  specList.value = data
}

//规格变化，商品sku变化处理，取规格属性值 笛卡尔积生成sku
const handleSpecValueChange = val => {
  form.value.productSkuList = []
  let specValueList = JSON.parse(form.value.specValue)

  // let array = [
  //   ['红色', '黑色', '白色'],
  //   ['16G', '32G'],
  // ]
  let array = []
  specValueList.forEach(function(item) {
    let res = []
    item.valueList.map(x => {
      //res.push(item.key + ':' + x)
      res.push(x)
    })
    array.push(res)
  })
  //取数组的笛卡尔积
  let result = array.reduce(
      (a, b, c) => {
        let res = []
        a.map(x => {
          b.map(y => {
            res.push([...x, y])
          })
        })
        return res
      },
      [[]]
  )
  result.forEach(function(item) {
    form.value.productSkuList.push({
      skuSpec: item.join(' + '),
      skuSpecList: item,
      price: 0,
    })
  })
}

// 上传商品详情图片
const detailsImageTempUrlList = ref([])
const handleDetailsRemove = (uploadFile, uploadFiles) => {
  const url = (uploadFile.response && uploadFile.response.data) ? uploadFile.response.data.url : uploadFile.url
  const index = form.value.detailsImageUrlList.indexOf(url)
  if (index > -1) {
    form.value.detailsImageUrlList.splice(index, 1)
  }
}
const handleDetailsSuccess = (response, uploadFile) => {
  form.value.detailsImageUrlList.push(response.data.url)
}
</script>
<style>
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
  width: 70px;
  height: 70px;
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 50px;
  height: 50px;
  text-align: center;
}
</style>