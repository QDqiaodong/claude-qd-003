<template>
  <el-container style="min-height: 100vh">
    <el-header style="display:flex;align-items:center;gap:28px;background:#f5f7fa">
      <strong style="font-size:16px">花卉苗圃 · 温室育苗与出圃管理</strong>
      <el-menu mode="horizontal" :default-active="route.path" router style="flex:1;border:none;background:transparent">
        <el-menu-item index="/greenhouses">温室与苗床</el-menu-item>
        <el-menu-item index="/batches">品种与育苗批次</el-menu-item>
        <el-menu-item index="/beds">苗床占用</el-menu-item>
        <el-menu-item index="/shipments">出圃发货</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
