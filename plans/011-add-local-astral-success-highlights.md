# 011 — Add local Astral success highlights

- **Status**: TODO
- **Commit**: f8dcedf7
- **Severity**: LOW
- **Category**: Missed opportunity / Purpose & frequency
- **Estimated scope**: 10 files, about 180 lines

## Problem

Rare positive actions currently resolve through ordinary state replacement.
Upload completion immediately closes the dialog:

```js
// frontend/src/components/ImageUpload.vue:251-257 — current
uploading.value = false
if (errors.length) {
  ElMessage.error(errors.join('; '))
}
ElMessage.success(`已上传 ${success} / ${fileList.value.length} 张图片`)
visible.value = false
emit('uploaded', uploadedImages)
```

Home already retains the uploaded image records but does not use them for
visual confirmation:

```js
// frontend/src/views/Home.vue:383-388 — current
async function handleUploaded(uploadedImages = []) {
  if (Array.isArray(uploadedImages) && uploadedImages.length > 0) {
    recentUploadResources.value = uploadedImages.filter(img => img?.uuid)
  }
  await Promise.all([fetchCategories(), fetchList()])
}
```

Comment creation refreshes the entire list without identifying the new item:

```js
// frontend/src/views/ImageDetail.vue:325-330 — current
await addComment({ imageId: image.value.id, content: text || '📷', imagePath })
ElMessage.success('评论成功')
commentText.value = ''
cmtFile.value = null
const res = await getComments(image.value.uuid)
await replaceComments(res.data || [])
```

Image like success only updates count and boolean:

```js
// frontend/src/views/ImageDetail.vue:343-347 — current
const res = image.value.likedByMe
  ? await unlikeImage(image.value.uuid)
  : await likeImage(image.value.uuid)
image.value.likeCount = res.data.likeCount
image.value.likedByMe = res.data.likedByMe
```

These are suitable rare delight moments, but success must remain local. A
global particle burst would compete with the image and celebrate destructive
or reverse operations.

## Target

Create one global, presentation-only class using a pseudo-element. It animates
only `opacity` and `transform`:

```css
/* frontend/src/style.css — target */
@keyframes astral-success-glint {
  0% {
    opacity: 0;
    transform: scale(0.96);
  }
  45% {
    opacity: 0.82;
    transform: scale(1);
  }
  100% {
    opacity: 0;
    transform: scale(1.04);
  }
}

@keyframes astral-success-fade {
  0% { opacity: 0; }
  45% { opacity: 0.72; }
  100% { opacity: 0; }
}

.motion-celebrate {
  position: relative;
  isolation: isolate;
}

.motion-celebrate::after {
  content: '';
  position: absolute;
  z-index: 2;
  inset: -2px;
  border: 1px solid rgba(185, 163, 111, 0.72);
  border-radius: inherit;
  background:
    radial-gradient(circle at 18% 22%, rgba(244, 241, 237, 0.72) 0 1px, transparent 2px),
    radial-gradient(circle at 82% 30%, rgba(93, 143, 139, 0.68) 0 1px, transparent 2px),
    radial-gradient(circle at 70% 82%, rgba(167, 123, 131, 0.64) 0 1px, transparent 2px);
  pointer-events: none;
  animation: astral-success-glint var(--duration-overlay) var(--ease-out) both;
}

@media (prefers-reduced-motion: reduce) {
  .motion-celebrate::after {
    transform: none;
    animation: astral-success-fade 200ms ease both;
  }
}
```

Trigger the class only for:

- newly uploaded cards after the refreshed Home list contains their UUIDs,
- a newly created comment after the refreshed comment list identifies it,
- a successful transition from not-liked to liked.

Never trigger it for:

- initial render of an already-liked image,
- unlike,
- delete, batch delete, account deletion, or error,
- loading or polling updates,
- every existing card/comment on refresh.

Clear each trigger from the CSS `animationend` event, not a timeout.

## Repo conventions to follow

- Astral colors already live in `frontend/src/styles/tokens.css`; the target
  uses existing gold, teal, rose, and starlight values.
- Global third-party integration and utility selectors live in
  `frontend/src/style.css`.
- `recentUploadResources` already owns uploaded UUIDs; do not add another
  upload-tracking request.
