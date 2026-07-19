const CREATOR_TONES = ['#38d5ff', '#b7ff3c', '#f5b84b', '#9b8cff', '#ff6b57']

function hashString(value) {
  return String(value).split('').reduce((hash, char) => ((hash << 5) - hash) + char.charCodeAt(0), 0)
}

function creatorIdentity(image) {
  const userUuid = String(image?.userUuid || '').trim()
  if (userUuid) return `uuid:${userUuid}`

  if (image?.userId != null && String(image.userId).trim()) {
    return `id:${String(image.userId).trim()}`
  }

  const username = String(image?.username || '').trim().toLocaleLowerCase()
  return username ? `username:${username}` : ''
}

function interactionCount(value) {
  const count = Number(value)
  return Number.isFinite(count) ? count : 0
}

function uploadTimestamp(image) {
  const timestamp = Date.parse(image?.uploadTime || image?.createTime || '')
  return Number.isFinite(timestamp) ? timestamp : 0
}

export function collectPageTags(images = []) {
  const tags = new Set()
  for (const image of images) {
    String(image?.tags || '')
      .split('#')
      .map(tag => tag.trim())
      .filter(Boolean)
      .forEach(tag => tags.add(tag))
  }
  return Array.from(tags)
}

export function collectPageCreators(images = []) {
  const creators = new Map()
  for (const image of images) {
    const key = creatorIdentity(image)
    const uuidOrId = image?.userUuid || image?.userId
    if (!key || !uuidOrId) continue

    if (!creators.has(key)) {
      const username = image.username || 'unknown'
      const name = image.displayName || username || '?'
      const toneIndex = Math.abs(hashString(key)) % CREATOR_TONES.length
      creators.set(key, {
        key,
        username,
        displayName: image.displayName || image.username,
        uuidOrId,
        likeCount: 0,
        avatarColor: CREATOR_TONES[toneIndex],
        initial: String(name).charAt(0).toUpperCase()
      })
    }

    creators.get(key).likeCount += interactionCount(image.likeCount)
  }

  return Array.from(creators.values()).sort((left, right) =>
    right.likeCount - left.likeCount
      || String(left.displayName || left.username).localeCompare(String(right.displayName || right.username))
  )
}

export function selectFeaturedImage(images = []) {
  return [...images].sort((left, right) =>
    interactionCount(right?.likeCount) - interactionCount(left?.likeCount)
      || interactionCount(right?.commentCount) - interactionCount(left?.commentCount)
      || uploadTimestamp(right) - uploadTimestamp(left)
      || String(left?.uuid || left?.id || '').localeCompare(String(right?.uuid || right?.id || ''))
  )[0] || null
}
