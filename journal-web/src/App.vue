<template>
  <div class="common-layout">
    <el-container>
      <el-container>
        <el-aside width="300px">
            <el-form label-width="auto" style="height: 15vh; padding: 10px 15px;">
                <el-form-item label="键">
                    <el-select v-model="inputKey" allow-create filterable default-first-option>
                        <el-option
                            v-for="item in inputKeys"
                            :key="item"
                            :label="item"
                            :value="item"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="值">
                    <el-select v-model="inputSubKey" allow-create filterable default-first-option>
                        <el-option
                            v-for="item in inputSubKeys"
                            :key="item"
                            :label="item"
                            :value="item"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="inputRemark" type="textarea"/>
                </el-form-item>
                <el-form-item>
                    <el-button @click="refresh">刷新</el-button>
                    <el-button @click="submit" type="primary">提交</el-button>
                </el-form-item>
            </el-form>
            <el-timeline style="height: calc(85vh - 45px); overflow-y: scroll; padding-top: 15px;">
                <el-timeline-item
                    v-for="(item, index) in timeline"
                    :key="index"
                    :timestamp="item.time">
                    <p>{{ item.key }} {{ item.subKey !== '-' ? (': ' + item.subKey) : '' }}</p>
                    <p style="color: #888; font-size: 10px; word-break: break-all">{{ item.remark }}</p>
                </el-timeline-item>
            </el-timeline>
        </el-aside>
        <el-main style="height: calc(100vh - 40px);">
            <el-table :data="tableData" style="width: 100%" row-key="name" >
                <el-table-column label="名称" prop="name" width="180" />
                <el-table-column label="次数" prop="count" width="80" />
                <el-table-column label="上次时间" prop="lastTime" width="180" />
                <el-table-column label="间隔(全部)" prop="rate" width="140" />
                <el-table-column label="间隔(15次)" prop="rate15" width="140" />
                <el-table-column label="间隔(5次)" prop="rate5" width="140" />
                <el-table-column label="预计下次时间" prop="nextTime" width="180" />
                <el-table-column label="预计下次时间" width="80">
                    <template #default="{row}">
                        <span v-if="!row.nextDuration"></span>
                        <span v-else-if="row.nextDuration.startsWith('-')" style="color: red">{{ row.nextDuration ?? ' ' }}</span>
                        <span v-else style="color: green">{{ row.nextDuration ?? ' ' }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作">
                    <template #default="{row}">
                        <el-button @click="toInput(row.name, row.parent)">添加</el-button>
                        <el-button @click="getTimeline(row.name, row.parent)">查看</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-main>
        <el-aside width="300px">
            <el-timeline style="height: 100vh; overflow-y: scroll; padding-top: 15px;">
                <el-timeline-item
                    v-for="(item, index) in keyTimeline"
                    :key="index"
                    :timestamp="item.time">
                    <p>{{ item.key }} {{ item.subKey ? (': ' + item.subKey) : '' }}</p>
                    <p style="color: #888; font-size: 10px; word-break: break-all">{{ item.remark }}</p>
                </el-timeline-item>
            </el-timeline>
        </el-aside>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import axios from 'axios';
import { onMounted, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const tableData = ref([])
const timeline = ref([])
const inputKey = ref('')
const inputSubKey = ref('-')
const inputRemark = ref('')
const inputKeys = computed(() => {
    return tableData.value.map(item => item.name)
})
const inputSubKeys = computed(() => {
    return ['-', ...tableData.value.filter(item => item.name===inputKey.value)[0].children.map(item => item.name).filter(name => name !== '-')]
})
const keyTimeline = ref([])

const update = data => {
    const formatRate = rate => rate !== 0 ? (`${Math.floor(rate/24)}d` + (rate % 24) + 'H') : '-'
    if (data.data.success) {
        timeline.value = data.data.timeline
        tableData.value = data.data.statistics.map(data => {
            return {
                ...data,
                rate: formatRate(data.rate),
                rate5: formatRate(data.rate5),
                rate15: formatRate(data.rate15)
            }
        })
        ElMessage('refresh complate')
    } else {
        ElMessage.error(data.data.msg)
    }
}

const refresh = () => {
    ElMessage('refresh...')
    axios
        .get('/get')
        .then(update)
        .catch(e => {
            console.log(e)
            ElMessage.error(e)
        })
}

const submit = () => {
    axios
        .post('/submit', {
            key: inputKey.value,
            subKey: inputSubKey.value,
            remark: inputRemark.value
        })
        .then(data => {
            update(data)
            inputKey.value = ''
            inputSubKey.value = '-'
            inputRemark.value = ''
        })
        .catch(e => {
            console.log(e)
            ElMessage.error(e)
        })
}

const toInput = (name, parent) => {
    if (parent) {
        inputKey.value = parent
        inputSubKey.value = name
    } else {
        inputKey.value = name
        inputSubKey.value = '-'
    }
}

const getTimeline = (name, parent) => {
    axios
        .get(parent? `/getLine?key=${parent}&subKey=${name}`:`/getLine?key=${name}`)
        .then(data => {
            keyTimeline.value = data.data
        })
}

onMounted(refresh)
</script>

<style lang="css" scoped>

</style>