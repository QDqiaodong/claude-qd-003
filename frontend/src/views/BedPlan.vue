<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-select v-model="greenhouseId" placeholder="全部温室" clearable style="width:180px" @change="() => {}">
          <el-option v-for="h in houses" :key="h.id" :label="h.name" :value="h.id" />
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
        <span style="margin-left:auto;color:#909399">
          在用苗床 {{ visibleBeds.length }} 张 · 占着的批次 {{ holdingCount }} 批
        </span>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>苗床占用</template>
      <el-table :data="bedRows" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="苗床" width="100" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="所在温室" width="170">
          <template #default="{ row }">
            {{ houseName(row.greenhouseId) }}
            <el-tag v-if="row.houseStatus !== '在用'" size="small" type="info">温室{{ row.houseStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="苗床状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : row.status === '维修' ? 'warning' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="capacity" label="可放株数" width="100" />
        <el-table-column label="占用情况" min-width="360">
          <template #default="{ row }">
            <div v-if="row.holding.length">
              <div v-for="b in row.holding" :key="b.id" style="line-height:22px">
                <el-tag size="small" :type="b.status === '待出圃' ? 'warning' : 'danger'">{{ b.status }}</el-tag>
                {{ b.batchNo }} · {{ varietyName(b.varietyId) }} ·
                {{ b.sowDate }} ~ {{ b.expectOutDate || '未定' }} ·
                计划 {{ b.planQty }} 株
              </div>
            </div>
            <span v-else style="color:#67c23a">当前空着</span>
            <div v-if="row.used.length" style="color:#909399;line-height:20px">
              已了结：{{ row.used.map(u => `${u.batchNo}(${u.status})`).join('、') }}
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { batchApi, greenhouseApi, seedbedApi, varietyApi } from '../api'

const HOLDING = ['育苗中', '待出圃']

const houses = ref([])
const beds = ref([])
const batches = ref([])
const varieties = ref([])
const greenhouseId = ref(null)
const loading = ref(false)

const visibleBeds = computed(() =>
  beds.value.filter((b) => !greenhouseId.value || b.greenhouseId === greenhouseId.value)
)
const holdingCount = computed(() => batches.value.filter((b) => HOLDING.includes(b.status)).length)

const houseName = (id) => (id ? houses.value.find((h) => h.id === id)?.name || '未归棚' : '未归棚')
const varietyName = (id) => (id ? varieties.value.find((v) => v.id === id)?.name || `#${id}` : '—')

const bedRows = computed(() =>
  visibleBeds.value.map((b) => {
    const mine = batches.value.filter((x) => x.seedbedId === b.id)
    const house = houses.value.find((h) => h.id === b.greenhouseId)
    return {
      ...b,
      houseStatus: house ? house.status : '未归棚',
      holding: mine.filter((x) => HOLDING.includes(x.status)),
      used: mine.filter((x) => !HOLDING.includes(x.status))
    }
  })
)

const load = async () => {
  loading.value = true
  try {
    batches.value = await batchApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const [h, b, v] = await Promise.all([greenhouseApi.list(), seedbedApi.list({}), varietyApi.list()])
    houses.value = h
    beds.value = b
    varieties.value = v
  } catch (e) {
    ElMessage.error(e.message)
  }
  await load()
})
</script>
