<template>
  <section class="profile-header">
    <div class="avatar-column">
      <div class="avatar-wrap">
        <el-avatar :size="122" :src="avatarUrl" class="avatar"><el-icon :size="48"><UserFilled /></el-icon></el-avatar>
        <button v-if="owner" class="avatar-upload" :class="{ 'avatar-upload--active': editing }" type="button" aria-label="编辑头像" @click="emit('edit-avatar')"><el-icon><Camera /></el-icon></button>
      </div>
      <div v-if="!editing" class="profile-stats">
        <div v-for="item in stats" :key="item.label" class="stat-item"><strong>{{ item.value }}</strong><span>{{ item.label }}</span></div>
      </div>
    </div>

    <div class="profile-main">
      <template v-if="!editing">
        <div class="profile-name-row">
          <div><span class="section-label">个人主页</span><h1>{{ user.displayName || user.username }}</h1></div>
          <el-dropdown v-if="owner" trigger="click"><button class="dropdown-trigger" type="button" aria-label="更多操作"><el-icon><MoreFilled /></el-icon></button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="emit('edit-profile')">编辑资料</el-dropdown-item><el-dropdown-item @click="emit('delete-account')">注销账号</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
        </div>
        <div v-if="showMeta" class="profile-meta">
          <p v-if="user.bio" class="bio">{{ user.bio }}</p>
          <div v-if="owner && (user.email || user.phone)" class="contact"><span v-if="user.email"><el-icon><Message /></el-icon>{{ user.email }}</span><span v-if="user.phone"><el-icon><Phone /></el-icon>{{ user.phone }}</span></div>
          <span v-if="joinedAt" class="joined"><el-icon><Calendar /></el-icon>{{ joinedAt }}</span>
        </div>
      </template>

      <div v-else class="profile-edit">
        <span class="section-label">编辑资料</span>
        <el-form label-position="top">
          <div class="form-columns">
            <el-form-item label="展示名称"><el-input :model-value="form.displayName" maxlength="50" @update:model-value="updateField('displayName',$event)" /></el-form-item>
            <el-form-item label="邮箱" :error="fieldErrors.email"><el-input :model-value="form.email" @update:model-value="updateField('email',$event)" @blur="emit('email-blur')" /></el-form-item>
            <el-form-item v-if="emailChanged" label="邮箱验证码"><div class="email-code-row"><el-input :model-value="form.emailCode" maxlength="6" placeholder="输入新邮箱收到的验证码" @update:model-value="updateField('emailCode',$event)" /><el-button :loading="sendingEmailCode" :disabled="emailCodeCountdown > 0 || !!fieldErrors.email" @click="emit('send-email-code')">{{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s` : '发送验证码' }}</el-button></div></el-form-item>
          </div>
          <el-form-item label="手机号" :error="fieldErrors.phone"><el-input :model-value="form.phone" maxlength="20" @update:model-value="updateField('phone',$event)" @blur="emit('phone-blur')" /></el-form-item>
          <el-form-item label="个人介绍"><el-input :model-value="form.bio" type="textarea" :rows="3" maxlength="200" show-word-limit @update:model-value="updateField('bio',$event)" /></el-form-item>
          <div class="edit-actions"><el-button type="primary" :loading="saving" @click="emit('save')">保存</el-button><el-button @click="emit('cancel')">取消</el-button></div>
        </el-form>
      </div>
    </div>
  </section>
</template>

<script setup>
import { Calendar, Camera, Message, MoreFilled, Phone, UserFilled } from '@element-plus/icons-vue'

const props = defineProps({
  user: { type: Object, required: true }, avatarUrl: { type: String, default: '' }, owner: { type: Boolean, default: false }, editing: { type: Boolean, default: false },
  stats: { type: Array, default: () => [] }, showMeta: { type: Boolean, default: false }, joinedAt: { type: String, default: '' }, form: { type: Object, required: true },
  fieldErrors: { type: Object, default: () => ({}) }, emailChanged: { type: Boolean, default: false }, sendingEmailCode: { type: Boolean, default: false }, emailCodeCountdown: { type: Number, default: 0 }, saving: { type: Boolean, default: false },
})
const emit = defineEmits(['edit-avatar','edit-profile','delete-account','update-form-field','email-blur','phone-blur','send-email-code','save','cancel'])
function updateField(field, value) { if (props.form[field] !== value) emit('update-form-field', { field, value }) }
</script>

<style scoped>
.profile-header{display:grid;grid-template-columns:240px minmax(0,1fr);gap:var(--space-6);padding:var(--space-6);border:1px solid var(--color-border-subtle);background:var(--color-surface-1)}
.avatar-column{display:flex;flex-direction:column;align-items:center;gap:var(--space-4)}.avatar-wrap{position:relative}.avatar{border:4px solid var(--color-surface-1);box-shadow:var(--shadow-float)}.avatar-upload{position:absolute;right:0;bottom:4px;display:grid;width:44px;height:44px;place-items:center;border:1px solid var(--color-border-strong);border-radius:50%;background:var(--color-surface-1);color:var(--color-text-primary);cursor:pointer;opacity:.64;transition:opacity var(--duration-fast) var(--ease-standard)}.avatar-upload--active{opacity:1}
.profile-stats{display:grid;width:100%;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-4)}.stat-item{display:flex;flex-direction:column;gap:var(--space-1);padding:var(--space-2) 0}.stat-item strong{font-family:var(--font-title);font-size:var(--text-xl)}.stat-item span{color:var(--color-text-muted);font-size:var(--text-xs)}
.profile-main{min-width:0}.profile-name-row{display:flex;align-items:flex-start;justify-content:space-between;gap:var(--space-4)}.section-label{color:var(--color-vermilion);font-size:var(--text-xs);font-weight:700;letter-spacing:.08em}h1{margin:var(--space-2) 0;font-family:var(--font-title);font-size:clamp(36px,5vw,64px);font-weight:400;line-height:1.04;overflow-wrap:anywhere}.dropdown-trigger{display:grid;width:44px;height:44px;place-items:center;border:1px solid var(--color-border-subtle);border-radius:var(--radius-sm);background:transparent;color:var(--color-text-primary);cursor:pointer}.profile-meta{padding-top:var(--space-4)}.bio{max-width:720px;color:var(--color-text-secondary);line-height:var(--leading-md);overflow-wrap:anywhere}.contact{display:flex;flex-wrap:wrap;gap:var(--space-4);color:var(--color-text-secondary)}.contact span,.joined{display:inline-flex;align-items:center;gap:var(--space-1)}.joined{margin-top:var(--space-3);color:var(--color-text-muted);font-size:var(--text-sm)}
.profile-edit{max-width:760px}.form-columns{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--space-3)}.email-code-row{display:flex;width:100%;gap:var(--space-2)}.edit-actions{display:flex;gap:var(--space-2)}
@media(max-width:820px){.profile-header{grid-template-columns:1fr}.avatar-column{align-items:flex-start}.profile-stats{max-width:360px}}
@media(max-width:560px){.profile-header{padding:var(--space-4)}.form-columns{grid-template-columns:1fr}.email-code-row{flex-direction:column}.edit-actions :deep(.el-button){min-height:44px}}
</style>
