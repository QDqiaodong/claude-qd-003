<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-select v-model="query.batchId" placeholder="按批次" clearable style="width:220px">
          <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo} · ${b.status}`" :value="b.id" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openTransfer">开转棚调拨单</el-button>
        <span style="margin-left:auto;color:#909399">共 {{ rows.length }} 张调拨单</span>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>转棚调拨单（落账即转移占用，只改棚名字不算）</template>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="transferNo" label="调拨单号" width="110" />
        <el-table-column label="批次" width="120">
          <template #default="{ row }">{{ batchLabel(row.batchId) }}</template>
        </el-table-column>
        <el-table-column label="调出床" min-width="140">
          <template #default="{ row }">
            {{ bedLabel(row.fromSeedbedId) }}
            <div class="house">{{ houseOf(row.fromSeedbedId) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="调入床" min-width="140">
          <template #default="{ row }">
            {{ bedLabel(row.toSeedbedId) }}
            <div class="house">{{ houseOf(row.toSeedbedId) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="planQty" label="调拨株数" width="90" />
        <el-table-column prop="operator" label="经手人" width="90" />
        <el-table-column label="计划搬迁" width="200">
          <template #default="{ row }">
            {{ row.planStartDate }} 起迁<br />
            {{ row.planEndDate }} 迁完
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag type="success">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="开转棚调拨单" width="560px">
      <el-form label-width="110px">
        <el-form-item label="批次">
          <el-select
            v-model="form.batchId"
            placeholder="只列还占着床的批次（育苗中/待出圃）"
            filterable
            style="width:100%"
            @change="onBatchChange"
          >
            <el-option
              v-for="b in transferableBatches"
              :key="b.id"
              :label="`${b.batchNo} · ${varietyName(b.varietyId)} · ${bedName(b.seedbedId)} · 计划 ${b.planQty} 株`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调出床">
          <el-input :model-value="fromBedLabel" disabled />
        </el-form-item>
        <el-form-item label="调入床">
          <el-select v-model="form.toSeedbedId" placeholder="只列在用、且与调出床不同的床" style="width:100%">
            <el-option
              v-for="b in toBedOptions"
              :key="b.id"
              :label="`${b.name}（${b.code} · ${houseName(b.greenhouseId)} · 可放 ${b.capacity} 株）`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调拨株数">
          <el-input-number v-model="form.planQty" :min="1" :step="50" />
          <span v-if="toBedCap" :style="{ color: form.planQty > toBedCap ? '#f56c6c' : '#909399', marginLeft: '10px' }">
            新床最多放 {{ toBedCap }} 株
          </span>
        </el-form-item>
        <el-form-item label="开始迁日期">
          <el-date-picker v-model="form.planStartDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="计划迁完日期">
          <el-date-picker v-model="form.planEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="经手人">
          <el-input v-model="form.operator" placeholder="谁经手搬这一趟" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">开单并落账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { batchApi, greenhouseApi, seedbedApi, transferApi, varietyApi } from '../api'

const HOLDING = ['育苗中', '待出圃']

const rows = ref([])
const batches = ref([])
const beds = ref([])
const houses = ref([])
const varieties = ref([])
const loading = ref(false)
const query = reactive({ batchId: null })

const dialogVisible = ref(false)
const today = new Date().toISOString().slice(0, 10)
const form = reactive({
  batchId: null,
  fromSeedbedId: null,
  toSeedbedId: null,
  planQty: 100,
  planStartDate: today,
  planEndDate: today,
  operator: ''
})

const route = useRoute()
const router = useRouter()

const transferableBatches = computed(() => batches.value.filter((b) => HOLDING.includes(b.status)))
const currentBatch = computed(() => batches.value.find((b) => b.id === form.batchId) || null)
const fromBed = computed(() => beds.value.find((b) => b.id === form.fromSeedbedId) || null)
const fromBedLabel = computed(() => (fromBed.value
  ? `${fromBed.value.name}（${fromBed.value.code} · ${houseName(fromBed.value.greenhouseId)}）`
  : '选了批次自动带出'))
const toBedOptions = computed(() =>
  beds.value.filter((b) => b.status === '在用' && b.id !== form.fromSeedbedId)
)
const toBedCap = computed(() => beds.value.find((b) => b.id === form.toSeedbedId)?.capacity || null)

const bedName = (id) => (id ? beds.value.find((b) => b.id === id)?.name || `#${id}` : '未安排')
const bedLabel = (id) => {
  const b = beds.value.find((x) => x.id === id)
  return b ? `${b.name}（${b.code}）` : `#${id}`
}
const houseName = (id) => (id ? houses.value.find((h) => h.id === id)?.name || '未归棚' : '未归棚')
const houseOf = (id) => {
  const b = beds.value.find((x) => x.id === id)
  return b ? houseName(b.greenhouseId) : ''
}
const varietyName = (id) => (id ? varieties.value.find((v) => v.id === id)?.name || `#${id}` : '—')
const batchLabel = (id) => {
  const b = batches.value.find((x) => x.id === id)
  return b ? `${b.batchNo}` : `#${id}`
}

const load = async () => {
  loading.value = true
  try {
    rows.value = await transferApi.list({ batchId: query.batchId || undefined })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const onBatchChange = (id) => {
  const b = batches.value.find((x) => x.id === id)
  form.fromSeedbedId = b ? b.seedbedId : null
  form.toSeedbedId = null
  form.planQty = b ? b.planQty : 100
}

const resetForm = () => {
  Object.assign(form, {
    batchId: null,
    fromSeedbedId: null,
    toSeedbedId: null,
    planQty: 100,
    planStartDate: today,
    planEndDate: today,
    operator: ''
  })
}

const openTransfer = () => {
  resetForm()
  const preset = route.query.batchId ? Number(route.query.batchId) : null
  if (preset && transferableBatches.value.some((b) => b.id === preset)) {
    form.batchId = preset
    onBatchChange(preset)
  }
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.batchId) return ElMessage.error('请选批次')
  if (!form.toSeedbedId) return ElMessage.error('请选调入床')
  if (!form.operator.trim()) return ElMessage.error('请填经手人')
  try {
    await transferApi.post({ ...form })
    ElMessage.success('调拨单已落账：旧床已让出、新床已占用')
    dialogVisible.value = false
    if (route.query.batchId) {
      router.replace({ path: route.path, query: {} })
    }
    await Promise.all([loadBatches(), load()])
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const loadBatches = async () => {
  batches.value = await batchApi.list({})
}

onMounted(async () => {
  try {
    const [b, s, h, v] = await Promise.all([
      batchApi.list({}),
      seedbedApi.list({}),
      greenhouseApi.list(),
      varietyApi.list()
    ])
    batches.value = b
    beds.value = s
    houses.value = h
    varieties.value = v
  } catch (e) {
    ElMessage.error(e.message)
  }
  await load()
  if (route.query.batchId) {
    openTransfer()
  }
})
</script>

<style scoped>
.house {
  color: #909399;
  font-size: 12px;
  line-height: 16px;
}
</style>
