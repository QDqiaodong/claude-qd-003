<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px">
          <span>温室</span>
          <el-button size="small" type="success" @click="openHouse">新增温室</el-button>
          <span style="margin-left:auto;color:#909399">
            在用 {{ houses.filter(h => h.status === '在用').length }} 座 / 共 {{ houses.length }} 座
          </span>
        </div>
      </template>
      <el-table :data="houses" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="温室编号" width="120" />
        <el-table-column prop="name" label="温室名称" min-width="160" />
        <el-table-column prop="kind" label="类型" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="名下苗床" width="110">
          <template #default="{ row }">
            {{ beds.filter(b => b.greenhouseId === row.id).length }} 张
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link :type="row.status === '在用' ? 'danger' : 'primary'" @click="toggleHouse(row)">
              {{ row.status === '在用' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
          <span>苗床台账</span>
          <el-select v-model="query.greenhouseId" placeholder="按温室" clearable size="small" style="width:170px">
            <el-option v-for="h in houses" :key="h.id" :label="h.name" :value="h.id" />
          </el-select>
          <el-select v-model="query.status" placeholder="按状态" clearable size="small" style="width:130px">
            <el-option label="在用" value="在用" />
            <el-option label="空置" value="空置" />
            <el-option label="维修" value="维修" />
          </el-select>
          <el-input v-model="query.keyword" placeholder="编号或名称" clearable size="small" style="width:170px" />
          <el-button size="small" type="primary" @click="loadBeds">查询</el-button>
          <el-button size="small" type="success" @click="openBed()">新增苗床</el-button>
        </div>
      </template>
      <el-table :data="beds" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="苗床编号" width="120" />
        <el-table-column prop="name" label="苗床名称" min-width="150" />
        <el-table-column label="所在温室" width="160">
          <template #default="{ row }">{{ houseName(row.greenhouseId) }}</template>
        </el-table-column>
        <el-table-column prop="capacity" label="可放株数" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : row.status === '维修' ? 'warning' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openBed(row)">调整</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="houseVisible" title="新增温室" width="440px">
      <el-form label-width="90px">
        <el-form-item label="温室编号"><el-input v-model="houseForm.code" placeholder="如 GH-05" /></el-form-item>
        <el-form-item label="温室名称"><el-input v-model="houseForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="houseForm.kind" style="width:100%">
            <el-option v-for="k in ['育苗棚', '成苗棚', '炼苗棚']" :key="k" :label="k" :value="k" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="houseVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHouse">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bedVisible" :title="bedForm.id ? '调整苗床' : '新增苗床'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="苗床编号">
          <el-input v-model="bedForm.code" :disabled="!!bedForm.id" placeholder="如 SB-103" />
        </el-form-item>
        <el-form-item label="苗床名称"><el-input v-model="bedForm.name" /></el-form-item>
        <el-form-item label="所在温室">
          <el-select v-model="bedForm.greenhouseId" clearable placeholder="可先不归" style="width:100%">
            <el-option v-for="h in houses" :key="h.id" :label="h.name" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="可放株数">
          <el-input-number v-model="bedForm.capacity" :min="0" :step="50" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="bedForm.status" style="width:100%">
            <el-option label="在用" value="在用" />
            <el-option label="空置" value="空置" />
            <el-option label="维修" value="维修" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bedVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBed">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { greenhouseApi, seedbedApi } from '../api'

const houses = ref([])
const beds = ref([])
const loading = ref(false)
const query = reactive({ greenhouseId: null, status: '', keyword: '' })

const houseVisible = ref(false)
const houseForm = reactive({ code: '', name: '', kind: '育苗棚' })
const bedVisible = ref(false)
const bedForm = reactive({ id: null, code: '', name: '', greenhouseId: null, capacity: 0, status: '在用' })

const houseName = (id) => (id ? houses.value.find((h) => h.id === id)?.name || '未归棚' : '未归棚')

const loadHouses = async () => {
  houses.value = await greenhouseApi.list()
}

const loadBeds = async () => {
  loading.value = true
  try {
    beds.value = await seedbedApi.list({
      greenhouseId: query.greenhouseId || undefined,
      status: query.status || undefined,
      keyword: query.keyword || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const openHouse = () => {
  Object.assign(houseForm, { code: '', name: '', kind: '育苗棚' })
  houseVisible.value = true
}

const submitHouse = async () => {
  try {
    await greenhouseApi.create({ ...houseForm })
    ElMessage.success('温室已新增')
    houseVisible.value = false
    await loadHouses()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const toggleHouse = async (row) => {
  try {
    await greenhouseApi.setStatus(row.id, row.status === '在用' ? '停用' : '在用')
    ElMessage.success('已更新')
    await loadHouses()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openBed = (row) => {
  if (row) {
    Object.assign(bedForm, row)
  } else {
    Object.assign(bedForm, { id: null, code: '', name: '', greenhouseId: null, capacity: 0, status: '在用' })
  }
  bedVisible.value = true
}

const submitBed = async () => {
  try {
    if (bedForm.id) {
      await seedbedApi.update(bedForm.id, {
        name: bedForm.name,
        greenhouseId: bedForm.greenhouseId,
        capacity: bedForm.capacity,
        status: bedForm.status
      })
    } else {
      await seedbedApi.create({ ...bedForm })
    }
    ElMessage.success('已保存')
    bedVisible.value = false
    await loadBeds()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    await loadHouses()
  } catch (e) {
    ElMessage.error(e.message)
  }
  await loadBeds()
})
</script>
