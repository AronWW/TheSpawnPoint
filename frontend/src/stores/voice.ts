import { defineStore } from 'pinia'
import { computed, ref, shallowRef, watch, type WatchStopHandle } from 'vue'
import { Room, RoomEvent, Track } from 'livekit-client'
import type {
  Participant,
  Room as LiveKitRoom,
  RemoteTrack,
  RemoteTrackPublication,
} from 'livekit-client'
import api from '../api/axios'
import { useToast } from '../composables/useToast'
import type {
  Party,
  VoiceConnectionState,
  VoiceParticipant,
  VoiceTokenResponse,
} from '../types'
import { useAuthStore } from './auth'

const ACTIVE_PARTY_STATUSES = new Set<Party['status']>(['OPEN', 'FULL', 'IN_GAME'])

export const useVoiceStore = defineStore('voice', () => {
  const auth = useAuthStore()
  const { show } = useToast()

  const room = shallowRef<LiveKitRoom | null>(null)
  const initialized = ref(false)
  const currentPartyId = ref<number | null>(null)
  const currentPartyTitle = ref('')
  const currentRoomName = ref('')
  const serverUrl = ref('')
  const participantIdentity = ref('')
  const participantName = ref('')
  const participants = ref<VoiceParticipant[]>([])
  const connectionState = ref<VoiceConnectionState>('disconnected')
  const isConnecting = ref(false)
  const isMicEnabled = ref(false)
  const autoplayBlocked = ref(false)
  const isExpanded = ref(true)
  const error = ref('')

  const attachedAudioElements = new Map<string, HTMLAudioElement>()
  let audioContainer: HTMLDivElement | null = null
  let stopAuthWatcher: WatchStopHandle | null = null

  const hasActiveSession = computed(() => room.value !== null && currentPartyId.value !== null)
  const participantCount = computed(() => participants.value.length)
  const shouldShowFloatingWidget = computed(() => hasActiveSession.value)
  const isConnected = computed(() => connectionState.value === 'connected')

  function init() {
    if (initialized.value) return

    initialized.value = true
    ensureAudioContainer()

    stopAuthWatcher = watch(
      () => auth.user,
      (user) => {
        if (!user || user.banned) {
          void leaveVoice({ silent: true })
        }
      },
      { immediate: true }
    )

    window.addEventListener('beforeunload', handleBeforeUnload)
  }

  function dispose() {
    stopAuthWatcher?.()
    stopAuthWatcher = null
    window.removeEventListener('beforeunload', handleBeforeUnload)
    cleanupAudioElements()
    removeAudioContainer()
    initialized.value = false
  }

  function handleBeforeUnload() {
    if (!room.value) return
    void room.value.disconnect(true)
  }

  function isInPartyVoice(partyId: number | null | undefined) {
    return !!room.value && currentPartyId.value === (partyId ?? null)
  }

  async function joinPartyVoice(party: Pick<Party, 'id' | 'title' | 'gameName'>) {
    if (!auth.user) {
      throw new Error('Потрібно увійти, щоб підключитися до голосового чату')
    }

    if (currentPartyId.value === party.id && room.value) {
      const sameRoomState = connectionState.value
      if (
        sameRoomState === 'connected' ||
        sameRoomState === 'connecting' ||
        sameRoomState === 'reconnecting'
      ) {
        isExpanded.value = true
        return
      }
    }

    error.value = ''
    isConnecting.value = true
    connectionState.value = 'connecting'

    if (room.value && currentPartyId.value !== party.id) {
      await leaveVoice({ silent: true })
    }

    currentPartyId.value = party.id
    currentPartyTitle.value = party.title?.trim() || party.gameName
    isExpanded.value = true

    let nextRoom: LiveKitRoom | null = null

    try {
      const tokenData = await fetchVoiceToken(party.id)

      nextRoom = new Room()
      bindRoomEvents(nextRoom)
      room.value = nextRoom

      serverUrl.value = tokenData.serverUrl
      currentRoomName.value = tokenData.roomName
      participantIdentity.value = tokenData.participantIdentity
      participantName.value = tokenData.participantName

      await nextRoom.connect(tokenData.serverUrl, tokenData.participantToken)
      attachExistingRemoteAudioTracks(nextRoom)
      await tryStartAudio(nextRoom)
      await nextRoom.localParticipant.setMicrophoneEnabled(true)

      isMicEnabled.value = nextRoom.localParticipant.isMicrophoneEnabled
      connectionState.value = 'connected'
      syncParticipantsFromRoom(nextRoom)
    } catch (err) {
      const message = extractErrorMessage(err, 'Не вдалося підключитися до голосового чату')
      error.value = message
      connectionState.value = 'error'
      show(message, 'error')

      if (nextRoom) {
        try {
          await nextRoom.disconnect(true)
        } catch {
        }
      }

      cleanupAudioElements()
      resetSessionState()
      throw new Error(message)
    } finally {
      isConnecting.value = false
    }
  }

  async function leaveVoice(options: { silent?: boolean } = {}) {
    const activeRoom = room.value

    cleanupAudioElements()
    resetSessionState()

    if (!activeRoom) return

    try {
      await activeRoom.disconnect(true)
    } catch {
    }

    if (!options.silent) {
      show('Ви вийшли з голосового чату', 'info')
    }
  }

  async function toggleMic() {
    if (!room.value) return

    const nextMicState = !room.value.localParticipant.isMicrophoneEnabled
    await room.value.localParticipant.setMicrophoneEnabled(nextMicState)
    isMicEnabled.value = room.value.localParticipant.isMicrophoneEnabled
    syncParticipantsFromRoom(room.value)
  }

  async function startAudioPlayback() {
    if (!room.value) return
    await tryStartAudio(room.value)
  }

  function expandWidget() {
    isExpanded.value = true
  }

  function collapseWidget() {
    isExpanded.value = false
  }

  async function syncPartyState(party: Party | null) {
    if (!party || currentPartyId.value !== party.id) return

    currentPartyTitle.value = party.title?.trim() || party.gameName

    const currentUserId = auth.user?.id
    const stillMember = !!party.members?.some((member) => member.userId === currentUserId)
    const stillActive = ACTIVE_PARTY_STATUSES.has(party.status)

    if (!stillMember) {
      await leaveVoice({ silent: true })
      show('Голосовий чат закрито: ви більше не є учасником цього лобі', 'info')
      return
    }

    if (!stillActive) {
      await leaveVoice({ silent: true })
      show('Голосовий чат закрито: лобі більше не активне', 'info')
    }
  }

  function resetSessionState() {
    room.value = null
    currentPartyId.value = null
    currentPartyTitle.value = ''
    currentRoomName.value = ''
    serverUrl.value = ''
    participantIdentity.value = ''
    participantName.value = ''
    participants.value = []
    isConnecting.value = false
    isMicEnabled.value = false
    autoplayBlocked.value = false
    isExpanded.value = false
    error.value = ''
    connectionState.value = 'disconnected'
  }

  function bindRoomEvents(targetRoom: LiveKitRoom) {
    const sync = () => {
      if (room.value !== targetRoom) return
      syncParticipantsFromRoom(targetRoom)
      isMicEnabled.value = targetRoom.localParticipant.isMicrophoneEnabled
      autoplayBlocked.value = !targetRoom.canPlaybackAudio
    }

    targetRoom
      .on(RoomEvent.Connected, () => {
        if (room.value !== targetRoom) return
        connectionState.value = 'connected'
        attachExistingRemoteAudioTracks(targetRoom)
        sync()
      })
      .on(RoomEvent.ConnectionStateChanged, (state) => {
        if (room.value !== targetRoom) return

        if (state === 'connected') {
          connectionState.value = 'connected'
        } else if (state === 'reconnecting') {
          connectionState.value = 'reconnecting'
        } else if (state === 'disconnected') {
          connectionState.value = 'disconnected'
        } else {
          connectionState.value = 'connecting'
        }

        sync()
      })
      .on(RoomEvent.Reconnecting, () => {
        if (room.value !== targetRoom) return
        connectionState.value = 'reconnecting'
      })
      .on(RoomEvent.Reconnected, () => {
        if (room.value !== targetRoom) return
        connectionState.value = 'connected'
        attachExistingRemoteAudioTracks(targetRoom)
        sync()
      })
      .on(RoomEvent.Disconnected, () => {
        if (room.value !== targetRoom) return
        cleanupAudioElements()
        resetSessionState()
      })
      .on(RoomEvent.ParticipantConnected, sync)
      .on(RoomEvent.ParticipantDisconnected, (participant) => {
        if (room.value !== targetRoom) return
        detachParticipantAudio(participant.identity)
        sync()
      })
      .on(RoomEvent.ActiveSpeakersChanged, sync)
      .on(RoomEvent.TrackMuted, sync)
      .on(RoomEvent.TrackUnmuted, sync)
      .on(RoomEvent.LocalTrackPublished, sync)
      .on(RoomEvent.LocalTrackUnpublished, sync)
      .on(RoomEvent.TrackSubscribed, (track, publication, participant) => {
        if (room.value !== targetRoom) return
        attachRemoteAudio(track, publication, participant.identity)
        sync()
      })
      .on(RoomEvent.TrackUnsubscribed, (track, publication, participant) => {
        if (room.value !== targetRoom) return
        detachRemoteAudio(track, publication, participant.identity)
        sync()
      })
      .on(RoomEvent.ParticipantNameChanged, sync)
      .on(RoomEvent.AudioPlaybackStatusChanged, () => {
        if (room.value !== targetRoom) return
        autoplayBlocked.value = !targetRoom.canPlaybackAudio
      })
      .on(RoomEvent.MediaDevicesError, (mediaError) => {
        if (room.value !== targetRoom) return
        error.value = mediaError.message || 'Не вдалося отримати доступ до мікрофона'
      })
  }

  function syncParticipantsFromRoom(targetRoom: LiveKitRoom) {
    const nextParticipants = [
      targetRoom.localParticipant,
      ...Array.from(targetRoom.remoteParticipants.values()),
    ].map(mapParticipant)

    participants.value = nextParticipants.sort((a, b) => {
      if (a.isLocal && !b.isLocal) return -1
      if (!a.isLocal && b.isLocal) return 1
      return a.name.localeCompare(b.name, 'uk')
    })
  }

  function mapParticipant(participant: Participant): VoiceParticipant {
    return {
      identity: participant.identity,
      userId: parseUserIdFromIdentity(participant.identity),
      name: participant.name?.trim() || participant.identity,
      isLocal: participant.isLocal,
      isSpeaking: participant.isSpeaking,
      isMicrophoneEnabled: participant.isMicrophoneEnabled,
      audioLevel: participant.audioLevel,
    }
  }

  function parseUserIdFromIdentity(identity: string): number | null {
    const match = /^user-(\d+)$/.exec(identity)
    if (!match) return null

    const parsed = Number(match[1])
    return Number.isFinite(parsed) ? parsed : null
  }

  function ensureAudioContainer() {
    if (typeof document === 'undefined') return null
    if (audioContainer && document.body.contains(audioContainer)) return audioContainer

    audioContainer = document.createElement('div')
    audioContainer.id = 'livekit-audio-root'
    audioContainer.style.display = 'none'
    document.body.appendChild(audioContainer)
    return audioContainer
  }

  function removeAudioContainer() {
    if (audioContainer) {
      audioContainer.remove()
      audioContainer = null
    }
  }

  function attachExistingRemoteAudioTracks(targetRoom: LiveKitRoom) {
    for (const participant of targetRoom.remoteParticipants.values()) {
      for (const publication of participant.trackPublications.values()) {
        const remotePublication = publication as RemoteTrackPublication & {
          audioTrack?: RemoteTrack | null
          track?: RemoteTrack | null
        }
        const track = remotePublication.audioTrack ?? remotePublication.track
        attachRemoteAudio(track ?? null, remotePublication, participant.identity)
      }
    }
  }

  function attachRemoteAudio(
    track: RemoteTrack | null | undefined,
    publication: RemoteTrackPublication,
    participantIdentity: string
  ) {
    if (!track || track.kind !== Track.Kind.Audio) return

    const key = getTrackKey(publication, participantIdentity)
    if (attachedAudioElements.has(key)) return

    const container = ensureAudioContainer()
    if (!container) return

    const element = track.attach() as HTMLAudioElement
    element.autoplay = true
    element.dataset.voiceTrackKey = key
    container.appendChild(element)
    attachedAudioElements.set(key, element)
  }

  function detachRemoteAudio(
    track: RemoteTrack | null | undefined,
    publication: RemoteTrackPublication,
    participantIdentity: string
  ) {
    const key = getTrackKey(publication, participantIdentity)
    track?.detach()

    const existingElement = attachedAudioElements.get(key)
    if (existingElement) {
      existingElement.remove()
      attachedAudioElements.delete(key)
    }
  }

  function detachParticipantAudio(participantIdentity: string) {
    for (const [key, element] of attachedAudioElements.entries()) {
      if (!key.startsWith(`${participantIdentity}:`)) continue
      element.remove()
      attachedAudioElements.delete(key)
    }
  }

  function cleanupAudioElements() {
    for (const element of attachedAudioElements.values()) {
      element.remove()
    }
    attachedAudioElements.clear()
  }

  function getTrackKey(publication: RemoteTrackPublication, participantIdentity: string) {
    const publicationLike = publication as { trackSid?: string; sid?: string }
    const sid = publicationLike.trackSid ?? publicationLike.sid ?? 'audio'
    return `${participantIdentity}:${sid}`
  }

  async function tryStartAudio(targetRoom: LiveKitRoom) {
    try {
      await targetRoom.startAudio()
    } catch {
    }

    autoplayBlocked.value = !targetRoom.canPlaybackAudio
  }

  async function fetchVoiceToken(partyId: number) {
    const { data } = await api.post<VoiceTokenResponse>(`/voice/parties/${partyId}/token`)
    return data
  }

  function extractErrorMessage(err: unknown, fallback: string) {
    if (typeof err === 'object' && err !== null) {
      const maybeError = err as {
        response?: { data?: { message?: string } }
        message?: string
      }

      if (maybeError.response?.data?.message) {
        return maybeError.response.data.message
      }

      if (maybeError.message) {
        return maybeError.message
      }
    }

    return fallback
  }

  return {
    room,
    initialized,
    currentPartyId,
    currentPartyTitle,
    currentRoomName,
    serverUrl,
    participantIdentity,
    participantName,
    participants,
    connectionState,
    isConnecting,
    isMicEnabled,
    autoplayBlocked,
    isExpanded,
    error,
    hasActiveSession,
    participantCount,
    shouldShowFloatingWidget,
    isConnected,
    init,
    dispose,
    isInPartyVoice,
    joinPartyVoice,
    leaveVoice,
    toggleMic,
    startAudioPlayback,
    expandWidget,
    collapseWidget,
    syncPartyState,
    resetSessionState,
  }
})
