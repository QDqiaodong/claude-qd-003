<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-select v-model="query.status" placeholder="按状态" clearable style="width:150px">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="query.batchId" placeholder="按批次" clearable style="width:200px">
          <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo} · ${b.status}`" :value="b.id" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openShip">开一张发货单</el-button>
        <span style="margin-left:auto;color:#909399">共 {{ rows.length }} 张</span>
      </div>
    </el-card>

    <el-table :data="rows" border stripe size="small" style="margin-top:12px" v-loading="loading">
      <el-table-column prop="shipmentNo" label="发货单号" width="110" />
      <el-table-column label="批次" width="150">
        <template #default="{ row }">{{ batchLabel(row.batchId) }}</template>
      </el-table-column>
      <el-table-column prop="customer" label="收货方" min-width="150" />
      <el-table-column prop="qty" label="株数" width="90" />
      <el-table-column prop="carrier" label="承运人" width="100" />
      <el-table-column prop="shipDate" label="发车日期" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button v-if="row.status === '待发货'" link type="primary" @click="openGo(row)">发车</el-button>
          <template v-if="row.status === '已发货'">
            <el-button link type="success" @click="advance(row, 'sign')">签收</el-button>
            <el-button link type="danger" @click="advance(row, 'back')">退回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="shipVisible" title="开一张发货单" width="500px">
      <el-form label-width="100px">
        <el-form-item label="批次">
          <el-select v-model="shipForm.batchId" placeholder="只列能发货的批次" style="width:100%">
            <el-option
              v-for="b in shippableBatches"
              :key="b.id"
              :label="`${b.batchNo} · ${varietyName(b.varietyId)} · 可发 ${remainQty(b)} 株`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="收货方"><el-input v-model="shipForm.customer" /></el-form-item>
        <el-form-item label="株数"><el-input-number v-model="shipForm.qty" :min="1" :step="10" /></el-form-item>
        <el-form-item label="承运人"><el-input v-model="shipForm.carrier" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" @click="submitShip">开单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="goVisible" title="发车" width="440px">
      <el-form label-width="100px">
        <el-form-item label="承运人"><el-input v-model="goForm.carrier" /></el-form-item>
        <el-form-item label="发车日期">
          <el-date-picker v-model="goForm.shipDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="goVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGo">确定发车</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { batchApi, shipmentApi, varietyApi } from '../api'

const statuses = ['待发货', '已发货', '已签收', '已退回']

const rows = ref([])
const batches = ref([])
const varieties = ref([])
const loading = ref(false)
const query = reactive({ status: '', batchId: null })

const shipVisible = ref(false)
const shipForm = reactive({ batchId: null, customer: '', qty: 100, carrier: '' })
const goVisible = ref(false)
const goForm = reactive({ id: null, carrier: '', shipDate: '' })

const shippableBatches = computed(() =>
  batches.value.filter((b) => b.status === '待出圃' || b.status === '已出圃')
)

const varietyName = (id) => (id ? varieties.value.find((v) => v.id === id)?.name || `#${id}` : '—')
const batchLabel = (id) => {
  const b = batches.value.find((x) => x.id === id)
  return b ? `${b.batchNo} · ${varietyName(b.varietyId)}` : `#${id}`
}
const tagType = (s) =>
  s === '已签收' ? 'success' : s === '已发货' ? 'warning' : s === '已退回' ? 'danger' : ''

/** 这批还能发多少：成苗株数 - 没退回来的那些单累计 */
const remainQty = (batch) => {
  const shipped = rows.value
    .filter((r) => r.batchId === batch.id && r.status !== '已退回')
    .reduce((sum, r) => sum + r.qty, 0)
  return batch.actualQty - shipped
}

const load = async () => {
  loading.value = true
  try {
    rows.value = await shipmentApi.list({
      status: query.status || undefined,
      batchId: query.batchId || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const openShip = () => {
  Object.assign(shipForm, { batchId: null, customer: '', qty: 100, carrier: '' })
  shipVisible.value = true
}

const submitShip = async () => {
  try {
    await shipmentApi.open({ ...shipForm })
    ElMessage.success('发货单已开')
    shipVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openGo = (row) => {
  Object.assign(goForm, {
    id: row.id,
    carrier: row.carrier || '',
    shipDate: new Date().toISOString().slice(0, 10)
  })
  goVisible.value = true
}

const submitGo = async () => {
  try {
    await shipmentApi.advance(goForm.id, 'ship', goForm.carrier, goForm.shipDate)
    ElMessage.success('已发车')
    goVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const advance = async (row, action) => {
  try {
    await shipmentApi.advance(row.id, action, null, null)
    ElMessage.success('已更新')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    const [b, v] = await Promise.all([batchApi.list({}), varietyApi.list()])
    batches.value = b
    varieties.value = v
  } catch (e) {
    ElMessage.error(e.message)
  }
  await load()
})
</script>
