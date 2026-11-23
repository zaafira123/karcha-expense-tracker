let latestSharedText: string | null = null
const listeners = new Set<(text: string) => void>()

export function initializeShareTargetReceiver() {
  if (!('serviceWorker' in navigator)) return
  navigator.serviceWorker.addEventListener('message', event => {
    const data = event.data as { type?: string; text?: string } | undefined
    if (data?.type !== 'KHARCHA_SHARED_SMS' || !data.text) return
    latestSharedText = data.text
    listeners.forEach(listener => listener(data.text!))
    event.ports[0]?.postMessage({ type: 'KHARCHA_SHARE_RECEIVED' })
  })
}

export function subscribeToSharedSms(listener: (text: string) => void) {
  listeners.add(listener)
  if (latestSharedText) listener(latestSharedText)
  return () => { listeners.delete(listener) }
}
