<template>
  <div class="app-container">

    <!-- 搜索表单 -->
    <el-form ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="品牌名称" prop="name">
        <el-input
            placeholder="请输入品牌名称"
            clearable
            v-model="queryParams.name"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery" v-hasPermi="['product:brand:list']">搜索</el-button>
        <el-button icon="Refresh" @click="queryReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 功能按钮栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['product:brand:add']"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['product:brand:edit']"
        >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['product:brand:remove']"
        >删除
        </el-button>
      </el-col>

      <!-- 功能按钮栏 -->
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" v-hasPermi="['product:brand:list']"></right-toolbar>
    </el-row>

    <!-- 数据展示表格 -->
    <el-table :data="brandList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="品牌名称" prop="name" width="200"/>
      <el-table-column label="品牌图标" prop="logo" #default="scope">
        <img :src="scope.row.logo" width="50"/>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['product:brand:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['product:brand:remove']">删除</el-button>
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


    <!-- 新增或修改分类品牌对话框 -->
    <el-dialog :title="title" v-model="open"  width="500px" append-to-body>
      <el-form ref="brandRef" :model="form" :rules="rules"  label-width="80px">
        <el-form-item label="品牌名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入品牌名称"/>
        </el-form-item>
        <el-form-item label="品牌图标" prop="logo">
          <el-upload
              class="avatar-uploader"
              :action="imgUpload.url"
              :headers="imgUpload.headers"
              :show-file-list="false"
              :before-upload="beforeUpload"
              :on-success="handleAvatarSuccess"
          >
            <img v-if="form.logo" :src="form.logo" class="avatar"/>
            <el-icon v-else class="avatar-uploader-icon">
              <Plus/>
            </el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="commit" >确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="Brand">
//=====================导入区域=================================
import {addBrand, deleteBrand, getBrand, listBrand, updateBrand} from '@/api/product/brand.js'
//import * as brandApi from '@/api/product/brand.js'
import {getToken} from '@/utils/auth'

import {ElMessage, ElMessageBox} from 'element-plus'


//=====================数据模型、变量 定义区域=====================
const { proxy } = getCurrentInstance();


//定义分页列表数据模型
const brandList = ref([]);
//定义列表总记录数模型
const total = ref(2);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 2,
    name: ''
  },
  form: {
    id: null,
    name: null,
    logo: null,
  },
  imgUpload: {
    url: import.meta.env.VITE_APP_BASE_API + "/file/upload",
    headers: {
      Authorization: 'Bearer ' + getToken()
    }
  },
  // 表单校验规则
  rules: {
    name: [{ required: true, message: "品牌名称不能为空1111", trigger: "blur" }],
    logo: [{ required: true, message: "品牌LOGO不能为空", trigger: "blur" }],
  }
})

const {queryParams, form, imgUpload, rules } = toRefs(data) // 将声明的多个数据解析成一个一个的ref数据
//控制添加、修改对话框显示
const open = ref(false)

//添加、修改对话框标题
const title = ref('')

//控制修改按钮是否可用
const single = ref(true)
//控制删除按钮是否可用
const multiple = ref(true)
//批量修改和删除 数据ids
const ids = ref([])

//定义隐藏搜索控制模型
const showSearch = ref(true);

//=====================事件、函数  声明区域=======================

function getList() { //调用api接口，向后端发起请求
  listBrand(queryParams.value).then((resultJson) => {
    brandList.value = resultJson.rows
    total.value = resultJson.total
  })
}

getList(); //页面加载完成就调用list函数

function handleQuery() {
  getList()
}

function queryReset() {
  // queryParams.value.name = ''
  queryParams.value.pageNum = 1
  queryParams.value.pageSize = 10
  proxy.resetForm('queryRef') //重置表单项
  handleQuery()
}

//弹出对话框
function handleAdd() {
  open.value = true
  title.value = '添加品牌'
  reset()
}

function reset() {
  form.value.id = null   //保留，因为proxy.resetForm('brandRef')重置表单，只能重置表单引用到的字段。
  //form.value.name = null
  //form.value.logo = null
  //form.value = {}
  proxy.resetForm('brandRef')
}


