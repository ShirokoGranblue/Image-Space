import { ElMessageBox } from 'element-plus'
import { getImageAlt } from './imageRequests'

export async function confirmImageDelete(image) {
  const imageName = getImageAlt(image)
  try {
    await ElMessageBox.confirm(
      `删除后无法恢复。确定删除《${imageName}》吗？`,
      '删除图片',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    return true
  } catch {
    return false
  }
}
