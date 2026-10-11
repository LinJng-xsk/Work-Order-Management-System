<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="login-title">
          <el-icon :size="22"><Tools /></el-icon>
          <span>{{ mode === 'login' ? 'WOMS 工单管理系统' : '注册账号' }}</span>
        </div>
      </template>

      <!-- ★ 登录表单 -->
      <el-form v-if="mode === 'login'" ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item prop="code">
          <div class="captcha-row">
            <el-input v-model="form.code" placeholder="验证码" :prefix-icon="Key" style="flex: 1" />
            <img
              v-if="captchaImg"
              :src="captchaImg"
              class="captcha-img"
              alt="验证码"
              title="点击刷新"
              @click="loadCaptcha"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- ★ 注册表单 -->
      <el-form v-else ref="registerRef" :model="regForm" :rules="registerRules" size="large" @keyup.enter="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="regForm.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="regForm.nickname" placeholder="昵称" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="regForm.phone" placeholder="手机号" :prefix-icon="Iphone" maxlength="11" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="regForm.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="regForm.confirmPassword" type="password" placeholder="确认密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="role">
          <el-radio-group v-model="regForm.role" class="role-group" @change="handleRoleChange">
            <el-radio value="employee" border>用户</el-radio>
            <el-radio value="operator" border>运维人员</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- ★ 工作日：仅运维人员显示 -->
        <el-form-item v-if="regForm.role === 'operator'" prop="workDays" label="工作日">
          <el-checkbox-group v-model="regForm.workDays" class="wd-group">
            <el-checkbox v-for="(d, i) in WEEK_LABELS" :key="i" :value="String(i + 1)">{{ d }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="registerLoading" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <!-- ★ 登录 / 注册 切换 -->
      <div class="switch-link">
        <span v-if="mode === 'login'">
          还没有账号？<el-link type="primary" :underline="'never'" @click="switchMode">立即注册</el-link>
        </span>
        <span v-else>
          已有账号？<el-link type="primary" :underline="'never'" @click="switchMode">去登录</el-link>
        </span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, Iphone } from '@element-plus/icons-vue'
import { getCaptcha, login, register } from '../api/auth'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const captchaImg = ref('')

// ★ 登录 / 注册 模式
const mode = ref('login')
const registerRef = ref()
const registerLoading = ref(false)

const form = reactive({
  username: '',
  password: '',
  uuid: '',
  code: ''
})

// ★ 注册表单
const WEEK_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const regForm = reactive({
  username: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: '',
  role: 'employee',
  workDays: []
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// ★ 注册校验
const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== regForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  workDays: [
    {
      validator: (rule, value, callback) => {
        if (regForm.role === 'operator' && (!value || value.length === 0)) {
          callback(new Error('请选择工作日'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// ★ 切换角色：从运维切回用户时清空工作日
const handleRoleChange = () => {
  if (regForm.role !== 'operator') {
    regForm.workDays = []
  }
}

// 获取验证码：uuid + base64 图片
const loadCaptcha = async () => {
  try {
    const data = await getCaptcha()
    form.uuid = data.uuid
    if (data.captchaCode) {
      form.code = data.captchaCode
    }
    captchaImg.value = data.img.startsWith('data:')
      ? data.img
      : `data:image/png;base64,${data.img}`
  } catch (e) {
    // 错误已在拦截器提示
  }
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login({
      username: form.username,
      password: form.password,
      uuid: form.uuid,
      code: form.code
    })
    userStore.setLogin(data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    form.code = ''
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

// ★ 切换模式
const switchMode = () => {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  if (mode.value === 'login') {
    loadCaptcha()
  }
}

// ★ 注册
const handleRegister = async () => {
  await registerRef.value.validate()
  registerLoading.value = true
  try {
    await register({
      username: regForm.username,
      nickname: regForm.nickname,
      phone: regForm.phone,
      password: regForm.password,
      role: regForm.role,
      workDays: regForm.role === 'operator' ? [...regForm.workDays].sort().join(',') : null
    })
    ElMessage.success('注册成功，请登录')
    form.username = regForm.username
    mode.value = 'login'
    loadCaptcha()
  } finally {
    registerLoading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d2939 0%, #334155 100%);
}
.login-card {
  width: 420px;
}
.login-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}
.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.captcha-img {
  height: 40px;
  width: 120px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  cursor: pointer;
  background: #f9fafb;
}
.login-btn {
  width: 100%;
}
.switch-link {
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
  color: #6b7280;
}
.role-group {
  display: flex;
  width: 100%;
}
.role-group .el-radio {
  flex: 1;
  justify-content: center;
  margin-right: 0;
}
/* ★ 工作日多选 */
.wd-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.wd-group .el-checkbox {
  margin-right: 0;
  width: calc(100% / 4 - 4px);
}
</style>
