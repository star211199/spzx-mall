<template>
  <div class="app-container">
    <el-tree :props="props" :load="loadNode" lazy />
  </div>
</template>

<script setup name="Region">
import { getTreeSelect } from "@/api/base/region";

const props = {
  label: 'name',
  isLeaf: 'leaf',
}

const parentCode = ref('0')

const loadNode = async (node, resolve) => {
  if (node.data && node.data.code) {
    parentCode.value = node.data.code
  }
  const { data } = await getTreeSelect(parentCode.value)
  ;(data || []).forEach(item => {
    item.leaf = item.level == 3
  })
  resolve(data)
}
</script>