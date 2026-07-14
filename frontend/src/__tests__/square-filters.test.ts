import { describe, expect, it } from 'vitest'

import {
  buildSquareParams,
  loadSquareSession,
  saveSquareSession,
} from '../utils/squareFilters'

function createStorage(): Storage {
  const values = new Map<string, string>()
  return {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => { values.set(key, value) },
    removeItem: (key: string) => { values.delete(key) },
    clear: () => { values.clear() },
    key: (index: number) => [...values.keys()][index] ?? null,
    get length() { return values.size },
  }
}

describe('square filters', () => {
  it('passes a real category id to the existing square query', () => {
    const params = buildSquareParams({ page: 1, limit: 50, keyword: '', categoryId: 7, tags: [], sortField: '', randomSeed: 'seed-category' })
    expect(params.categoryId).toBe(7)
  })

  it('uses hidden random mode when no visible sort is selected', () => {
    const params = buildSquareParams({
      page: 2,
      limit: 12,
      keyword: '  1  ',
      tags: ['cat', 'blue'],
      sortField: '',
      randomSeed: 'seed-1',
    })

    expect(params).toEqual({
      page: 2,
      limit: 50,
      keyword: '1',
      categoryId: undefined,
      tags: 'cat#blue',
      sortMode: 'random',
      sortField: 'upload_time',
      sortOrder: 'desc',
      randomSeed: 'seed-1',
    })
  })

  it('uses explicit time or size sorting when selected', () => {
    const params = buildSquareParams({
      page: 1,
      limit: 50,
      keyword: '',
      categoryId: undefined,
      tags: [],
      sortField: 'file_size',
      randomSeed: 'seed-2',
    })

    expect(params).toEqual({
      page: 1,
      limit: 50,
      keyword: '',
      categoryId: undefined,
      tags: '',
      sortMode: 'latest',
      sortField: 'file_size',
      sortOrder: 'desc',
      randomSeed: undefined,
    })
  })

  it('uses ascending name sorting when selected', () => {
    const params = buildSquareParams({
      page: 1,
      limit: 50,
      keyword: '',
      tags: [],
      sortField: 'image_name',
      randomSeed: 'seed-3',
    })

    expect(params).toEqual({
      page: 1,
      limit: 50,
      keyword: '',
      categoryId: undefined,
      tags: '',
      sortMode: 'latest',
      sortField: 'image_name',
      sortOrder: 'asc',
      randomSeed: undefined,
    })
  })

  it('keeps the current session filters without reusing the old random seed', () => {
    const storage = createStorage()

    saveSquareSession({
      page: 3,
      limit: 100,
      keyword: 'exact-name.jpg',
      categoryId: 7,
      tags: ['tag-a'],
      sortField: '',
      randomSeed: 'old-seed',
    }, storage)

    expect(loadSquareSession(storage)).toEqual({
      page: 3,
      limit: 100,
      keyword: 'exact-name.jpg',
      categoryId: 7,
      tags: ['tag-a'],
      sortField: '',
    })
  })

  it('does not keep explicit sorting after browser refresh so default random runs again', () => {
    const storage = createStorage()

    saveSquareSession({
      page: 2,
      limit: 30,
      keyword: '',
      categoryId: null,
      tags: [],
      sortField: 'image_name',
      randomSeed: 'old-seed',
    }, storage)

    expect(loadSquareSession(storage)).toEqual({
      page: 2,
      limit: 30,
      keyword: '',
      categoryId: null,
      tags: [],
      sortField: '',
    })
  })
})
