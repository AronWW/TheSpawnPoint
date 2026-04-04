export interface UserMe {
  id: number
  email: string
  displayName: string
  emailVerified: boolean
  role: string
  status: string
  lastSeen: string
  avatarUrl: string | null
  banned: boolean
  banReason: string | null
}

export interface Game {
  id: number
  name: string
  genre: string | null
  releaseYear: number | null
  imageUrl: string | null
  maxPartySize: number
}

export interface PartyMember {
  userId: number
  displayName: string
  avatarUrl: string | null
  isCreator: boolean
  joinedAt: string
  rating: number | null
}

export interface Party {
  id: number
  creatorId: number
  creatorDisplayName: string
  creatorAvatarUrl: string | null
  gameId: number
  gameName: string
  gameImageUrl: string | null
  maxMembers: number
  currentMembers: number
  isOpen: boolean
  status: 'OPEN' | 'FULL' | 'IN_GAME' | 'COMPLETED' | 'CANCELLED'
  title: string | null
  description: string | null
  eventTime: string | null
  platform: string[]
  languages: string[]
  skillLevel: string | null
  playStyle: string | null
  tags: string[] | null
  region: string | null
  members: PartyMember[] | null
  chatId: number | null
  createdAt: string
  creatorRating: number | null
}

export interface CreatePartyRequest {
  gameId: number | null
  title: string
  description: string
  eventTime: string | null
  platform: string[]
  languages: string[]
  skillLevel: string | null
  playStyle: string | null
  tags: string[]
  region: string | null
  maxMembers: number
}

export interface Notification {
  id: number
  type: string
  message: string
  referenceId: number | null
  read: boolean
  createdAt: string
}

export interface PartyInvite {
  inviteId: number
  senderId: number
  senderDisplayName: string
  senderAvatarUrl: string | null
  receiverId: number
  receiverDisplayName: string
  receiverAvatarUrl: string | null
  partyId: number | null
  gameName: string | null
  gameImageUrl: string | null
  status: string
  createdAt: string
}

export interface Page<T> {
  content: T[]

  page?: {
    totalElements: number
    totalPages: number
    number: number
    size: number
  }

  totalElements?: number
  totalPages?: number
  number?: number
  size?: number
}

export interface PartyFilters {
  gameId: number | null
  platform: string
  skillLevel: string
  playStyle: string
  language: string
  region: string
  search: string
}

export type SortOption = 'newest' | 'slots' | 'game'

export interface Friend {
  userId: number
  email: string
  displayName: string
  avatarUrl: string | null
  status: string
  lastSeen: string | null
  friendsSince: string
  rating: number | null
}

export interface FriendRequest {
  inviteId: number
  senderId: number
  senderEmail: string
  senderDisplayName: string
  senderAvatarUrl: string | null
  receiverId: number
  receiverDisplayName: string
  receiverAvatarUrl: string | null
  createdAt: string
}

export interface Profile {
  userId: number
  email: string
  displayName: string
  fullName: string | null
  avatarUrl: string | null
  bio: string | null
  birthDate: string | null
  platforms: string[]
  skillLevel: string | null
  playStyle: string | null
  languages: string[]
  country: string | null
  region: string | null
  discord: string | null
  steam: string | null
  twitch: string | null
  xbox: string | null
  playstation: string | null
  nintendo: string | null
  bannerUrl: string | null
  status: 'ONLINE' | 'IN_LOBBY' | 'IN_GAME' | 'OFFLINE'
  lastSeen: string | null
  createdAt: string
  privacy?: PrivacySettings | null
  rating?: number | null
  ratingCount?: number | null
}

export type VisibilityLevel = 'ALL' | 'FRIENDS' | 'NOBODY'

export interface PrivacySettings {
  friendsVisibility: string
  statusVisibility: string
  favoriteGamesVisibility: string
  statsVisibility: string
  socialsVisibility: string
  commentsPolicy: string
  achievementsVisibility: string
}

export type AchievementType = 'STANDARD' | 'SECRET'

export interface Achievement {
  code: string
  title: string
  description: string
  type: AchievementType
  icon: string
  requirementText: string | null
  secretHint: string | null
  hiddenBeforeUnlock: boolean
  unlocked: boolean
  unlockedAt: string | null
  order: number
  currentProgress: number | null
  targetProgress: number | null
  progressPercent: number | null
  showProgress: boolean
}

export interface AchievementPreview {
  totalCount: number
  unlockedCount: number
  items: Achievement[]
}

export interface AchievementUnlockEvent {
  code: string
  title: string
  description: string
  type: AchievementType
  icon: string
  unlockedAt: string
}

export interface ProfileComment {
  id: number
  profileUserId: number
  authorId: number
  authorDisplayName: string
  authorAvatarUrl: string | null
  content: string
  createdAt: string
}

export interface UserStats {
  completedGames: number
  partiesCreated: number
  partiesJoined: number
  hoursPlayed: number
  favoriteGameName: string | null
  favoriteGameImageUrl: string | null
  favoriteGameId: number | null
}

export interface ChatParticipant {
  userId: number
  displayName: string
  email: string
  avatarUrl: string | null
  role: 'OWNER' | 'ADMIN' | 'MEMBER'
}

export interface ChatItem {
  id: number

