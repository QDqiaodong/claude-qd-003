import { createRouter, createWebHistory } from 'vue-router'
import Greenhouse from '../views/Greenhouse.vue'
import Batches from '../views/Batches.vue'
import BedPlan from '../views/BedPlan.vue'
import Transfers from '../views/Transfers.vue'
import Shipments from '../views/Shipments.vue'

const routes = [
  { path: '/', redirect: '/greenhouses' },
  { path: '/greenhouses', component: Greenhouse, meta: { title: '温室与苗床' } },
  { path: '/batches', component: Batches, meta: { title: '品种与育苗批次' } },
  { path: '/beds', component: BedPlan, meta: { title: '苗床占用' } },
  { path: '/transfers', component: Transfers, meta: { title: '转棚调拨' } },
  { path: '/shipments', component: Shipments, meta: { title: '出圃发货' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