/*const beforeUpload = (rawFile) => {
  if (rawFile.type !== 'image/jpeg') {
    ElMessage.error('图片必须是JPG格式!')
    return false
  } else if (rawFile.size / 1024 / 1024 > 1) {
    ElMessage.error('图片不能大于 1MB!')
    return false
  }
  return true
}*/
const beforeUpload = (rawFile) => {
  // 允许的图片类型：jpg、jpeg、png
  const allowedTypes = ['image/jpeg', 'image/png'];

  // 判断文件格式是否在允许范围内
  if (!allowedTypes.includes(rawFile.type)) {
    ElMessage.error('图片必须是 JPG / PNG 格式!');
    return false;
  }
  // 判断文件大小不超过 1MB
  else if (rawFile.size / 1024 / 1024 > 1) {
    ElMessage.error('图片不能大于 1MB!');
    return false;
  }
  return true;
};


//上传图片成功后进行回调处理
function handleAvatarSuccess(resultJson, uploadFile) {
  //{"code":200,"msg":null,"data":{"name":"p1_20240923114612A001.jpg","url":"http://192.168.6.121:9000/spzx/2024/09/23/p1_20240923114612A001.jpg"}}
  console.log(resultJson)
  console.log(uploadFile)  //  uploadFile.name

  form.value.logo = resultJson.data.url //将后端返回的图片地址赋值给数据模块(响应式的数据模型)
  proxy.$refs['brandRef'].clearValidate('logo')
}

//添加或修改
function commit() {

  proxy.$refs['brandRef'].validate((valid)=>{
    if(valid){ //表单校验没问题。可以提交请求
      if (form.value.id) { //值存在则进行修改操作
        updateBrand(form.value).then(resultJson => {
          open.value = false //隐藏对话框
          //ElMessage.success(resultJson.msg)
          proxy.$modal.msgSuccess(resultJson.msg)
          getList();
        })
      } else { //id值不在，进行添加操作
        addBrand(form.value).then(resultJson => {
          open.value = false //隐藏对话框
          //ElMessage.success(resultJson.msg)
          proxy.$modal.msgSuccess(resultJson.msg)
          getList();
        })
      }
    }else{//表单数据校验存在问题。不去提交请求
      proxy.$modal.msgError('表单数据校验失败')
    }
  })



}

//列表后，每一个行上修改按钮的事件处理
function handleUpdate(row) {
  reset()
  open.value = true
  title.value = '修改品牌'
  var _id = row.id || ids.value
  getBrand(_id).then(resultJson => {
    form.value = resultJson.data
  })
}


function cancel() {
  open.value = false
  reset()
}


function handleDelete(row) {
  proxy.$modal.confirm('您确定要删除吗?').then(() => { // 确定 执行 then回调处理
    const _id = row.id || ids.value
    return deleteBrand(_id)
  }).then((resultJson) => {
    ElMessage.success(resultJson.msg)
    getList();
  }).catch((e) => { // 取消执行  catch回调处理
    if ('cancel' === e) {
      ElMessage.info('取消删除')
    } else {
      ElMessage.error('请求失败')
    }
  })
}

/*function handleDelete(row) {
  ElMessageBox.confirm('您确定要删除吗?', '友情提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => { // 确定 执行 then回调处理
    const _id = row.id || ids.value
    return deleteBrand(_id)
  }).then((resultJson) => {
    ElMessage.success(resultJson.msg)
    getList();
  }).catch((e) => { // 取消执行  catch回调处理
    if ('cancel' === e) {
      ElMessage.info('取消删除')
    } else {
      ElMessage.error('请求失败')
    }
  })
}*/

/*function handleDelete(row){
  ElMessageBox.confirm('您确定要删除吗?','友情提示',{
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type : 'warning'
  }).then(()=>{ // 确定 执行 then回调处理
    const _id = row.id
    deleteBrand(_id).then(resultJson=>{
      ElMessage.success(resultJson.msg)
      getList();
    })
  }).catch((e)=>{ // 取消执行  catch回调处理
    if('cancel'===e){
      ElMessage.info('取消删除')
    }else{
      ElMessage.error('请求失败')
    }
  })
}
 */


function handleSelectionChange(selection) { // selection表示勾选的元素集合
  ids.value = selection.map(item => item.id)
  console.log(ids.value)
  single.value = ids.value.length != 1
  multiple.value = ids.value.length == 0
}


</script>

<style scoped>
.avatar-uploader .avatar {
  width: 178px;
  height: 178px;
  display: block;
}
</style>

<style>
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
}
</style>


// {
//     "total": 6,
//     "rows": [
//     {
//         "id": 13,
//         "createBy": null,
//         "createTime": "2024-09-20 09:45:01",
//         "updateBy": null,
//         "updateTime": null,
//         "remark": null,
//         "delFlag": null,
//         "name": "联想",
//         "logo": "123"
//     },
//
//     ],
//     "code": 200,
//     "msg": "查询成功"
// }