  isGroup: boolean
  chatType: 'DM' | 'GROUP' | 'GAME'
  lastMessage: string | null
  lastMessageAt: string | null
  unreadCount: number

  partnerEmail: string | null
  partnerDisplayName: string | null
  partnerAvatarUrl: string | null
  partnerStatus: string | null
  partnerLastSeen: string | null

  isPartyLinked: boolean
  title: string | null
  partyId: number | null
  groupAvatarUrl: string | null
  participants: ChatParticipant[] | null

  archived: boolean
  pinned: boolean
  pinnedAt: string | null
}

export interface ChatMessage {
  id: number
  chatId: number
  senderEmail: string | null
  senderName: string | null
  senderAvatarUrl: string | null
  content: string
  sentAt: string
  read: boolean
  system: boolean
  deleted: boolean
  edited: boolean
  editedAt: string | null
  replyToId: number | null
  replyToContent: string | null
  replyToSenderName: string | null
  reactions: ReactionInfo[]
}

export interface ReactionInfo {
  emoji: string
  count: number
  userEmails: string[]
}

export interface PinnedMessageInfo {
  id: number
  messageId: number
  chatId: number
  content: string
  senderName: string
  pinnedByName: string
  pinnedAt: string
}

export interface ChatEvent {
  type: string
  chatId: number
  payload: any
}

export interface AdminDashboard {
  totalUsers: number
  bannedUsers: number
  totalGames: number
  openParties: number
  pendingSuggestions: number
  openReports: number
  openTickets: number
  pendingUnbanRequests: number
}

export interface AdminUser {
  id: number
  displayName: string
  email: string
  role: string
  status: string
  emailVerified: boolean
  banned: boolean
  banReason: string | null
  bannedAt: string | null
  lastSeen: string | null
  createdAt: string
  avatarUrl: string | null
}

export interface AdminActiveParty {
  id: number
  gameName: string
  gameImageUrl: string | null
  creatorDisplayName: string
  currentMembers: number
  maxMembers: number
  languages: string[] | null
  createdAt: string
}

export interface GameSuggestion {
  id: number
  name: string
  genre: string | null
  releaseYear: number | null
  imageUrl: string | null
  maxPartySize: number
  status: string
  adminComment: string | null
  suggestedByUserId: number
  suggestedByDisplayName: string
  createdAt: string
  reviewedAt: string | null
}

export interface Report {
  id: number
  reporterId: number
  reporterDisplayName: string
  reportedUserId: number
  reportedUserDisplayName: string
  reason: string
  description: string | null
  status: string
  adminComment: string | null
  createdAt: string
  reviewedAt: string | null
}

export interface SupportTicket {
  id: number
  userId: number
  userDisplayName: string
  subject: string
  status: string
  createdAt: string
  closedAt: string | null
}

export interface TicketMessage {
  id: number
  senderId: number
  senderDisplayName: string
  senderAvatarUrl: string | null
  admin: boolean
  content: string
  sentAt: string
}

export interface BanRecord {
  banReason: string | null
  createdAt: string
  requestReason: string
  requestStatus: string
}

export interface UnbanRequest {
  id: number
  userId: number
  userDisplayName: string
  userEmail: string
  userAvatarUrl: string | null
  reason: string
  status: string
  adminComment: string | null
  banReason: string | null
  banHistory: BanRecord[]
  createdAt: string
  reviewedAt: string | null
}



export interface VoiceTokenResponse {
  serverUrl: string
  participantToken: string
  roomName: string
  participantIdentity: string
  participantName: string
}

export type VoiceConnectionState =
  | 'disconnected'
  | 'connecting'
  | 'connected'
  | 'reconnecting'
  | 'error'

export interface BlockedUser {
  userId: number
  displayName: string
  avatarUrl: string | null
  createdAt: string
}

export interface VoiceParticipant {
  identity: string
  userId: number | null
  name: string
  avatarUrl: string | null
  isLocal: boolean
  isSpeaking: boolean
  isMicrophoneEnabled: boolean
  audioLevel: number
}

export interface RecentTeammate {
  userId: number
  displayName: string
  avatarUrl: string | null
  gamesPlayedTogether: number
  rating: number | null
}

export interface PlayAgainData {
  gameId: number
  title: string
  description: string
  platform: string[]
  languages: string[]
  skillLevel: string | null
  playStyle: string | null
  tags: string[]
  region: string | null
  maxMembers: number
  previousMembers: PartyMember[]
}

export interface PartyPreset {
  id: number
  name: string
  slotIndex: number
  gameId: number
  gameName: string
  gameImageUrl: string | null
  maxMembers: number
  platform: string[]
  languages: string[]
  skillLevel: string | null
  playStyle: string | null
  tags: string[]
  region: string | null
}

export interface SavePartyPresetRequest {
  name: string
  slotIndex: number
  gameId: number
  maxMembers: number
  platform: string[]
  languages: string[]
  skillLevel: string | null
  playStyle: string | null
  tags: string[]
  region: string | null
}

export interface IndividualRating {
  userId: number
  score: number
}

export interface SubmitRatingsRequest {
  partyId: number
  ratings: IndividualRating[]
}

export interface UserRating {
  rating: number | null
  ratingCount: number
  ratingVisible: boolean
}
