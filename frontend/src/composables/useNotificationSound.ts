const STORAGE_KEY = 'tsp_notification_sound'
const SOUND_SRC = '/sounds/notification.mp3'

let _enabled: boolean | null = null
let _audio: HTMLAudioElement | null = null

function loadPreference(): boolean {
  if (_enabled !== null) return _enabled
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    _enabled = stored === null ? true : stored === '1'
  } catch {
    _enabled = true
  }
  return _enabled
}

export function isNotificationSoundEnabled(): boolean {
  return loadPreference()
}

export function setNotificationSoundEnabled(enabled: boolean) {
  _enabled = enabled
  try {
    localStorage.setItem(STORAGE_KEY, enabled ? '1' : '0')
  } catch {}
}

export function toggleNotificationSound(): boolean {
  const next = !isNotificationSoundEnabled()
  setNotificationSoundEnabled(next)
  return next
}

function getAudio(): HTMLAudioElement {
  if (!_audio) {
    _audio = new Audio(SOUND_SRC)
    _audio.preload = 'auto'
  }
  return _audio
}

export function playNotificationSound() {
  if (!isNotificationSoundEnabled()) return

  const audio = getAudio()
  audio.currentTime = 0
  audio.play().catch(() => {})
}