- `ImageCard.vue` is the compatibility wrapper around `GalleryItem.vue`; pass
  highlight state explicitly through both components rather than relying on
  recursive attribute fallthrough.
- Parent views own asynchronous success state. Presentational children only
  receive a boolean/id and emit `celebration-end`.

## Steps

1. Execute plans 001, 004, 008, and 010 first.
2. Add the exact global keyframes and `.motion-celebrate` rules to
   `frontend/src/style.css`.
3. Add a `celebrated` Boolean prop and `celebration-end` emit to
   `frontend/src/components/gallery/GalleryItem.vue`. Apply
   `motion-celebrate` to the root only when the prop is true. On
   `animationend`, emit only when `event.animationName` is
   `astral-success-glint` or `astral-success-fade`.
4. Pass the prop and event explicitly through
   `frontend/src/components/ImageCard.vue`.
5. In `frontend/src/views/Home.vue`, maintain a `Set` of celebrated upload
   UUIDs derived from the `uploadedImages` argument. After `fetchList`
   completes, retain only UUIDs that are actually present in
   `displayedImages`. Bind `celebrated` on each `ImageCard`; remove that UUID
   on `celebration-end`. Do not alter `recentUploadResources` polling.
6. Add a `celebrateLike` Boolean prop and `celebration-end` emit to
   `frontend/src/components/detail/ImageDetailInfoPanel.vue`. Apply the class
   to the like button only while true.
7. In `frontend/src/views/ImageDetail.vue`, record the pre-request
   `likedByMe` value. Set a local `celebrateLike` ref only when a successful
   response changes `false → true`; clear it from the child event. Do not
   trigger on `true → false` or initial load.
8. Add a `celebratedCommentId` prop and `celebration-end` emit to
   `frontend/src/components/detail/ImageCommentsSection.vue`. Apply the class
   only to the matching comment article.
9. Before adding a comment, capture the existing comment IDs. Keep the
   `addComment` response, refresh the list as today, then choose the new ID
   from `response.data.id` when available or the first refreshed ID absent
   from the captured set. Set `celebratedCommentId`; clear it from
   `animationend`.
10. Add/update tests:
    - `frontend/src/__tests__/ImageCard.test.ts`: only a celebrated card gets
      the class and emits the end event.
    - `frontend/src/__tests__/ImageDetail.test.ts`: like glint occurs only on
      successful like, not unlike/initial state; only the new comment is
      highlighted.
    - `frontend/src/__tests__/StageSixFoundation.test.ts`: the keyframes use
      only opacity/transform, duration is `--duration-overlay`, and the reduced
      variant is fade-only.

## Boundaries

- Do NOT delay closing the upload dialog or delay API completion.
- Do NOT use `setTimeout`, canvas particles, audio, vibration, confetti, blur,
  box-shadow animation, or a new dependency.
- Do NOT celebrate destructive operations, unlike, failed operations, initial
  data load, or polling refresh.
- Do NOT cover the viewport; the pseudo-element stays within two pixels of the
  successful control/card/comment.
- Do NOT change API requests, payloads, backend responses, or persisted state.
- Do NOT let the decorative pseudo-element receive pointer events.
- Do NOT exceed the existing `240ms` overlay duration.

## Verification

- **Mechanical**:
  - From `frontend/`, run
    `npm.cmd test -- src/__tests__/ImageCard.test.ts src/__tests__/ImageDetail.test.ts src/__tests__/StageSixFoundation.test.ts`.
  - Run `npm.cmd run check`.
  - Expected result: all commands exit 0; source contains no timer or added
    animation dependency.
- **Feel check**:
  - Upload one image. The dialog should close on the existing schedule; only
    the returned card should receive one local gold/teal/rose glint.
  - Like an unliked image. The like control should glint once. Unlike it and
    confirm no celebration occurs.
  - Add a comment. Only the new comment should glint after the refreshed list
    arrives; existing comments must remain still.
  - Trigger each action repeatedly after its previous animation ends and
    confirm the animation can replay without stale classes.
  - At 10% speed, verify only opacity and transform animate and the effect
    stays local.
  - Under reduced motion, verify each success uses a local 200ms fade with no
    scale.
- **Done when**: upload, positive like, and new comment success each receive
  one local, non-blocking Astral highlight; reverse/destructive/initial states
  never celebrate; all checks pass.
