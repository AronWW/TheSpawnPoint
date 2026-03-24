export type SocialField = 'discord' | 'steam' | 'twitch' | 'xbox' | 'playstation' | 'nintendo'

export interface SocialValidationResult {
  normalized: string
  error: string
}

const DISCORD_RE = /^(?=.{2,37}$)(?!.*\.\.)(?!\.)(?!.*\.$)[A-Za-z0-9._]+(?:#[0-9]{4})?$/
const STEAM_ID64_RE = /^\d{17}$/
const STEAM_VANITY_RE = /^[A-Za-z0-9_-]{2,32}$/
const TWITCH_LOGIN_RE = /^[A-Za-z0-9_]{4,25}$/
const XBOX_GAMERTAG_RE = /^(?=.{3,15}$)[A-Za-z0-9][A-Za-z0-9 ]{2,14}$/
const PLAYSTATION_ID_RE = /^[A-Za-z0-9_.-]{3,16}$/
const NINTENDO_RE = /^(?:SW-)?\d{4}-\d{4}-\d{4}$/i

function tryParseUrl(raw: string): URL | null {
  const value = raw.trim()
  if (!value) return null
  const withProtocol = /^https?:\/\//i.test(value) ? value : `https://${value}`
  try {
    return new URL(withProtocol)
  } catch {
    return null
  }
}

function normalizeDiscord(raw: string): SocialValidationResult {
  const value = raw.trim()
  if (!value) return { normalized: '', error: '' }
  if (/^https?:\/\//i.test(value)) {
    return { normalized: value, error: 'Discord: введи username, а не посилання.' }
  }
  if (!DISCORD_RE.test(value)) {
    return { normalized: value, error: 'Discord: невірний формат username.' }
  }
  return { normalized: value, error: '' }
}

function normalizeSteam(raw: string): SocialValidationResult {
  const value = raw.trim()
  if (!value) return { normalized: '', error: '' }

  if (STEAM_ID64_RE.test(value) || STEAM_VANITY_RE.test(value)) {
    return { normalized: value, error: '' }
  }

  const url = tryParseUrl(value)
  if (!url) {
    return { normalized: value, error: 'Steam: введи коректний vanity/Steam ID або steamcommunity.com URL.' }
  }

  const host = url.hostname.toLowerCase()
  if (host !== 'steamcommunity.com' && host !== 'www.steamcommunity.com') {
    return { normalized: value, error: 'Steam: дозволено лише посилання на steamcommunity.com.' }
  }

  const parts = url.pathname.split('/').filter(Boolean)
  if (parts.length < 2) {
    return { normalized: value, error: 'Steam: очікується /id/<name> або /profiles/<steamid64>.' }
  }

  const sectionRaw = parts[0]
  const idRaw = parts[1]
  if (!sectionRaw || !idRaw) {
    return { normalized: value, error: 'Steam: невірний формат профілю.' }
  }

  const section = sectionRaw.toLowerCase()
  const id = decodeURIComponent(idRaw).trim()
  if (section === 'id' && STEAM_VANITY_RE.test(id)) {
    return { normalized: `https://steamcommunity.com/id/${encodeURIComponent(id)}`, error: '' }
  }
  if (section === 'profiles' && STEAM_ID64_RE.test(id)) {
    return { normalized: `https://steamcommunity.com/profiles/${id}`, error: '' }
  }

  return { normalized: value, error: 'Steam: невірний формат профілю.' }
}

function normalizeTwitch(raw: string): SocialValidationResult {
  const value = raw.trim()
  if (!value) return { normalized: '', error: '' }
  if (TWITCH_LOGIN_RE.test(value)) {
    return { normalized: value, error: '' }
  }

  const url = tryParseUrl(value)
  if (!url) {
    return { normalized: value, error: 'Twitch: введи login або twitch.tv/<login>.' }
  }

  const host = url.hostname.toLowerCase()
  if (host !== 'twitch.tv' && host !== 'www.twitch.tv') {
    return { normalized: value, error: 'Twitch: дозволено лише посилання на twitch.tv.' }
  }

  const login = decodeURIComponent(url.pathname.split('/').filter(Boolean)[0] ?? '').trim()
  if (!TWITCH_LOGIN_RE.test(login)) {
    return { normalized: value, error: 'Twitch: невірний login.' }
  }

  return { normalized: login, error: '' }
}

function normalizeXbox(raw: string): SocialValidationResult {
  const value = raw.trim()
  if (!value) return { normalized: '', error: '' }
  if (XBOX_GAMERTAG_RE.test(value)) {
    return { normalized: value, error: '' }
  }

  const url = tryParseUrl(value)
  if (!url) {
    return { normalized: value, error: 'Xbox: введи gamertag або account.xbox.com URL.' }
  }

  if (url.hostname.toLowerCase() !== 'account.xbox.com') {
    return { normalized: value, error: 'Xbox: дозволено лише посилання на account.xbox.com.' }
  }

  if (!/\/profile$/i.test(url.pathname)) {
    return { normalized: value, error: 'Xbox: невірний формат посилання профілю.' }
  }

  const gamertag = decodeURIComponent(url.searchParams.get('gamertag') ?? '').trim()
  if (!XBOX_GAMERTAG_RE.test(gamertag)) {
    return { normalized: value, error: 'Xbox: невірний gamertag.' }
  }

  return { normalized: gamertag, error: '' }
}

function normalizePlaystation(raw: string): SocialValidationResult {
  const value = raw.trim()
  if (!value) return { normalized: '', error: '' }
  if (PLAYSTATION_ID_RE.test(value)) {
    return { normalized: value, error: '' }
  }

  const url = tryParseUrl(value)
  if (!url) {
    return { normalized: value, error: 'PlayStation: введи Online ID або офіційне посилання профілю.' }
  }

  const host = url.hostname.toLowerCase()
  const parts = url.pathname.split('/').filter(Boolean)

  if (host === 'psnprofiles.com' || host === 'www.psnprofiles.com') {
    const id = decodeURIComponent(parts[0] ?? '').trim()
    if (!PLAYSTATION_ID_RE.test(id)) {
      return { normalized: value, error: 'PlayStation: невірний PSN ID у посиланні.' }
    }
    return { normalized: id, error: '' }
  }

  if (host === 'my.playstation.com') {
    if (parts[0]?.toLowerCase() !== 'profile') {
      return { normalized: value, error: 'PlayStation: невірний формат посилання профілю.' }
    }
    const id = decodeURIComponent(parts[1] ?? '').trim()
    if (!PLAYSTATION_ID_RE.test(id)) {
      return { normalized: value, error: 'PlayStation: невірний PSN ID у посиланні.' }
    }
    return { normalized: id, error: '' }
  }

  return { normalized: value, error: 'PlayStation: дозволені лише psnprofiles.com або my.playstation.com.' }
}

function normalizeNintendo(raw: string): SocialValidationResult {
  const value = raw.trim().toUpperCase()
  if (!value) return { normalized: '', error: '' }
  if (!NINTENDO_RE.test(value)) {
    return { normalized: value, error: 'Nintendo: формат має бути SW-1234-5678-9012.' }
  }

  const digits = value.replace(/\D/g, '')
  const normalized = `SW-${digits.slice(0, 4)}-${digits.slice(4, 8)}-${digits.slice(8, 12)}`
  return { normalized, error: '' }
}

export function validateAndNormalizeSocial(field: SocialField, value: string): SocialValidationResult {
  switch (field) {
    case 'discord':
      return normalizeDiscord(value)
    case 'steam':
      return normalizeSteam(value)
    case 'twitch':
      return normalizeTwitch(value)
    case 'xbox':
      return normalizeXbox(value)
    case 'playstation':
      return normalizePlaystation(value)
    case 'nintendo':
      return normalizeNintendo(value)
    default:
      return { normalized: value.trim(), error: '' }
  }
}


