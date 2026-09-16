<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px">
          <span>品种</span>
          <el-button size="small" type="success" @click="openVariety">新增品种</el-button>
          <span style="margin-left:auto;color:#909399">
            在售 {{ varieties.filter(v => v.status === '在售').length }} 个 / 共 {{ varieties.length }} 个
          </span>
        </div>
      </template>
      <el-table :data="varieties" border stripe size="small">
        <el-table-column prop="code" label="品种编号" width="120" />
        <el-table-column prop="name" label="品种名称" min-width="150" />
        <el-table-column prop="category" label="类别" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '在售' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link :type="row.status === '在售' ? 'danger' : 'primary'" @click="toggleVariety(row)">
              {{ row.status === '在售' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
          <span>育苗批次</span>
          <el-select v-model="query.status" placeholder="按状态" clearable size="small" style="width:140px">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
          <el-select v-model="query.varietyId" placeholder="按品种" clearable size="small" style="width:160px">
            <el-option v-for="v in varieties" :key="v.id" :label="v.name" :value="v.id" />
          </el-select>
          <el-input v-model="query.keyword" placeholder="批次号或师傅" clearable size="small" style="width:170px" />
          <el-button size="small" type="primary" @click="loadBatches">查询</el-button>
          <el-button size="small" type="success" @click="openBatch">开一批苗</el-button>
        </div>
      </template>
      <el-table :data="batches" border stripe size="small" v-loading="loading">
        <el-table-column prop="batchNo" label="批次号" width="110" />
        <el-table-column label="品种" width="120">
          <template #default="{ row }">{{ varietyName(row.varietyId) }}</template>
        </el-table-column>
        <el-table-column label="苗床" width="150">
          <template #default="{ row }">{{ bedName(row.seedbedId) }}</template>
        </el-table-column>
        <el-table-column label="档期" width="200">
          <template #default="{ row }">{{ row.sowDate }} ~ {{ row.expectOutDate || '未定' }}</template>
        </el-table-column>
        <el-table-column prop="planQty" label="计划株数" width="100" />
        <el-table-column prop="actualQty" label="成苗株数" width="100" />
        <el-table-column prop="grower" label="师傅" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210">
          <template #default="{ row }">
            <el-button v-if="row.status === '育苗中'" link type="primary" @click="openReady(row)">转待出圃</el-button>
            <el-button v-if="row.status === '待出圃'" link type="success" @click="advance(row, 'out')">出圃</el-button>
            <el-button
              v-if="row.status !== '已出圃' && row.status !== '已报废'"
              link
              type="danger"
              @click="scrap(row)"
            >报废</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="varietyVisible" title="新增品种" width="440px">
      <el-form label-width="90px">
        <el-form-item label="品种编号"><el-input v-model="varietyForm.code" placeholder="如 V-1007" /></el-form-item>
        <el-form-item label="品种名称"><el-input v-model="varietyForm.name" /></el-form-item>
        <el-form-item label="类别">
          <el-select v-model="varietyForm.category" style="width:100%">
            <el-option v-for="c in ['草本', '木本', '多肉', '蔬果']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="varietyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitVariety">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="开一批苗" width="500px">
      <el-form label-width="100px">
        <el-form-item label="品种">
          <el-select v-model="batchForm.varietyId" placeholder="只列在售品种" style="width:100%">
            <el-option
              v-for="v in sellingVarieties"
              :key="v.id"
              :label="`${v.name}（${v.code} · ${v.category}）`"
              :value="v.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="苗床">
          <el-select v-model="batchForm.seedbedId" placeholder="只列在用苗床" style="width:100%">
            <el-option
              v-for="b in usableBeds"
              :key="b.id"
              :label="`${b.name}（${b.code} · 可放 ${b.capacity} 株）`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="播种日期">
          <el-date-picker v-model="batchForm.sowDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="预计出圃">
          <el-date-picker v-model="batchForm.expectOutDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="计划株数">
          <el-input-number v-model="batchForm.planQty" :min="1" :step="50" />
        </el-form-item>
        <el-form-item label="负责师傅"><el-input v-model="batchForm.grower" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatch">开苗</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="readyVisible" title="转待出圃" width="420px">
      <el-form label-width="110px">
        <el-form-item label="实际成苗株数">
          <el-input-number v-model="readyQty" :min="1" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="readyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReady">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchApi, seedbedApi, varietyApi } from '../api'

const statuses = ['育苗中', '待出圃', '已出圃', '已报废']

const varieties = ref([])
const beds = ref([])
const batches = ref([])
const loading = ref(false)
const query = reactive({ status: '', varietyId: null, keyword: '' })

const varietyVisible = ref(false)
const varietyForm = reactive({ code: '', name: '', category: '草本' })
const batchVisible = ref(false)
const batchForm = reactive({
  varietyId: null, seedbedId: null, sowDate: '', expectOutDate: '', planQty: 100, grower: ''
})
const readyVisible = ref(false)
const readyQty = ref(100)
const readyId = ref(null)

const sellingVarieties = computed(() => varieties.value.filter((v) => v.status === '在售'))
const usableBeds = computed(() => beds.value.filter((b) => b.status === '在用'))

const varietyName = (id) => (id ? varieties.value.find((v) => v.id === id)?.name || `#${id}` : '未选')
const bedName = (id) => (id ? beds.value.find((b) => b.id === id)?.name || `#${id}` : '未安排')
const tagType = (s) =>
  s === '已出圃' ? 'success' : s === '待出圃' ? 'warning' : s === '已报废' ? 'info' : ''

const loadVarieties = async () => {
  varieties.value = await varietyApi.list()
}

const loadBatches = async () => {
  loading.value = true
  try {
    batches.value = await batchApi.list({
      status: query.status || undefined,
      varietyId: query.varietyId || undefined,
      keyword: query.keyword || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const openVariety = () => {
  Object.assign(varietyForm, { code: '', name: '', category: '草本' })
  varietyVisible.value = true
}

const submitVariety = async () => {
  try {
    await varietyApi.create({ ...varietyForm })
    ElMessage.success('品种已新增')
    varietyVisible.value = false
    await loadVarieties()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const toggleVariety = async (row) => {
  try {
    await varietyApi.setStatus(row.id, row.status === '在售' ? '停用' : '在售')
    ElMessage.success('已更新')
    await loadVarieties()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openBatch = () => {
  Object.assign(batchForm, {
    varietyId: null, seedbedId: null,
    sowDate: new Date().toISOString().slice(0, 10),
    expectOutDate: '', planQty: 100, grower: ''
  })
  batchVisible.value = true
}

const submitBatch = async () => {
  try {
    await batchApi.open({ ...batchForm, expectOutDate: batchForm.expectOutDate || null })
    ElMessage.success('这批苗已经排上苗床')
    batchVisible.value = false
    await loadBatches()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openReady = (row) => {
  readyId.value = row.id
  readyQty.value = row.planQty
  readyVisible.value = true
}

const submitReady = async () => {
  try {
    await batchApi.advance(readyId.value, 'ready', readyQty.value)
    ElMessage.success('已转待出圃')
    readyVisible.value = false
    await loadBatches()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const advance = async (row, action) => {
  try {
    await batchApi.advance(row.id, action, null)
    ElMessage.success('已更新')
    await loadBatches()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const scrap = async (row) => {
  try {
    await ElMessageBox.confirm(`确定把批次 ${row.batchNo} 报废？`, '提示')
  } catch {
    return
  }
  advance(row, 'scrap')
}

onMounted(async () => {
  try {
    const [v, b] = await Promise.all([varietyApi.list(), seedbedApi.list({})])
    varieties.value = v
    beds.value = b
  } catch (e) {
    ElMessage.error(e.message)
  }
  await loadBatches()
})
</script>
