import { beforeEach, describe, expect, it, vi } from 'vitest'

const confettiMock = vi.hoisted(() => vi.fn())

vi.mock('canvas-confetti', () => ({
  default: confettiMock,
}))

import {
  fireBigSideCannons,
  fireMediumSideCannons,
  fireSmallSideCannons,
} from '../utils/confettiEffect'

function mockReducedMotion(matches = false) {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: vi.fn().mockImplementation((query) => ({
      matches,
      media: query,
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  })
}

describe('confettiEffect', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    confettiMock.mockReset()
    mockReducedMotion(false)
  })

  it('fires small side cannons from both lower screen edges', () => {
    fireSmallSideCannons()

    expect(confettiMock).toHaveBeenCalledTimes(2)
    expect(confettiMock).toHaveBeenNthCalledWith(1, expect.objectContaining({
      angle: 60,
      origin: { x: 0, y: 1 },
    }))
    expect(confettiMock).toHaveBeenNthCalledWith(2, expect.objectContaining({
      angle: 120,
      origin: { x: 1, y: 1 },
    }))
  })

  it('fires medium side cannons with more particles than the small effect', () => {
    fireSmallSideCannons()
    const smallParticles = confettiMock.mock.calls[0][0].particleCount
    confettiMock.mockClear()

    fireMediumSideCannons()

    expect(confettiMock).toHaveBeenCalledTimes(2)
    expect(confettiMock.mock.calls[0][0].particleCount).toBeGreaterThan(smallParticles)
    expect(confettiMock.mock.calls[1][0]).toEqual(expect.objectContaining({ angle: 120, origin: { x: 1, y: 1 } }))
  })

  it('fires big side cannons once with denser left and right bursts', () => {
    fireMediumSideCannons()
    const mediumParticles = confettiMock.mock.calls[0][0].particleCount
    confettiMock.mockClear()

    fireBigSideCannons()

    expect(confettiMock).toHaveBeenCalledTimes(2)
    vi.advanceTimersByTime(1200)

    expect(confettiMock).toHaveBeenCalledTimes(2)
    expect(confettiMock.mock.calls[0][0].particleCount).toBeGreaterThan(mediumParticles)
    expect(confettiMock.mock.calls[1][0].particleCount).toBeGreaterThan(mediumParticles)
    expect(confettiMock.mock.calls.some(([options]) => options.origin.x === 0 && options.angle === 60)).toBe(true)
    expect(confettiMock.mock.calls.some(([options]) => options.origin.x === 1 && options.angle === 120)).toBe(true)
  })

  it('does not fire confetti when reduced motion is requested', () => {
    mockReducedMotion(true)

    fireSmallSideCannons()
    fireMediumSideCannons()
    fireBigSideCannons()
    vi.advanceTimersByTime(1200)

    expect(confettiMock).not.toHaveBeenCalled()
  })
})