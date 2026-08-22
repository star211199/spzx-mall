<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="分类">
        <el-cascader
            :props="categoryProps"
            v-model="queryCategoryIdList"
            @change="handleCategoryChange"
            style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="规格名称" prop="specName">
        <el-input
            v-model="queryParams.specName"
            placeholder="请输入规格名称"
            clearable
            @keyup.enter="handleQuery"
        />
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
        >删除
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="specList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="分类名称" prop="categoryName" width="120"/>
      <el-table-column label="规格名称" align="left" prop="specName" width="120"/>
      <el-table-column label="规格值" #default="scope">
        <div
            v-for="(item1, index1) in scope.row.specValueList"
            :key="index1"
            style="padding: 5px; margin: 0;width: 100%;"
        >
          {{ item1.key }}：
          <span
              v-for="(item2, index2) in item1.valueList"
              :key="index2"
              class="div-atrr"
          >
          {{ item2 }}
        </span>
        </div>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页条组件 -->
    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

    <!-- 添加或修改商品规格对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="specRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类" prop="categoryIdList">
          <el-cascader
              :props="categoryProps"
              v-model="form.categoryIdList"
          />
        </el-form-item>
        <el-form-item label="规格名称" prop="specName">
          <el-input v-model="form.specName" placeholder="请输入规格名称"/>
        </el-form-item>
        <el-form-item>
          <div
              v-for="(item1, index1) in specValueList"
              :key="index1"
              style="padding: 10px; margin: 0;"
          >
            {{ item1.key }}：
            <span
                v-for="(item2, index2) in item1.valueList"
                :key="index2"
                class="div-atrr"
            >
            {{ item2 }}
          </span>
            <el-button size="small" @click="removeAttr(index1)">删除</el-button>
            <br/>
          </div>
        </el-form-item>
        <el-form-item label="">
          <el-row v-if="isAdd">
            <el-col :span="10">
              <el-input
                  v-model="specValue.key"
                  placeholder="规格"
                  style="width: 90%;"
              />
            </el-col>
            <el-col :span="10">
              <el-input
                  v-model="specValue.values"
                  placeholder="规格值(如:白色,红色)"
                  style="width: 90%;"
              />
            </el-col>
            <el-col :span="4">
              <el-button size="default" @click="addSpecValue">添加</el-button>
            </el-col>
          </el-row>
          <el-row v-if="!isAdd">
            <el-col :span="4" align="left">
              <el-button size="default" @click="isAdd = true">
                添加新规格
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>

      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import {addSpec, delSpec, getSpec, listSpec, updateSpec} from "@/api/product/productSpec";
import {getTreeSelect} from "@/api/product/category";

const {proxy} = getCurrentInstance();

//定义分页列表数据模型
const specList = ref([]);
//定义列表总记录数模型
const total = ref(0);
//加载数据时显示的动效控制模型
const loading = ref(true);
//定义隐藏搜索控制模型
const showSearch = ref(true);
//新增与修改弹出层控制模型
const open = ref(false);
//新增与修改弹出层标题模型
const title = ref("");
//定义批量操作id列表模型
const ids = ref([]);
//定义单选控制模型
const single = ref(true);
//定义多选控制模型
const multiple = ref(true);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    categoryId: null,
    specName: null
  },
  form: {},
  rules: {
    categoryIdList: [
      {required: true, message: "分类不能为空", trigger: "blur"}
    ],
    specName: [
      {required: true, message: "规则名称不能为空", trigger: "blur"}
    ]
  }
});

const {queryParams, form, rules} = toRefs(data);

/** 查询商品规格列表 */
function getList() {
  loading.value = true;
  listSpec(queryParams.value).then(response => {
    specList.value = response.rows;

    specList.value.forEach(function (item) {
      //解析规格属性值，字符串转换为json数组，挂载到临时属性上
      item.specValueList = JSON.parse(item.specValue)
    })

    total.value = response.total;
    loading.value = false;
  });
}

//三级分类
const props = {
  lazy: true,
  value: 'id',
  label: 'name',
  leaf: 'leaf',
  async lazyLoad(node, resolve) {
    const {level} = node
    if (typeof node.value == 'undefined') node.value = 0
    const {code, data, message} = await getTreeSelect(
        node.value
    )
    //hasChildren判断是否有子节点
    data.forEach(function (item) {
      item.leaf = !item.hasChildren
    })
    resolve(data)
  },
}
const categoryProps = ref(props)

//处理分类change事件
const queryCategoryIdList = ref([])
const handleCategoryChange = () => {
  if (queryCategoryIdList.value.length == 3) {
    queryParams.value.categoryId = queryCategoryIdList.value[2]
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryCategoryIdList.value = []
  proxy.resetForm("queryRef");
  queryParams.value.categoryId = null
  handleQuery();
}

//编辑规格属性
const isAdd = ref(false)
const specValue = ref({key: '', values: ''})
const specValueList = ref([])

function addSpecValue() {
  if (!specValue.value.key || !specValue.value.values) {
    proxy.$modal.msgError("请填写规格名和规格值");
    return
  }
  specValueList.value.push({
    key: specValue.value.key,
    valueList: specValue.value.values.split(','),
  })
  specValue.value = {key: '', values: ''}
  isAdd.value = false
}

function removeAttr(index) {
  specValueList.value.splice(index, 1)
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加商品规格";

  specValueList.value = []
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    specName: null,
    specValue: null,
    categoryId: null,
    categoryIdList: []
  };
  proxy.resetForm("specRef");
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getSpec(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改商品规格";

    specValueList.value = JSON.parse(form.value.specValue)
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["specRef"].validate(valid => {
    if (valid) {
      form.value.specValue = JSON.stringify(specValueList.value)
      //系统只需要三级分类id
      form.value.categoryId = form.value.categoryIdList[2]

      if (form.value.id != null) {
        updateSpec(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addSpec(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除商品规格编号为"' + _ids + '"的数据项？').then(function () {
    return delSpec(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

getList()
</script>


<style scoped>
.div-atrr {
  padding: 5px 10px;
  margin: 0 10px;
  background-color: powderblue;
  border-radius: 10px;
}
</style>