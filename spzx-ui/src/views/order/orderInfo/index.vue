<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">

      <el-form-item label="订单号" prop="orderNo">
        <el-input
            v-model="queryParams.orderNo"
            placeholder="请输入订单号"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单状态">
        <el-select
            v-model="queryParams.orderStatus"
            class="m-2"
            placeholder="订单状态"
            style="width: 100%"
        >
          <el-option
              v-for="item in orderStatusList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" style="width: 308px">
        <el-date-picker
            v-model="dateRange"
            value-format="YYYY-MM-DD"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item label="收货人姓名">
        <el-input
            style="width: 100%"
            v-model="queryParams.receiverName"
            placeholder="收货人姓名"
        ></el-input>
      </el-form-item>
      <el-form-item label="收货人手机">
        <el-input
            style="width: 100%"
            v-model="queryParams.receiverPhone"
            placeholder="收货人手机"
        ></el-input>
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
            v-hasPermi="['order:info:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="infoList">
      <el-table-column prop="orderNo" label="订单号"/>
      <el-table-column prop="totalAmount" label="订单总额" width="80"/>
      <el-table-column prop="orderStatus" label="订单状态" #default="scope" width="80">
        {{
          scope.row.orderStatus == 0
              ? '待支付'
              : scope.row.orderStatus == 1
                  ? '待发货'
                  : scope.row.orderStatus == 2
                      ? '已发货'
                      : scope.row.orderStatus == -1
                          ? '已取消'
                          : '完成'
        }}
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160"/>
      <el-table-column label="支付时间" align="center" prop="paymentTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.paymentTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="receiverName" label="收货人姓名" width="100"/>
      <el-table-column prop="receiverPhone" label="收货人电话" width="120"/>
      <el-table-column prop="receiverAddress" label="详细地址" width="380"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="60">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleShow(scope.row)">详情</el-button>
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

    <!-- 添加或修改订单对话框 -->
    <el-dialog :title="title" v-model="open" width="60%" append-to-body>
      <el-form ref="infoRef" :model="form" :rules="rules" label-width="120px">
        <el-divider/>
        <span style="margin-bottom: 5px;font-weight:bold;">订单基本信息</span>
        <el-row>
          <el-col :span="12">
            <el-form-item label="订单号">
              {{ form.orderNo }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单总额">
              {{ form.totalAmount }}
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="优惠券">
              {{ form.couponAmount }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价金额">
              {{ form.originalTotalAmount }}
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="运费">
              {{ form.feightFee }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付方式">
              支付宝
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="订单状态">
              {{
                form.orderStatus == 0
                    ? '待支付'
                    : form.orderStatus == 1
                        ? '待发货'
                        : form.orderStatus == 2
                            ? '已发货'
                            : form.orderStatus == -1
                                ? '已取消'
                                : '完成'
              }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创建时间">
              {{ form.createTime }}
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="支付时间">
              {{ form.paymentTime }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单备注">
              {{ form.remark }}
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider/>
        <span style="margin-bottom: 5px;font-weight:bold;">收货人信息</span>
        <el-row>
          <el-col :span="12">
            <el-form-item label="收货人姓名">
              {{ form.receiverName }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收货人电话">
              {{ form.receiverPhone }}
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="地址标签">
              {{ form.receiverTagName }}
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收货详细地址">
              {{ form.receiverAddress }}
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider/>
        <span style="margin-bottom: 5px;font-weight:bold;">订单明细信息</span>
        <el-table :data="form.orderItemList" style="width: 100%">
          <el-table-column prop="skuName" label="SKU名称"/>
          <el-table-column label="图片" #default="scope" width="150">
            <img :src="scope.row.thumbImg" width="50"/>
          </el-table-column>
          <el-table-column prop="skuPrice" label="SKU价格" width="120"/>
          <el-table-column prop="skuNum" label="购买数量" width="120"/>
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

<script setup name="Info">
import {getInfo, listInfo} from "@/api/order/orderInfo";

const {proxy} = getCurrentInstance();

const infoList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const title = ref("");

const orderStatusList = ref([
  {id: 0, name: '待支付'},
  {id: 1, name: '待发货'},
  {id: 2, name: '已发货'},
  {id: 3, name: '完成'},
  {id: -1, name: '已取消'},
])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: '',
    orderStatus: '',
    receiverName: '',
    receiverPhone: '',
    createTimeBegin: '',
    createTimeEnd: ''
  }
});

const {queryParams, form, rules} = toRefs(data);

const dateRange = ref([]);

/** 查询会员列表 */
function getList() {
  loading.value = true;

  listInfo(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    infoList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
  queryParams.value.orderStatus = null
  queryParams.value.receiverName = null
  queryParams.value.receiverPhone = null
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleShow(row) {
  const _id = row.id
  getInfo(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "详情";
  });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('order/orderInfo/export', {
    ...proxy.addDateRange(queryParams.value, dateRange.value)
  }, `info_${new Date().getTime()}.xlsx`)
}

getList();
</script>