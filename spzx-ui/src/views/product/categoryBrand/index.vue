<template>
  <div class="app-container">
    <el-form ref="queryRef" :inline="true" label-width="68px" v-show="showSearch">
      <el-form-item label="分类">
        <el-cascader
            :props="categoryProps"
            style="width: 100%"
            v-model = "queryCategoryIdList"
            @change="handleCategoryChange"
        />
      </el-form-item>

      <el-form-item label="品牌">
        <el-select
            class="m-2"
            placeholder="选择品牌"
            size="small"
            style="width: 100%"
            v-model = "queryParams.brandId"
        >
          <!-- 下拉列选选项
              v-for="item in brandList"  对品牌集合进行迭代
              :key="item.id" 表示迭代多个<option>的唯一性。
              :label="item.name" 用品牌名称作为下拉选项的显示名称
              :value="item.id"  用品牌id作为选中选项提交的值
          -->
          <el-option
              v-for="item in brandList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" id="reset-all" @click="queryReset">重置</el-button>
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
      <!-- 功能按钮栏 -->
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据展示表格 -->
    <el-table :data="categoryBrandList" @selection-change="selectionCheckedChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="分类名称" prop="categoryName" />
      <el-table-column label="品牌名称" prop="brandName" />
      <el-table-column prop="logo" label="品牌图标" #default="scope">
        <img :src="scope.row.logo" width="50" />
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页条组件 -->
    <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />


    <!-- 添加或修改分类品牌对话框 -->
    <el-dialog :title="title" v-model="open" :rules="rules" width="500px" append-to-body>
      <el-form ref="categoryBrandRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类" prop="categoryIdList">
          <el-cascader
              :props="categoryProps"
              v-model="form.categoryIdList"
          />
        </el-form-item>
        <el-form-item label="品牌" prop="brandId">
          <el-select
              v-model="form.brandId"
              class="m-2"
              placeholder="选择品牌"
              size="small"
          >
            <el-option
                v-for="item in brandList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="commit">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>


  </div>
</template>






<script setup name="CategoryBrand">
import {selectCategoryBrand,getCategoryBrandById,addCategoryBrand,updateCategoryBrand,deleteCategoryBrandById,getBrandByCategoryId} from '@/api/product/categoryBrand.js'
import { getBrandAll } from '@/api/product/brand.js'
import { getTreeSelect } from '@/api/product/category.js'
// ================数据模型定义  start ===============================================
const {proxy} = getCurrentInstance()

// 定义搜索表单数据模型
const brandList = ref([])

const props = {
  lazy: true, //懒加载
  value: 'id', //提交分类id到queryCategoryIdList数组中
  label: 'name', //显示下拉名称
  leaf: 'leaf', // 'leaf'是临时属性，用于判断当前分类是否为叶子节点。如果是叶子节点，那么不显示 > 箭头了。
  async lazyLoad(node, resolve) {   // 加载数据的方法
    if(typeof node.value == 'undefined'){
      node.value = 0
    }
    const { data } = await getTreeSelect(node.value)

    data.forEach(item=>{
      item.leaf = !item.hasChildren //挂上一个临时属性，叫做  'leaf'
    })

    resolve(data)  // 返回数据
  }
};
const categoryProps = ref(props)

// 定义表格数据模型
const categoryBrandList = ref([
/*  {
    "id": 1,
    "createTime": "2023-05-06 10:59:08",
    "brandId": 2,
    "categoryId": 76,
    "categoryName": "UPS电源\t",
    "brandName": "华为",
    "logo": "http://139.198.127.41:9000/sph/20230506/华为.png"
  }*/
])

// 分页条数据模型
const total = ref(0)

//条件查询，临时存储三个级别分类id
const queryCategoryIdList = ref([])
const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    categoryId: null,
    brandId: null
  },
  rules: {
    categoryIdList: [
      { required: true, message: "分类不能为空", trigger: "blur" }
    ],
    brandId: [
      { required: true, message: "品牌不能为空", trigger: "blur" }
    ]
  },
  form: {
    id: null,
    categoryIdList: [], //临时属性,用于接收三个级别的分类id
    categoryId: null, //提交表单数据时，将categoryId = form.value.categoryIdList[2]
    brandId: null
  }
})

const { queryParams,rules,form } = toRefs(data)

//添加或修改对话框标题
const title = ref('')
//添加或修改对话框是否显示
const open = ref(false)

//按钮栏上修改和删除操作需要的变量
const ids = ref([])
const single = ref(true)
const multiple = ref(true)

//控制隐藏或显示
const showSearch = ref(true)

// =========   数据模型定义 end======================================================================



// =========   函数定义 start======================================================================
//搜索分类下拉列选临时数据处理。
function handleCategoryChange(){
  if(queryCategoryIdList.value.length == 3){
    queryParams.value.categoryId = queryCategoryIdList.value[2]
  }
}

//查询列表数据
function getList(){
  selectCategoryBrand(queryParams.value).then(resultJson=>{
    categoryBrandList.value = resultJson.rows
    total.value = resultJson.total
  })
}

getList()

//查询所有品牌
function getBrandList(){
  getBrandAll().then(resultJson=>{
    brandList.value = resultJson.data
  })
}
getBrandList()

//搜索按钮事件
function handleQuery(){
  getList()
}

//搜索重置事件
function queryReset(){
  queryCategoryIdList.value = []
  queryParams.value.pageNum = 1
  queryParams.value.categoryId = null
  queryParams.value.brandId = null
  getList()
}

//添加按钮的单击事件，弹出添加对话框
function handleAdd(){
  open.value = true
  title.value = '添加'
  reset()
}

function commit(){

  proxy.$refs['categoryBrandRef'].validate(valid=>{
    if(valid){
      if(form.value.categoryIdList.length == 3){
        form.value.categoryId = form.value.categoryIdList[2]
      }
      if(form.value.id){ //id存在进行修改
        updateCategoryBrand(form.value).then(resultJson=>{
          open.value = false
          proxy.$modal.msgSuccess(resultJson.msg)
          getList()
        })
      }else{//添加
        addCategoryBrand(form.value).then(resultJson=>{
          open.value = false
          proxy.$modal.msgSuccess(resultJson.msg)
          getList()
        })
      }
    }
  })
}
//添加或修改对话框取消按钮事件
function cancel(){
  reset()
  open.value = false
}

function reset(){
  // proxy.$refs['categoryBrandRef'].clearValidate('categoryIdList')
  // proxy.$refs['categoryBrandRef'].clearValidate('brandId')
  // form.value.categoryIdList = []
  // form.value.brandId = null
  proxy.resetForm('categoryBrandRef')
}

function handleUpdate(row){
  reset()
  open.value = true
  const _id = row.id || ids.value
  getCategoryBrandById(_id).then(resultJson=>{
    form.value = resultJson.data
  })
}

function handleDelete(row){
  const _id = row.id || ids.value
  proxy.$modal.confirm('您确定要删除数据吗?').then(()=>{
    return deleteCategoryBrandById(_id)
  }).then((resultJson)=>{
    proxy.$modal.msgSuccess(resultJson.msg)
    getList()
  }).catch(()=>{

  })
}

function selectionCheckedChange(selection){
  ids.value = selection.map(item=>item.id)
  single.value = ids.value.length != 1
  multiple.value = ids.value.length == 0
}

// =========   函数定义 end======================================================================

</script>