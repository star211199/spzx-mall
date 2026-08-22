<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入用户名或昵称或手机号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>

      <el-form-item label="创建时间" style="width: 308px">
        <el-date-picker
          v-model="daterangeCreateTime"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">

      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['user:userInfo:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="userInfoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="id" align="center" prop="id" />
      <el-table-column label="用户名" align="center" prop="username" />
      <el-table-column label="昵称" align="center" prop="nickName" />
      <el-table-column label="头像" prop="avatar" #default="scope">
        <img :src="scope.row.avatar" width="50"/>
      </el-table-column>
      <el-table-column prop="sex" label="性别" #default="scope">
        {{ scope.row.sex == 0 ? '男' : '女' }}
      </el-table-column>
      <el-table-column label="电话号码" align="center" prop="phone" />
      <el-table-column label="最后一次登录时间" align="center" prop="lastLoginTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.lastLoginTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" #default="scope" align="center">
        {{ scope.row.status == 1 ? '正常' : '禁止' }}
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleDetail(scope.row)" v-hasPermi="['user:userInfo:query']">详情</el-button>
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

    <!-- 添加或修改会员对话框 -->
    <el-dialog :title="title" v-model="open" width="50%" append-to-body>
      <el-form ref="userInfoRef" :model="form" :rules="rules" label-width="120px">
        <el-divider />
        <span style="margin-bottom: 5px;">基本信息</span>
        <el-row>
          <el-col :span="12">
            <el-form-item label="头像">
              <img :src="form.avatar" width="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名">
              {{ form.username }}
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="昵称">
              {{ form.nickName }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              {{ form.sex == 1 ? '女' : '男' }}
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="电话号码">
              {{ form.phone }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              {{ form.memo }}
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="最后一次登录ip">
              {{ form.lastLoginIp }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最后一次登录时间">
              {{ form.lastLoginTime }}
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="状态">
              {{ form.status == 1 ? '正常' : '停用' }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创建时间">
              {{ form.createTime }}
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider />
        <span style="margin-bottom: 5px;">地址信息</span>
        <el-table :data="userAddressList" style="width: 100%">
          <el-table-column prop="tagName" label="地址标签" width="80" />
          <el-table-column
              prop="isDefault"
              label="是否默认"
              #default="scope"
              width="100"
          >
            {{ scope.row.isDefault == 1 ? '是' : '否' }}
          </el-table-column>
          <el-table-column prop="isDefault" label="详细信息" #default="scope">
            {{ scope.row.name }} | {{ scope.row.phone }} |
            {{ scope.row.fullAddress }}
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160" />
        </el-table>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="UserInfo">
import { listUserInfo, getUserInfo,getUserAddress  } from "@/api/user/userInfo";

const { proxy } = getCurrentInstance();

const userInfoList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);

const total = ref(0);
const title = ref("");
const daterangeCreateTime = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    username: null,
    //params: {} //后端是map类型接收多个参数
  },
  rules: {
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" }
    ],
    updateTime: [
      { required: true, message: "更新时间不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

const userAddressList = ref([])

/** 查询会员列表 */
function getList() {
  loading.value = true;
  queryParams.value.params = {};
  if (null != daterangeCreateTime && daterangeCreateTime.value.length == 2) {
    queryParams.value.params["beginTime"] = daterangeCreateTime.value[0];
    queryParams.value.params["endTime"] = daterangeCreateTime.value[1];
  }
  listUserInfo(queryParams.value).then(response => {
    userInfoList.value = response.rows;
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
    username: null,
    password: null,
    nickName: null,
    phone: null,
    avatar: null,
    sex: null,
    memo: null,
    openId: null,
    unionId: null,
    lastLoginIp: null,
    lastLoginTime: null,
    status: null,
    createTime: null,
    createBy: null,
    updateTime: null,
    updateBy: null,
    delFlag: null,
    remark: null
  };

}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  daterangeCreateTime.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}



/** 详情按钮操作 */
function handleDetail(row) {
  const _id = row.id
  getUserInfo(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "会员详情";
  });
  getUserAddress(_id).then(response=>{
    userAddressList.value = response.data
  })

}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('user/userInfo/export', {
    ...queryParams.value
  }, `userInfo_${new Date().getTime()}.xlsx`)
}

getList();
</script>
